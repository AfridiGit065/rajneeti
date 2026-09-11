package com.rajneeti.game;

import com.rajneeti.dto.game.GameStateResponse;
import com.rajneeti.entity.Match;
import com.rajneeti.entity.MatchPlayer;
import com.rajneeti.entity.Room;
import com.rajneeti.entity.User;
import com.rajneeti.entity.enums.MatchStatus;
import com.rajneeti.entity.enums.PlayerStatus;
import com.rajneeti.exception.BusinessException;
import com.rajneeti.exception.MatchNotFoundException;
import com.rajneeti.repository.MatchPlayerRepository;
import com.rajneeti.repository.MatchRepository;
import com.rajneeti.service.TurnManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Module 08 — Game Engine.
 *
 * <p>The authoritative orchestrator for an in-memory Rajneeti game. It owns the
 * initialization flow (Match -> GameState -> players -> coins -> cards -> ready
 * for the TurnManager) and exposes player-safe projections of the live state.
 *
 * <p>Turn progression is delegated to the existing {@code TurnManager}
 * (Module 09). Basic gameplay actions that resolve instantly (income) are
 * implemented here; tax, steal, challenge, block, coup and the action resolver
 * arrive in later modules.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GameEngine {

    /** Every player starts with 2 coins. */
    public static final int STARTING_COINS = 2;

    /** Every player starts with 2 influence cards. */
    public static final int STARTING_INFLUENCE = CardManager.STARTING_HAND;

    /** Income grants exactly 1 coin, unconditionally. */
    public static final int INCOME_GAIN = 1;

    /** Foreign Aid grants 2 coins, but can be blocked by Minister. */
    public static final int FOREIGN_AID_GAIN = 2;

    /** Pending action type identifier for Foreign Aid. */
    public static final String ACTION_FOREIGN_AID = "FOREIGN_AID";

    /** Pending action type identifier for Exchange. */
    public static final String ACTION_EXCHANGE = "EXCHANGE";

    /** Pending action type identifier for Assassination. */
    public static final String ACTION_ASSASSINATE = "ASSASSINATE";

    /** Pending action type identifier for Tax. */
    public static final String ACTION_TAX = "TAX";

    /** Pending action type identifier for Steal. */
    public static final String ACTION_STEAL = "STEAL";

    /** Exchange draws exactly 2 cards and keeps exactly 2 of the 4 available. */
    public static final int EXCHANGE_DRAW = 2;

    /** The character claimed by an Exchange action. */
    public static final String CHARACTER_AMLA = "amla";

    /** The character claimed by an Assassination action. */
    public static final String CHARACTER_GHATOK = "ghatok";

    /** The character claimed by a Tax action. */
    public static final String CHARACTER_MINISTER = "minister";

    /** The character claimed by a Steal action. */
    public static final String CHARACTER_DALAL = "dalal";

    /** The character that can block an Assassination. */
    public static final String CHARACTER_GOYENDA = "goyenda";

    /** Tax grants exactly 3 coins. */
    public static final int TAX_GAIN = 3;

    /** Steal takes up to 2 coins from the target. */
    public static final int STEAL_GAIN = 2;

    /** Assassination costs exactly 3 coins. */
    public static final int ASSASSINATE_COST = 3;

    public static final String PHASE_SETUP = "setup";
    public static final String PHASE_IN_PROGRESS = "in_progress";
    public static final String PHASE_GAME_OVER = "game_over";

    private final MatchRepository matchRepository;
    private final MatchPlayerRepository matchPlayerRepository;
    private final GameStore gameStore;
    private final CardManager cardManager;
    private final TurnManager turnManager;
    private final GameStateMapper gameStateMapper;

    /**
     * Initializes a live game instance for an existing match.
     *
     * <p>Creates the 15-card deck, deals two influence cards to every player,
     * credits 2 starting coins to each player and stores the resulting state in
     * the in-memory {@link GameStore}.
     *
     * @param matchId the match to initialize
     * @return the initialized game state
     * @throws MatchNotFoundException      if the match does not exist
     * @throws BusinessException           if the match has no players, or the
     *                                     game state was already initialized
     */
    @Transactional(readOnly = true)
    public GameState initializeMatch(UUID matchId) {
        if (gameStore.containsKey(matchId)) {
            throw new BusinessException("MATCH_ALREADY_INITIALIZED",
                    "Game state for match '" + matchId + "' is already initialized.");
        }

        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new MatchNotFoundException("Match with ID '" + matchId + "' not found."));

        List<MatchPlayer> matchPlayers = matchPlayerRepository.findByMatchId(matchId).stream()
                .sorted(Comparator.comparing(MatchPlayer::getSeatNumber))
                .toList();

        if (matchPlayers.isEmpty()) {
            throw new BusinessException("NO_PLAYERS",
                    "Cannot initialize game state: match '" + matchId + "' has no players.");
        }

        Room room = match.getRoom();
        User host = room != null ? room.getHost() : null;

        // CardManager builds and shuffles the deck, then deals the initial hands.
        List<GameCard> deck = cardManager.createShuffledDeck();

        List<GamePlayerState> players = new ArrayList<>();
        List<UUID> turnOrder = new ArrayList<>();
        for (MatchPlayer matchPlayer : matchPlayers) {
            User user = matchPlayer.getUser();

            GamePlayerState player = GamePlayerState.builder()
                    .userId(user.getId())
                    .username(user.getUsername())
                    .avatarUrl(user.getAvatarUrl())
                    .seatNumber(matchPlayer.getSeatNumber())
                    .status(matchPlayer.getPlayerStatus() != null
                            ? matchPlayer.getPlayerStatus() : PlayerStatus.ACTIVE)
                    .coins(STARTING_COINS)
                    .host(host != null && host.getId().equals(user.getId()))
                    .cards(new ArrayList<>())
                    .build();

            players.add(player);
            turnOrder.add(user.getId());
        }

        cardManager.dealInitialHands(deck, players);
        cardManager.assertDeckIntegrity(deck, players);

        GameState state = GameState.builder()
                .matchId(match.getId())
                .roomId(room != null ? room.getId() : null)
                .roomCode(room != null ? room.getRoomCode() : null)
                .status(match.getStatus())
                .phase(PHASE_SETUP)
                .hostUserId(host != null ? host.getId() : null)
                .players(players)
                .turnOrder(turnOrder)
                .currentTurnPlayerId(match.getCurrentTurnPlayerId())
                .turnNumber(match.getTurnNumber() != null ? match.getTurnNumber() : 0)
                .deck(deck)
                .revealedCardsCount(0)
                .winnerUserId(match.getWinner() != null ? match.getWinner().getId() : null)
                .log(new ArrayList<>())
                .startedAt(LocalDateTime.now())
                .build();

        state.getLog().add(GameLogEntry.info(
                "Match started. Every player received 2 influence cards and 2 coins."));

        gameStore.put(matchId, state);

        log.info("Game state initialized for match '{}' ({} players, {} cards dealt, {} left in deck)",
                matchId, players.size(), players.size() * STARTING_INFLUENCE, deck.size());

        return state;
    }

    /**
     * Returns the stored live state or initializes it from the persisted match.
     *
     * <p>Reliable entry point for reads: existing matches that were created
     * before this module also become playable on first request, while a second
     * initialization is prevented by {@link #initializeMatch(UUID)}.
     *
     * @param matchId the match ID
     * @return the live game state
     */
    @Transactional(readOnly = true)
    public GameState getOrInitialize(UUID matchId) {
        GameState existing = gameStore.get(matchId);
        return existing != null ? existing : initializeMatch(matchId);
    }

    /**
     * Returns the stored live state for a match.
     *
     * @param matchId the match ID
     * @return the live game state
     * @throws BusinessException if the game state is not initialized
     */
    public GameState getGameState(UUID matchId) {
        GameState state = gameStore.get(matchId);
        if (state == null) {
            throw new BusinessException("GAME_NOT_INITIALIZED",
                    "Game state for match '" + matchId + "' has not been initialized.");
        }
        return state;
    }

    /**
     * Returns the player-safe public projection of the live game state.
     *
     * @param matchId the match ID
     * @param viewerId the requesting player's user ID
     * @return player-safe game state (own cards visible, opponents' hidden)
     */
    @Transactional(readOnly = true)
    public GameStateResponse getSafeGameState(UUID matchId, UUID viewerId) {
        GameState state = getOrInitialize(matchId);
        return gameStateMapper.toResponse(state, viewerId);
    }

    /**
     * Performs the Income action for the current turn holder.
     *
     * <p>Income is the basic action: the player gains exactly {@value #INCOME_GAIN}
     * coin. It claims no character, so it can never be blocked or challenged, and
     * it resolves instantly — no response window is opened. The turn is advanced
     * through the existing {@link TurnManager}.
     *
     * @param matchId the match ID
     * @param userId  the acting player's user ID
     * @return the updated player-safe game state
     * @throws BusinessException with specific error codes for every rule
     *                           violation (see implementation)
     */
    @Transactional
    public GameStateResponse performIncome(UUID matchId, UUID userId) {
        GameState state = getOrInitialize(matchId);
        state.setLastChallenge(null);

        if (state.getStatus() != MatchStatus.IN_PROGRESS
                && state.getStatus() != MatchStatus.CREATED) {
            throw new BusinessException("MATCH_NOT_ACTIVE",
                    "Cannot perform Income: match is not active.");
        }

        GamePlayerState player = state.getPlayers().stream()
                .filter(p -> p.getUserId().equals(userId))
                .findFirst()
                .orElseThrow(() -> new BusinessException("PLAYER_NOT_IN_MATCH",
                        "Cannot perform Income: player is not part of this match."));

        if (!userId.equals(state.getCurrentTurnPlayerId())) {
            throw new BusinessException("NOT_YOUR_TURN",
                    "It is not your turn.");
        }

        if (player.getStatus() != PlayerStatus.ACTIVE) {
            throw new BusinessException("PLAYER_ELIMINATED",
                    "Eliminated players cannot perform actions.");
        }

        if (player.getCards() == null || player.getCards().isEmpty()) {
            throw new BusinessException("NO_INFLUENCE",
                    "You need at least one influence card to perform an action.");
        }

        if (state.isActionExecuted()) {
            throw new BusinessException("ACTION_ALREADY_PERFORMED",
                    "You have already performed an action this turn.");
        }

        player.setCoins(player.getCoins() + INCOME_GAIN);
        state.setActionExecuted(true);
        state.getLog().add(GameLogEntry.of("action",
                player.getUsername() + " performed Income: +" + INCOME_GAIN + " coin."));

        Match match = turnManager.advanceTurn(matchId);
        state.setCurrentTurnPlayerId(match.getCurrentTurnPlayerId());
        state.setTurnNumber(match.getTurnNumber());
        state.setActionExecuted(false);
        state.setPhase(PHASE_IN_PROGRESS);

        log.info("Player '{}' collected income in match {} (+{} coin), turn → {}",
                player.getUsername(), matchId, INCOME_GAIN, match.getTurnNumber());

        return gameStateMapper.toResponse(state, userId);
    }

    /**
     * Performs the Foreign Aid action for the current turn holder.
     *
     * <p>Foreign Aid is NOT a character claim, so it cannot be challenged, but
     * it CAN be blocked by a Minister. A {@link PendingAction} block window is
     * recorded in the in-memory state. The turn is NOT advanced and coins are
     * NOT awarded until {@link #resolveForeignAid} is called with a resolution.
     *
     * @param matchId the match ID
     * @param userId  the acting player's user ID
     * @return the updated player-safe game state (with pending action visible)
     */
    @Transactional
    public GameStateResponse performForeignAid(UUID matchId, UUID userId) {
        GameState state = getOrInitialize(matchId);
        state.setLastChallenge(null);

        if (state.getStatus() != MatchStatus.IN_PROGRESS
                && state.getStatus() != MatchStatus.CREATED) {
            throw new BusinessException("MATCH_NOT_ACTIVE",
                    "Cannot perform Foreign Aid: match is not active.");
        }

        GamePlayerState player = state.getPlayers().stream()
                .filter(p -> p.getUserId().equals(userId))
                .findFirst()
                .orElseThrow(() -> new BusinessException("PLAYER_NOT_IN_MATCH",
                        "Cannot perform Foreign Aid: player is not part of this match."));

        if (!userId.equals(state.getCurrentTurnPlayerId())) {
            throw new BusinessException("NOT_YOUR_TURN",
                    "It is not your turn.");
        }

        if (player.getStatus() != PlayerStatus.ACTIVE) {
            throw new BusinessException("PLAYER_ELIMINATED",
                    "Eliminated players cannot perform actions.");
        }

        if (player.getCards() == null || player.getCards().isEmpty()) {
            throw new BusinessException("NO_INFLUENCE",
                    "You need at least one influence card to perform an action.");
        }

        if (state.isActionExecuted()) {
            throw new BusinessException("ACTION_ALREADY_PERFORMED",
                    "You have already performed an action this turn.");
        }

        state.setPendingAction(PendingAction.builder()
                .type(ACTION_FOREIGN_AID)
                .actorUserId(userId)
                .startedAt(LocalDateTime.now())
                .build());
        state.setActionExecuted(true);
        state.getLog().add(GameLogEntry.of("action",
                player.getUsername() + " declared Foreign Aid. Block window open."));

        log.info("Player '{}' declared Foreign Aid in match {} (block window open)",
                player.getUsername(), matchId);

        return gameStateMapper.toResponse(state, userId);
    }

    /**
     * Resolves a pending Foreign Aid after its block window closes.
     *
     * <p>If {@code blocked} is {@code true} the action is cancelled (no coins,
     * Minister block succeeds). If {@code false} the action resolves normally
     * and the actor receives {@value #FOREIGN_AID_GAIN} coins. The turn
     * advances in both cases.
     *
     * <p>Module 19 (Block Manager) will eventually call this after processing
     * the real block/challenge flow. This endpoint is the minimal seam so that
     * the game can proceed today while a full Block Manager is not yet built.
     *
     * @param matchId the match ID
     * @param userId  the actor's user ID (must match the pending action)
     * @param blocked true if the block succeeded, false if no block / block failed
     * @return the updated player-safe game state
     */
    @Transactional
    public GameStateResponse resolveForeignAid(UUID matchId, UUID userId, boolean blocked) {
        GameState state = getGameState(matchId);

        if (state.getPendingAction() == null) {
            throw new BusinessException("NO_PENDING_ACTION",
                    "No pending action to resolve.");
        }

        if (!ACTION_FOREIGN_AID.equals(state.getPendingAction().getType())) {
            throw new BusinessException("INVALID_PENDING_ACTION",
                    "The pending action is not Foreign Aid.");
        }

        if (!userId.equals(state.getPendingAction().getActorUserId())) {
            throw new BusinessException("NOT_ACTOR",
                    "Only the action's actor can resolve the pending action.");
        }

        GamePlayerState player = state.getPlayers().stream()
                .filter(p -> p.getUserId().equals(userId))
                .findFirst()
                .orElseThrow(() -> new BusinessException("PLAYER_NOT_IN_MATCH",
                        "Player is not part of this match."));

        state.setPendingAction(null);

        if (!blocked) {
            player.setCoins(player.getCoins() + FOREIGN_AID_GAIN);
            state.getLog().add(GameLogEntry.of("action",
                    player.getUsername() + " collected Foreign Aid: +"
                            + FOREIGN_AID_GAIN + " coins."));
        } else {
            state.getLog().add(GameLogEntry.of("block",
                    "Foreign Aid by " + player.getUsername() + " was blocked."));
        }

        Match match = turnManager.advanceTurn(matchId);
        state.setCurrentTurnPlayerId(match.getCurrentTurnPlayerId());
        state.setTurnNumber(match.getTurnNumber());
        state.setActionExecuted(false);
        state.setPhase(PHASE_IN_PROGRESS);

        log.info("Foreign Aid resolved (blocked={}) in match {}, turn → {}",
                blocked, matchId, match.getTurnNumber());

        return gameStateMapper.toResponse(state, userId);
    }

    /**
     * Performs the Exchange action for the current turn holder.
     *
     * <p>Exchange claims the {@code amla} character. The actor draws
     * {@value #EXCHANGE_DRAW} cards from the deck into their hand, temporarily
     * holding the initial 2 plus the 2 drawn cards (the private pool). The hand
     * is replaced by the pool so the 15-card single-location invariant holds
     * throughout. A {@link PendingAction} challenge window is recorded in the
     * in-memory state; the cards themselves are NOT swapped until
     * {@link #confirmExchange} resolves the action.
     *
     * <p>Module 16 (Challenge Manager) will eventually decide challenges during
     * this window and restore the original hand via {@code originalHandCardIds}
     * if the amla claim is overturned. Today this endpoint is the authoritative
     * seam that opens the window.
     *
     * @param matchId the match ID
     * @param userId  the acting player's user ID
     * @return the updated player-safe game state (actor sees the private pool)
     */
    @Transactional
    public GameStateResponse performExchange(UUID matchId, UUID userId) {
        GameState state = getOrInitialize(matchId);
        state.setLastChallenge(null);

        if (state.getStatus() != MatchStatus.IN_PROGRESS
                && state.getStatus() != MatchStatus.CREATED) {
            throw new BusinessException("MATCH_NOT_ACTIVE",
                    "Cannot perform Exchange: match is not active.");
        }

        GamePlayerState player = state.getPlayers().stream()
                .filter(p -> p.getUserId().equals(userId))
                .findFirst()
                .orElseThrow(() -> new BusinessException("PLAYER_NOT_IN_MATCH",
                        "Cannot perform Exchange: player is not part of this match."));

        if (!userId.equals(state.getCurrentTurnPlayerId())) {
            throw new BusinessException("NOT_YOUR_TURN",
                    "It is not your turn.");
        }

        if (player.getStatus() != PlayerStatus.ACTIVE) {
            throw new BusinessException("PLAYER_ELIMINATED",
                    "Eliminated players cannot perform actions.");
        }

        if (player.getCards() == null || player.getCards().size() < STARTING_INFLUENCE) {
            throw new BusinessException("NO_INFLUENCE",
                    "You need at least " + STARTING_INFLUENCE
                            + " influence cards to perform an Exchange.");
        }

        if (state.isActionExecuted()) {
            throw new BusinessException("ACTION_ALREADY_PERFORMED",
                    "You have already performed an action this turn.");
        }

        List<GameCard> deck = state.getDeck();
        if (deck == null || deck.size() < EXCHANGE_DRAW) {
            throw new BusinessException("INSUFFICIENT_DECK",
                    "Not enough cards in the deck to perform Exchange.");
        }

        // Draw 2 into the actor's hand so deck integrity holds at every step.
        List<GameCard> drawn = cardManager.drawMany(deck, EXCHANGE_DRAW);
        List<GameCard> originalHand = new ArrayList<>(player.getCards());
        List<UUID> originalHandCardIds = originalHand.stream()
                .map(GameCard::getId)
                .toList();
        player.getCards().addAll(drawn);

        List<GameCard> pool = new ArrayList<>();
        pool.addAll(originalHand);
        pool.addAll(drawn);

        state.setPendingAction(PendingAction.builder()
                .type(ACTION_EXCHANGE)
                .actorUserId(userId)
                .startedAt(LocalDateTime.now())
                .claimedCharacter(CHARACTER_AMLA)
                .exchangePool(pool)
                .originalHandCardIds(originalHandCardIds)
                .build());
        state.setActionExecuted(true);
        state.getLog().add(GameLogEntry.of("action",
                player.getUsername() + " declared Exchange, claiming Amla. Challenge window open."));

        log.info("Player '{}' declared Exchange in match {} (drew {}, challenge window open)",
                player.getUsername(), matchId, drawn.size());

        return gameStateMapper.toResponse(state, userId);
    }

    /**
     * Resolves a pending Exchange: the actor keeps exactly 2 cards from the
     * temporary private pool and the remaining pool cards are returned to the
     * deck. The selection is validated entirely server-side — the client never
     * supplies card objects, only the card IDs to keep, and they must match the
     * pool recorded when the Exchange was declared.
     *
     * <p>Deck integrity is re-verified after the swap, the pending action is
     * cleared, and the turn advances.
     *
     * @param matchId     the match ID
     * @param userId      the actor's user ID (must match the pending action)
     * @param keepCardIds the exactly {@value #EXCHANGE_DRAW} unique card IDs to keep
     * @return the updated player-safe game state
     */
    @Transactional
    public GameStateResponse confirmExchange(UUID matchId, UUID userId, List<UUID> keepCardIds) {
        GameState state = getGameState(matchId);

        if (state.getPendingAction() == null) {
            throw new BusinessException("NO_PENDING_ACTION",
                    "No pending action to resolve.");
        }

        if (!ACTION_EXCHANGE.equals(state.getPendingAction().getType())) {
            throw new BusinessException("INVALID_PENDING_ACTION",
                    "The pending action is not an Exchange.");
        }

        if (!userId.equals(state.getPendingAction().getActorUserId())) {
            throw new BusinessException("NOT_ACTOR",
                    "Only the exchange's actor can keep cards.");
        }

        GamePlayerState player = state.getPlayers().stream()
                .filter(p -> p.getUserId().equals(userId))
                .findFirst()
                .orElseThrow(() -> new BusinessException("PLAYER_NOT_IN_MATCH",
                        "Player is not part of this match."));

        List<GameCard> pool = state.getPendingAction().getExchangePool();
        if (pool == null || pool.size() != STARTING_INFLUENCE * 2) {
            throw new BusinessException("INVALID_GAME_STATE",
                    "Exchange pool is missing or corrupt: cannot resolve.");
        }

        // The actor's current hand must still be exactly the recorded pool.
        if (player.getCards().size() != pool.size()
                || !new HashSet<>(player.getCards().stream()
                        .map(GameCard::getId).toList())
                .equals(new HashSet<>(pool.stream().map(GameCard::getId).toList()))) {
            throw new BusinessException("INVALID_GAME_STATE",
                    "The actor's hand no longer matches the exchange pool.");
        }

        if (keepCardIds == null || keepCardIds.size() != EXCHANGE_DRAW) {
            throw new BusinessException("INVALID_CARD_SELECTION",
                    "You must keep exactly " + EXCHANGE_DRAW + " cards.");
        }

        Set<UUID> unique = new HashSet<>(keepCardIds);
        if (unique.size() != keepCardIds.size()) {
            throw new BusinessException("DUPLICATE_CARD_SELECTION",
                    "The same card cannot be kept twice.");
        }

        List<GameCard> kept = new ArrayList<>(EXCHANGE_DRAW);
        for (UUID cardId : keepCardIds) {
            GameCard card = pool.stream()
                    .filter(c -> c.getId().equals(cardId))
                    .findFirst()
                    .orElseThrow(() -> new BusinessException("INVALID_CARD_SELECTION",
                            "Card '" + cardId + "' was not part of the exchange pool."));
            kept.add(card);
        }

        List<GameCard> returned = pool.stream()
                .filter(card -> !keepCardIds.contains(card.getId()))
                .toList();

        player.setCards(kept);
        for (GameCard card : returned) {
            cardManager.returnToDeck(state.getDeck(), card);
        }
        cardManager.assertDeckIntegrity(state.getDeck(), state.getPlayers());

        state.setPendingAction(null);
        state.setActionExecuted(true);
        state.getLog().add(GameLogEntry.of("action",
                player.getUsername() + " completed an Exchange (kept 2 cards, returned 2 to the deck)."));

        Match match = turnManager.advanceTurn(matchId);
        state.setCurrentTurnPlayerId(match.getCurrentTurnPlayerId());
        state.setTurnNumber(match.getTurnNumber());
        state.setActionExecuted(false);
        state.setPhase(PHASE_IN_PROGRESS);

        log.info("Exchange resolved in match {} (kept {} returned {}), turn → {}",
                matchId, kept.size(), returned.size(), match.getTurnNumber());

        return gameStateMapper.toResponse(state, userId);
    }

    /**
     * Performs the Assassination action for the current turn holder.
     *
     * <p>Assassination claims the {@code ghatok} character, targets one
     * opponent and costs {@value #ASSASSINATE_COST} coins. Ownership of a
     * GHATOK card is NOT required — the claim may be bluffed and resolved later
     * by the Challenge Manager. The cost is only reserved on the pending action
     * and is never deducted up front, so a failed challenge or successful block
     * costs the actor nothing and the balance can never go negative.
     *
     * <p>A {@link PendingAction} block/challenge window is recorded in the
     * in-memory state. Nothing is deducted and the turn does NOT advance until
     * {@link #resolveAssassinate} is called with a resolution.
     *
     * @param matchId the match ID
     * @param userId  the acting player's user ID
     * @param targetPlayerId the user ID of the targeted opponent
     * @return the updated player-safe game state (with pending action visible)
     */
    @Transactional
    public GameStateResponse performAssassinate(UUID matchId, UUID userId, UUID targetPlayerId) {
        GameState state = getOrInitialize(matchId);
        state.setLastChallenge(null);

        if (state.getStatus() != MatchStatus.IN_PROGRESS
                && state.getStatus() != MatchStatus.CREATED) {
            throw new BusinessException("MATCH_NOT_ACTIVE",
                    "Cannot perform Assassination: match is not active.");
        }

        GamePlayerState player = state.getPlayers().stream()
                .filter(p -> p.getUserId().equals(userId))
                .findFirst()
                .orElseThrow(() -> new BusinessException("PLAYER_NOT_IN_MATCH",
                        "Cannot perform Assassination: player is not part of this match."));

        if (!userId.equals(state.getCurrentTurnPlayerId())) {
            throw new BusinessException("NOT_YOUR_TURN",
                    "It is not your turn.");
        }

        if (player.getStatus() != PlayerStatus.ACTIVE) {
            throw new BusinessException("PLAYER_ELIMINATED",
                    "Eliminated players cannot perform actions.");
        }

        if (player.getCards() == null || player.getCards().isEmpty()) {
            throw new BusinessException("NO_INFLUENCE",
                    "You need at least one influence card to perform an action.");
        }

        if (state.isActionExecuted()) {
            throw new BusinessException("ACTION_ALREADY_PERFORMED",
                    "You have already performed an action this turn.");
        }

        if (player.getCoins() < ASSASSINATE_COST) {
            throw new BusinessException("INSUFFICIENT_COINS",
                    "Assassination costs " + ASSASSINATE_COST
                            + " coins, but you only have " + player.getCoins() + ".");
        }

        if (targetPlayerId == null) {
            throw new BusinessException("TARGET_REQUIRED",
                    "You must target another player to perform an Assassination.");
        }

        if (userId.equals(targetPlayerId)) {
            throw new BusinessException("TARGET_SELF",
                    "You cannot assassinate yourself.");
        }

        GamePlayerState target = state.getPlayers().stream()
                .filter(p -> p.getUserId().equals(targetPlayerId))
                .findFirst()
                .orElseThrow(() -> new BusinessException("TARGET_NOT_IN_MATCH",
                        "Cannot perform Assassination: the target is not part of this match."));

        if (target.getStatus() != PlayerStatus.ACTIVE) {
            throw new BusinessException("TARGET_ELIMINATED",
                    "Cannot assassinate an eliminated player.");
        }

        if (target.getCards() == null || target.getCards().isEmpty()) {
            throw new BusinessException("TARGET_NO_INFLUENCE",
                    "The target has no influence cards left to lose.");
        }

        state.setPendingAction(PendingAction.builder()
                .type(ACTION_ASSASSINATE)
                .actorUserId(userId)
                .startedAt(LocalDateTime.now())
                .claimedCharacter(CHARACTER_GHATOK)
                .targetPlayerId(targetPlayerId)
                .reservedCoins(ASSASSINATE_COST)
                .build());
        state.setActionExecuted(true);
        state.getLog().add(GameLogEntry.of("action",
                player.getUsername() + " declared an Assassination on "
                        + target.getUsername() + ", claiming GHATOK ("
                        + ASSASSINATE_COST + " coins reserved). Block/challenge window open."));

        log.info("Player '{}' declared an Assassination on '{}' in match {} ({} coins reserved)",
                player.getUsername(), target.getUsername(), matchId, ASSASSINATE_COST);

        return gameStateMapper.toResponse(state, userId);
    }

    /**
     * Resolves a pending Assassination after its block/challenge window closes.
     *
     * <p>If {@code succeeded} is {@code true} the {@value #ASSASSINATE_COST}
     * reserved coins are deducted from the actor and the target loses one
     * influence card (the first card of their hand). If that was the target's
     * last card the target is eliminated. If {@code false} the action is
     * cancelled: nothing is deducted and the target keeps every card. The turn
     * advances in both cases.
     *
     * @param matchId   the match ID
     * @param userId    the actor's user ID (must match the pending action)
     * @param succeeded true if the Assassination succeeded and must be paid for
     * @return the updated player-safe game state
     */
    @Transactional
    public GameStateResponse resolveAssassinate(UUID matchId, UUID userId, boolean succeeded) {
        GameState state = getGameState(matchId);

        if (state.getPendingAction() == null) {
            throw new BusinessException("NO_PENDING_ACTION",
                    "No pending action to resolve.");
        }

        if (!ACTION_ASSASSINATE.equals(state.getPendingAction().getType())) {
            throw new BusinessException("INVALID_PENDING_ACTION",
                    "The pending action is not an Assassination.");
        }

        if (!userId.equals(state.getPendingAction().getActorUserId())) {
            throw new BusinessException("NOT_ACTOR",
                    "Only the action's actor can resolve the pending action.");
        }

        GamePlayerState player = state.getPlayers().stream()
                .filter(p -> p.getUserId().equals(userId))
                .findFirst()
                .orElseThrow(() -> new BusinessException("PLAYER_NOT_IN_MATCH",
                        "The actor is no longer part of this match."));

        UUID targetId = state.getPendingAction().getTargetPlayerId();
        GamePlayerState target = state.getPlayers().stream()
                .filter(p -> p.getUserId().equals(targetId))
                .findFirst()
                .orElseThrow(() -> new BusinessException("TARGET_NOT_IN_MATCH",
                        "The target is no longer part of this match."));

        state.setPendingAction(null);

        if (succeeded) {
            if (player.getCoins() < ASSASSINATE_COST) {
                throw new BusinessException("INSUFFICIENT_COINS",
                        "The actor no longer has the " + ASSASSINATE_COST
                                + " coins required to pay for the Assassination.");
            }
            if (target.getCards() == null || target.getCards().isEmpty()) {
                throw new BusinessException("TARGET_NO_INFLUENCE",
                        "The target has no influence cards left to lose.");
            }

            player.setCoins(player.getCoins() - ASSASSINATE_COST);

            List<GameCard> remaining = new ArrayList<>(target.getCards());
            remaining.remove(0);
            target.setCards(remaining);
            state.setRevealedCardsCount(state.getRevealedCardsCount() + 1);

            if (remaining.isEmpty()) {
                target.setStatus(PlayerStatus.ELIMINATED);
                state.getLog().add(GameLogEntry.of("elimination",
                        target.getUsername() + " lost their last influence card and was eliminated."));
            } else {
                state.getLog().add(GameLogEntry.of("action",
                        player.getUsername() + " assassinated an influence card of "
                                + target.getUsername() + " (" + ASSASSINATE_COST
                                + " coins paid)."));
            }

            log.info("Assassination succeeded in match {}: '{}' lost one influence card, "
                            + "'{}' paid {} coins",
                    matchId, target.getUsername(), player.getUsername(), ASSASSINATE_COST);
        } else {
            state.getLog().add(GameLogEntry.of("block",
                    "The Assassination by " + player.getUsername() + " was prevented. "
                            + "No coins paid, no influence lost."));
            log.info("Assassination cancelled in match {} (no coins paid, no influence lost)", matchId);
        }

        Match match = turnManager.advanceTurn(matchId);
        state.setCurrentTurnPlayerId(match.getCurrentTurnPlayerId());
        state.setTurnNumber(match.getTurnNumber());
        state.setActionExecuted(false);
        state.setPhase(PHASE_IN_PROGRESS);

        return gameStateMapper.toResponse(state, userId);
    }

    /**
     * Performs the Tax action for the current turn holder.
     *
     * <p>Tax claims the {@code minister} character and eventually grants
     * {@value #TAX_GAIN} coins. Ownership of a MINISTER card is NOT required —
     * the claim may be bluffed and resolved later by the Challenge Manager
     * (Module 18). A {@link PendingAction} challenge window is recorded in the
     * in-memory state; nothing is awarded until {@link #resolveTax} is called
     * with a resolution.
     *
     * @param matchId the match ID
     * @param userId  the acting player's user ID
     * @return the updated player-safe game state (with pending action visible)
     */
    @Transactional
    public GameStateResponse performTax(UUID matchId, UUID userId) {
        GameState state = getOrInitialize(matchId);
        state.setLastChallenge(null);

        if (state.getStatus() != MatchStatus.IN_PROGRESS
                && state.getStatus() != MatchStatus.CREATED) {
            throw new BusinessException("MATCH_NOT_ACTIVE",
                    "Cannot perform Tax: match is not active.");
        }

        GamePlayerState player = state.getPlayers().stream()
                .filter(p -> p.getUserId().equals(userId))
                .findFirst()
                .orElseThrow(() -> new BusinessException("PLAYER_NOT_IN_MATCH",
                        "Cannot perform Tax: player is not part of this match."));

        if (!userId.equals(state.getCurrentTurnPlayerId())) {
            throw new BusinessException("NOT_YOUR_TURN",
                    "It is not your turn.");
        }

        if (player.getStatus() != PlayerStatus.ACTIVE) {
            throw new BusinessException("PLAYER_ELIMINATED",
                    "Eliminated players cannot perform actions.");
        }

        if (player.getCards() == null || player.getCards().isEmpty()) {
            throw new BusinessException("NO_INFLUENCE",
                    "You need at least one influence card to perform an action.");
        }

        if (state.isActionExecuted()) {
            throw new BusinessException("ACTION_ALREADY_PERFORMED",
                    "You have already performed an action this turn.");
        }

        state.setPendingAction(PendingAction.builder()
                .type(ACTION_TAX)
                .actorUserId(userId)
                .startedAt(LocalDateTime.now())
                .claimedCharacter(CHARACTER_MINISTER)
                .build());
        state.setActionExecuted(true);
        state.getLog().add(GameLogEntry.of("action",
                player.getUsername() + " declared Tax, claiming Minister. Challenge window open."));

        log.info("Player '{}' declared Tax in match {} (challenge window open)",
                player.getUsername(), matchId);

        return gameStateMapper.toResponse(state, userId);
    }

    /**
     * Performs the Steal action for the current turn holder.
     *
     * <p>Steal claims the {@code dalal} character, targets one opponent and
     * eventually transfers up to {@value #STEAL_GAIN} coins from the target to
     * the actor. Ownership of a DALAL card is NOT required — the claim may be
     * bluffed and resolved later by the Challenge Manager (Module 18). A
     * {@link PendingAction} challenge window is recorded in the in-memory state;
     * nothing is transferred until {@link #resolveSteal} is called with a
     * resolution.
     *
     * @param matchId        the match ID
     * @param userId         the acting player's user ID
     * @param targetPlayerId the user ID of the targeted opponent
     * @return the updated player-safe game state (with pending action visible)
     */
    @Transactional
    public GameStateResponse performSteal(UUID matchId, UUID userId, UUID targetPlayerId) {
        GameState state = getOrInitialize(matchId);
        state.setLastChallenge(null);

        if (state.getStatus() != MatchStatus.IN_PROGRESS
                && state.getStatus() != MatchStatus.CREATED) {
            throw new BusinessException("MATCH_NOT_ACTIVE",
                    "Cannot perform Steal: match is not active.");
        }

        GamePlayerState player = state.getPlayers().stream()
                .filter(p -> p.getUserId().equals(userId))
                .findFirst()
                .orElseThrow(() -> new BusinessException("PLAYER_NOT_IN_MATCH",
                        "Cannot perform Steal: player is not part of this match."));

        if (!userId.equals(state.getCurrentTurnPlayerId())) {
            throw new BusinessException("NOT_YOUR_TURN",
                    "It is not your turn.");
        }

        if (player.getStatus() != PlayerStatus.ACTIVE) {
            throw new BusinessException("PLAYER_ELIMINATED",
                    "Eliminated players cannot perform actions.");
        }

        if (player.getCards() == null || player.getCards().isEmpty()) {
            throw new BusinessException("NO_INFLUENCE",
                    "You need at least one influence card to perform an action.");
        }

        if (state.isActionExecuted()) {
            throw new BusinessException("ACTION_ALREADY_PERFORMED",
                    "You have already performed an action this turn.");
        }

        if (targetPlayerId == null) {
            throw new BusinessException("TARGET_REQUIRED",
                    "You must target another player to perform a Steal.");
        }

        if (userId.equals(targetPlayerId)) {
            throw new BusinessException("TARGET_SELF",
                    "You cannot steal from yourself.");
        }

        GamePlayerState target = state.getPlayers().stream()
                .filter(p -> p.getUserId().equals(targetPlayerId))
                .findFirst()
                .orElseThrow(() -> new BusinessException("TARGET_NOT_IN_MATCH",
                        "Cannot perform Steal: the target is not part of this match."));

        if (target.getStatus() != PlayerStatus.ACTIVE) {
            throw new BusinessException("TARGET_ELIMINATED",
                    "Cannot steal from an eliminated player.");
        }

        if (target.getCoins() <= 0) {
            throw new BusinessException("TARGET_NO_COINS",
                    "The target has no coins to steal.");
        }

        state.setPendingAction(PendingAction.builder()
                .type(ACTION_STEAL)
                .actorUserId(userId)
                .startedAt(LocalDateTime.now())
                .claimedCharacter(CHARACTER_DALAL)
                .targetPlayerId(targetPlayerId)
                .build());
        state.setActionExecuted(true);
        state.getLog().add(GameLogEntry.of("action",
                player.getUsername() + " declared a Steal on " + target.getUsername()
                        + ", claiming DALAL. Challenge window open."));

        log.info("Player '{}' declared a Steal on '{}' in match {} (challenge window open)",
                player.getUsername(), target.getUsername(), matchId);

        return gameStateMapper.toResponse(state, userId);
    }

    /**
     * Resolves a pending Tax after its challenge window closes.
     *
     * <p>A truthful claim (unchallenged, or a challenge that was won) grants the
     * actor {@value #TAX_GAIN} coins. If the claim was exposed as a bluff the
     * Challenge Manager already cancelled the pending action, so {@code granted}
     * only reaches this seam as {@code true}. {@code false} is tolerated for
     * completeness and simply resolves without awarding coins.
     *
     * @param matchId the match ID
     * @param userId  the actor's user ID (must match the pending action)
     * @param granted true if the Tax is rewarded, false if it is cancelled
     * @return the updated player-safe game state
     */
    @Transactional
    public GameStateResponse resolveTax(UUID matchId, UUID userId, boolean granted) {
        GameState state = getGameState(matchId);

        if (state.getPendingAction() == null) {
            throw new BusinessException("NO_PENDING_ACTION",
                    "No pending action to resolve.");
        }

        if (!ACTION_TAX.equals(state.getPendingAction().getType())) {
            throw new BusinessException("INVALID_PENDING_ACTION",
                    "The pending action is not Tax.");
        }

        if (!userId.equals(state.getPendingAction().getActorUserId())) {
            throw new BusinessException("NOT_ACTOR",
                    "Only the action's actor can resolve the pending action.");
        }

        GamePlayerState player = state.getPlayers().stream()
                .filter(p -> p.getUserId().equals(userId))
                .findFirst()
                .orElseThrow(() -> new BusinessException("PLAYER_NOT_IN_MATCH",
                        "Player is not part of this match."));

        state.setPendingAction(null);

        if (granted) {
            player.setCoins(player.getCoins() + TAX_GAIN);
            state.getLog().add(GameLogEntry.of("action",
                    player.getUsername() + " collected Taxes: +" + TAX_GAIN + " coins."));
            log.info("Tax resolved in match {}: player '{}' gained {} coins",
                    matchId, player.getUsername(), TAX_GAIN);
        } else {
            state.getLog().add(GameLogEntry.of("block",
                    "The Tax by " + player.getUsername() + " was cancelled."));
            log.info("Tax cancelled in match {} (no coins awarded)", matchId);
        }

        Match match = turnManager.advanceTurn(matchId);
        state.setCurrentTurnPlayerId(match.getCurrentTurnPlayerId());
        state.setTurnNumber(match.getTurnNumber());
        state.setActionExecuted(false);
        state.setPhase(PHASE_IN_PROGRESS);

        return gameStateMapper.toResponse(state, userId);
    }

    /**
     * Resolves a pending Steal after its challenge window closes.
     *
     * <p>A truthful claim transfers up to {@value #STEAL_GAIN} coins from the
     * target to the actor (capped by the target's current balance). If the claim
     * was exposed as a bluff the Challenge Manager already cancelled the pending
     * action, so {@code granted} only reaches this seam as {@code true}.
     * {@code false} is tolerated for completeness and simply resolves without
     * transferring any coins.
     *
     * @param matchId the match ID
     * @param userId  the actor's user ID (must match the pending action)
     * @param granted true if the Steal is rewarded, false if it is cancelled
     * @return the updated player-safe game state
     */
    @Transactional
    public GameStateResponse resolveSteal(UUID matchId, UUID userId, boolean granted) {
        GameState state = getGameState(matchId);

        if (state.getPendingAction() == null) {
            throw new BusinessException("NO_PENDING_ACTION",
                    "No pending action to resolve.");
        }

        if (!ACTION_STEAL.equals(state.getPendingAction().getType())) {
            throw new BusinessException("INVALID_PENDING_ACTION",
                    "The pending action is not a Steal.");
        }

        if (!userId.equals(state.getPendingAction().getActorUserId())) {
            throw new BusinessException("NOT_ACTOR",
                    "Only the action's actor can resolve the pending action.");
        }

        GamePlayerState player = state.getPlayers().stream()
                .filter(p -> p.getUserId().equals(userId))
                .findFirst()
                .orElseThrow(() -> new BusinessException("PLAYER_NOT_IN_MATCH",
                        "The actor is no longer part of this match."));

        UUID targetId = state.getPendingAction().getTargetPlayerId();
        GamePlayerState target = state.getPlayers().stream()
                .filter(p -> p.getUserId().equals(targetId))
                .findFirst()
                .orElseThrow(() -> new BusinessException("TARGET_NOT_IN_MATCH",
                        "The target is no longer part of this match."));

        state.setPendingAction(null);

        if (granted) {
            int stolen = Math.min(STEAL_GAIN, target.getCoins());
            if (stolen > 0) {
                target.setCoins(target.getCoins() - stolen);
                player.setCoins(player.getCoins() + stolen);
                state.getLog().add(GameLogEntry.of("action",
                        player.getUsername() + " stole " + stolen + " coin(s) from "
                                + target.getUsername() + "."));
                log.info("Steal resolved in match {}: '{}' stole {} coins from '{}'",
                        matchId, player.getUsername(), stolen, target.getUsername());
            }
        } else {
            state.getLog().add(GameLogEntry.of("block",
                    "The Steal by " + player.getUsername() + " was cancelled."));
            log.info("Steal cancelled in match {} (no coins transferred)", matchId);
        }

        Match match = turnManager.advanceTurn(matchId);
        state.setCurrentTurnPlayerId(match.getCurrentTurnPlayerId());
        state.setTurnNumber(match.getTurnNumber());
        state.setActionExecuted(false);
        state.setPhase(PHASE_IN_PROGRESS);

        return gameStateMapper.toResponse(state, userId);
    }
}