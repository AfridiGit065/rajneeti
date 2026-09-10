package com.rajneeti.game;

import com.rajneeti.dto.game.GameStateResponse;
import com.rajneeti.entity.Match;
import com.rajneeti.entity.MatchPlayer;
import com.rajneeti.entity.Room;
import com.rajneeti.entity.User;
import com.rajneeti.entity.enums.PlayerStatus;
import com.rajneeti.exception.BusinessException;
import com.rajneeti.exception.MatchNotFoundException;
import com.rajneeti.repository.MatchPlayerRepository;
import com.rajneeti.repository.MatchRepository;
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
 * <p>Turn progression is NOT implemented here — that is the responsibility of
 * the existing {@code TurnManager} (Module 09). Gameplay actions (income, tax,
 * steal, challenge, block, ...) are intentionally out of scope for this module.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GameEngine {

    /** Every player starts with 2 coins. */
    public static final int STARTING_COINS = 2;

    /** Every player starts with 2 influence cards. */
    public static final int STARTING_INFLUENCE = CardManager.STARTING_HAND;

    public static final String PHASE_SETUP = "setup";
    public static final String PHASE_IN_PROGRESS = "in_progress";
    public static final String PHASE_GAME_OVER = "game_over";

    private final MatchRepository matchRepository;
    private final MatchPlayerRepository matchPlayerRepository;
    private final GameStore gameStore;
    private final CardManager cardManager;
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
}