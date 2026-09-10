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
import java.util.List;
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
}