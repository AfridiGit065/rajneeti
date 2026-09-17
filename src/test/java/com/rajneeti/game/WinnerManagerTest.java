package com.rajneeti.game;

import com.rajneeti.dto.game.GamePlayerDto;
import com.rajneeti.dto.game.GameStateResponse;
import com.rajneeti.entity.Match;
import com.rajneeti.entity.MatchPlayer;
import com.rajneeti.entity.User;
import com.rajneeti.entity.enums.MatchStatus;
import com.rajneeti.entity.enums.PlayerStatus;
import com.rajneeti.exception.BusinessException;
import com.rajneeti.repository.MatchPlayerRepository;
import com.rajneeti.repository.MatchRepository;
import com.rajneeti.service.TurnManager;
import com.rajneeti.websocket.WebSocketEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Module 21 — Winner Manager tests.
 *
 * <p>Seeds the in-memory {@link GameStore} with a real deck and mocked
 * repositories whose {@code match_players} rows mirror the seeded runtime
 * players. That lets the tests observe both faces of the Winner Manager:
 * the in-memory game-over transition and the persisted final outcome
 * (statuses, ranks, coins, winner, end time).
 *
 * <p>The Coup, Assassination and challenge integration tests drive the real
 * {@link GameEngine}, {@link ChallengeManager} and Module 20
 * {@link ActionResolver}, proving the Winner Manager is invoked from every
 * resolution seam that can remove a player's last influence card.
 */
@ExtendWith(MockitoExtension.class)
class WinnerManagerTest {

    @Mock
    private MatchRepository matchRepository;

    @Mock
    private MatchPlayerRepository matchPlayerRepository;

    @Mock
    private TurnManager turnManager;

    @Mock
    private WebSocketEventPublisher webSocketEventPublisher;

    private final GameStore gameStore = new GameStore();
    private final CardManager cardManager = new CardManager();
    private final GameStateMapper gameStateMapper = new GameStateMapper();

    private WinnerManager winnerManager;
    private GameEngine gameEngine;
    private ChallengeManager challengeManager;
    private ActionResolver actionResolver;

    private UUID matchId;
    private UUID actorId;
    private UUID otherId;
    private UUID thirdId;

    private Match matchRow;
    private List<MatchPlayer> persisted;

    @BeforeEach
    void setUp() {
        matchId = UUID.randomUUID();
        actorId = UUID.randomUUID();
        otherId = UUID.randomUUID();
        thirdId = UUID.randomUUID();

        winnerManager = new WinnerManager(matchRepository, matchPlayerRepository, webSocketEventPublisher);
        gameEngine = new GameEngine(
                matchRepository, matchPlayerRepository, gameStore,
                cardManager, turnManager, gameStateMapper, winnerManager, webSocketEventPublisher,
                new GameStateSyncService(gameStateMapper, webSocketEventPublisher));
        challengeManager = new ChallengeManager(gameEngine, cardManager, turnManager, winnerManager, webSocketEventPublisher,
                new GameStateSyncService(gameStateMapper, webSocketEventPublisher));
        actionResolver = new ActionResolver(gameEngine, gameStateMapper, winnerManager,
                new GameStateSyncService(gameStateMapper, webSocketEventPublisher));
        gameStore.remove(matchId);
    }

    /* ------------------------------------------------------------------ */
    /*  Helpers                                                           */
    /* ------------------------------------------------------------------ */

    private GameState seed(List<GameCard> deck, List<GamePlayerState> players, UUID currentTurn) {
        GameState state = GameState.builder()
                .matchId(matchId)
                .roomId(UUID.randomUUID())
                .roomCode("RAJWN")
                .status(MatchStatus.IN_PROGRESS)
                .phase(GameEngine.PHASE_IN_PROGRESS)
                .players(players)
                .turnOrder(players.stream().map(GamePlayerState::getUserId).toList())
                .currentTurnPlayerId(currentTurn)
                .turnNumber(1)
                .deck(deck)
                .log(new ArrayList<>())
                .build();
        gameStore.put(matchId, state);
        persist(players, currentTurn);
        return state;
    }

    private void persist(List<GamePlayerState> players, UUID currentTurn) {
        matchRow = Match.builder()
                .id(matchId)
                .status(MatchStatus.IN_PROGRESS)
                .currentTurnPlayerId(currentTurn)
                .turnNumber(1)
                .build();
        persisted = new ArrayList<>();
        for (GamePlayerState player : players) {
            User user = User.builder()
                    .id(player.getUserId())
                    .username(player.getUsername())
                    .email(player.getUsername() + "@test.local")
                    .password("secret")
                    .build();
            persisted.add(MatchPlayer.builder()
                    .id(UUID.randomUUID())
                    .match(matchRow)
                    .user(user)
                    .seatNumber(player.getSeatNumber())
                    .playerStatus(player.getStatus())
                    .coins(player.getCoins())
                    .coinsAtEnd(0)
                    .eliminated(false)
                    .build());
        }
        lenient().when(matchRepository.findById(matchId)).thenReturn(Optional.of(matchRow));
        lenient().when(matchPlayerRepository.findByMatchId(matchId)).thenReturn(persisted);
    }

    private GameState seedTwo(int actorCoins, int otherCoins, int otherInfluence) {
        List<GameCard> deck = cardManager.createDeck();
        List<GameCard> actorCards = cardManager.drawMany(deck, 2);
        List<GameCard> otherCards = otherInfluence > 0
                ? cardManager.drawMany(deck, otherInfluence)
                : new ArrayList<>();
        return seed(deck, List.of(
                player(actorId, "actor", actorCoins, PlayerStatus.ACTIVE, actorCards, 1),
                player(otherId, "other", otherCoins, PlayerStatus.ACTIVE, otherCards, 2)),
                actorId);
    }

    private GameState seedThree() {
        List<GameCard> deck = cardManager.createDeck();
        List<GameCard> actorCards = cardManager.drawMany(deck, 2);
        List<GameCard> otherCards = cardManager.drawMany(deck, 2);
        List<GameCard> thirdCards = cardManager.drawMany(deck, 2);
        return seed(deck, List.of(
                player(actorId, "actor", 2, PlayerStatus.ACTIVE, actorCards, 1),
                player(otherId, "other", 2, PlayerStatus.ACTIVE, otherCards, 2),
                player(thirdId, "third", 2, PlayerStatus.ACTIVE, thirdCards, 3)),
                actorId);
    }

    private GamePlayerState player(UUID userId, String username, int coins,
                                   PlayerStatus status, List<GameCard> cards, int seat) {
        return GamePlayerState.builder()
                .userId(userId)
                .username(username)
                .seatNumber(seat)
                .status(status)
                .coins(coins)
                .cards(cards)
                .host(false)
                .build();
    }

    private void stubAdvance(UUID nextPlayerId, int turnNumber) {
        when(turnManager.advanceTurn(matchId)).thenReturn(Match.builder()
                .id(matchId)
                .status(MatchStatus.IN_PROGRESS)
                .currentTurnPlayerId(nextPlayerId)
                .turnNumber(turnNumber)
                .build());
    }

    private GamePlayerState byId(GameState state, UUID userId) {
        return state.getPlayers().stream()
                .filter(p -> p.getUserId().equals(userId))
                .findFirst()
                .orElseThrow();
    }

    private MatchPlayer rowOf(UUID userId) {
        return persisted.stream()
                .filter(row -> row.getUser().getId().equals(userId))
                .findFirst()
                .orElseThrow();
    }

    /* ------------------------------------------------------------------ */
    /*  checkAndFinish — active-player counting                            */
    /* ------------------------------------------------------------------ */

    @Test
    @DisplayName("checkAndFinish - two active players: the game continues")
    void twoActivePlayers_gameContinues() {
        GameState state = seedTwo(2, 2, 2);

        assertThat(winnerManager.checkAndFinish(state)).isFalse();
        assertThat(state.getStatus()).isEqualTo(MatchStatus.IN_PROGRESS);
        assertThat(state.getWinnerUserId()).isNull();
    }

    @Test
    @DisplayName("checkAndFinish - one active player: that player wins and the match finishes")
    void oneActivePlayer_gameOver() {
        GameState state = seedTwo(2, 2, 1);
        GamePlayerState loser = byId(state, otherId);
        loser.setCards(new ArrayList<>());
        loser.setStatus(PlayerStatus.ELIMINATED);

        assertThat(winnerManager.checkAndFinish(state)).isTrue();

        assertThat(state.getStatus()).isEqualTo(MatchStatus.FINISHED);
        assertThat(state.getPhase()).isEqualTo(GameEngine.PHASE_GAME_OVER);
        assertThat(state.getWinnerUserId()).isEqualTo(actorId);
        assertThat(state.getEndedAt()).isNotNull();
        assertThat(state.getPendingAction()).isNull();
        assertThat(state.isActionExecuted()).isFalse();
        assertThat(state.getLog()).anyMatch(entry -> entry.getText().contains("wins the game"));
    }

    @Test
    @DisplayName("checkAndFinish - zero active players is rejected, not silently won")
    void zeroActivePlayers_invalidState() {
        GameState state = seedTwo(2, 2, 1);
        state.getPlayers().forEach(player -> {
            player.setCards(new ArrayList<>());
            player.setStatus(PlayerStatus.ELIMINATED);
        });

        assertThatThrownBy(() -> winnerManager.checkAndFinish(state))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo("INVALID_GAME_STATE");
    }

    @Test
    @DisplayName("checkAndFinish - a duplicate game-over call is a no-op")
    void duplicateGameOverPrevented() {
        GameState state = seedTwo(2, 2, 1);
        byId(state, otherId).setStatus(PlayerStatus.ELIMINATED);

        assertThat(winnerManager.checkAndFinish(state)).isTrue();
        assertThat(winnerManager.checkAndFinish(state)).isFalse();
    }

    /* ------------------------------------------------------------------ */
    /*  syncPlayerStates — persisted elimination + rank                    */
    /* ------------------------------------------------------------------ */

    @Test
    @DisplayName("syncPlayerStates - persists the eliminated player with a final rank")
    void syncPlayerStates_persistsElimination() {
        GameState state = seedTwo(2, 2, 1);
        byId(state, otherId).setStatus(PlayerStatus.ELIMINATED);

        assertThat(winnerManager.syncPlayerStates(state)).isTrue();

        MatchPlayer row = rowOf(otherId);
        assertThat(row.getPlayerStatus()).isEqualTo(PlayerStatus.ELIMINATED);
        assertThat(row.getEliminated()).isTrue();
        assertThat(row.getEliminatedAt()).isNotNull();
        assertThat(row.getFinalRank()).isEqualTo(2);
    }

    @Test
    @DisplayName("syncPlayerStates - is idempotent (keeps the original elimination time)")
    void syncPlayerStates_isIdempotent() {
        GameState state = seedTwo(2, 2, 1);
        byId(state, otherId).setStatus(PlayerStatus.ELIMINATED);

        winnerManager.syncPlayerStates(state);
        var firstTime = rowOf(otherId).getEliminatedAt();

        assertThat(winnerManager.syncPlayerStates(state)).isFalse();
        assertThat(rowOf(otherId).getEliminatedAt()).isEqualTo(firstTime);
        assertThat(rowOf(otherId).getFinalRank()).isEqualTo(2);
    }

    /* ------------------------------------------------------------------ */
    /*  finish — persisted Match + final snapshot                          */
    /* ------------------------------------------------------------------ */

    @Test
    @DisplayName("finish - the Match row becomes FINISHED with the winner and end time")
    void finish_persistsMatchOutcome() {
        GameState state = seedTwo(5, 3, 1);
        byId(state, otherId).setStatus(PlayerStatus.ELIMINATED);

        winnerManager.checkAndFinish(state);

        assertThat(matchRow.getStatus()).isEqualTo(MatchStatus.FINISHED);
        assertThat(matchRow.getWinner()).isNotNull();
        assertThat(matchRow.getWinner().getId()).isEqualTo(actorId);
        assertThat(matchRow.getEndedAt()).isNotNull();
        assertThat(matchRow.getPendingAction()).isNull();
    }

    @Test
    @DisplayName("finish - every player gets a final coin snapshot and the winner gets rank 1")
    void finish_persistsFinalPlayerStates() {
        GameState state = seedTwo(5, 3, 1);
        byId(state, otherId).setStatus(PlayerStatus.ELIMINATED);

        winnerManager.checkAndFinish(state);

        MatchPlayer winnerRow = rowOf(actorId);
        assertThat(winnerRow.getFinalRank()).isEqualTo(1);
        assertThat(winnerRow.getCoins()).isEqualTo(5);
        assertThat(winnerRow.getCoinsAtEnd()).isEqualTo(5);

        MatchPlayer loserRow = rowOf(otherId);
        assertThat(loserRow.getCoinsAtEnd()).isEqualTo(3);
        assertThat(loserRow.getFinalRank()).isEqualTo(2);
    }

    /* ------------------------------------------------------------------ */
    /*  Integration — every elimination seam invokes the Winner Manager    */
    /* ------------------------------------------------------------------ */

    @Test
    @DisplayName("Coup - eliminating the last opponent finishes the game")
    void coup_eliminatingLastOpponentFinishesGame() {
        GameState state = seedTwo(10, 2, 1);
        stubAdvance(otherId, 2);

        gameEngine.performCoup(matchId, actorId, otherId);

        assertThat(byId(state, otherId).getStatus()).isEqualTo(PlayerStatus.ELIMINATED);
        assertThat(state.getStatus()).isEqualTo(MatchStatus.FINISHED);
        assertThat(state.getWinnerUserId()).isEqualTo(actorId);
        assertThat(rowOf(otherId).getPlayerStatus()).isEqualTo(PlayerStatus.ELIMINATED);
        assertThat(matchRow.getWinner().getId()).isEqualTo(actorId);
    }

    @Test
    @DisplayName("Assassinate via the Action Resolver - eliminating the last opponent finishes the game")
    void assassinate_eliminatingLastOpponentFinishesGame() {
        GameState state = seedTwo(3, 5, 1);
        stubAdvance(otherId, 2);

        gameEngine.performAssassinate(matchId, actorId, otherId);
        GameStateResponse response = actionResolver.resolve(matchId, actorId);

        assertThat(byId(state, otherId).getStatus()).isEqualTo(PlayerStatus.ELIMINATED);
        assertThat(state.getLastActionResult().isEliminated()).isTrue();
        assertThat(state.getStatus()).isEqualTo(MatchStatus.FINISHED);
        assertThat(state.getWinnerUserId()).isEqualTo(actorId);
        assertThat(response.getStatus()).isEqualTo(MatchStatus.FINISHED);
        assertThat(response.getWinnerUserId()).isEqualTo(actorId);
    }

    @Test
    @DisplayName("Bluff challenge - eliminating the claimant finishes the game for the challenger")
    void bluffChallenge_eliminatingClaimantFinishesGame() {
        List<GameCard> deck = cardManager.createDeck();
        List<GameCard> actorCards = cardManager.drawMany(deck, 1);
        List<GameCard> otherCards = cardManager.drawMany(deck, 2);
        GameState state = seed(deck, List.of(
                player(actorId, "actor", 2, PlayerStatus.ACTIVE, actorCards, 1),
                player(otherId, "other", 2, PlayerStatus.ACTIVE, otherCards, 2)),
                actorId);
        stubAdvance(otherId, 2);

        gameEngine.performTax(matchId, actorId);
        challengeManager.challenge(matchId, otherId, null);

        assertThat(byId(state, actorId).getStatus()).isEqualTo(PlayerStatus.ELIMINATED);
        assertThat(state.getStatus()).isEqualTo(MatchStatus.FINISHED);
        assertThat(state.getWinnerUserId()).isEqualTo(otherId);
        assertThat(rowOf(actorId).getPlayerStatus()).isEqualTo(PlayerStatus.ELIMINATED);
    }

    /* ------------------------------------------------------------------ */
    /*  TurnManager integration                                            */
    /* ------------------------------------------------------------------ */

    @Test
    @DisplayName("TurnManager - skips a player the Winner Manager persisted as eliminated")
    void turnManager_skipsEliminatedPlayer() {
        GameState state = seedThree();
        byId(state, otherId).setStatus(PlayerStatus.ELIMINATED);
        winnerManager.syncPlayerStates(state);

        TurnManager realTurnManager = new TurnManager(matchRepository, matchPlayerRepository, webSocketEventPublisher);
        Match advanced = realTurnManager.advanceTurn(matchId);

        assertThat(advanced.getCurrentTurnPlayerId()).isEqualTo(thirdId);
        assertThat(advanced.getTurnNumber()).isEqualTo(2);
    }

    @Test
    @DisplayName("TurnManager - does not create a new turn once one active player remains")
    void turnManager_doesNotAdvanceWhenSingleSurvivor() {
        GameState state = seedThree();
        byId(state, otherId).setStatus(PlayerStatus.ELIMINATED);
        byId(state, thirdId).setStatus(PlayerStatus.ELIMINATED);
        winnerManager.syncPlayerStates(state);

        TurnManager realTurnManager = new TurnManager(matchRepository, matchPlayerRepository, webSocketEventPublisher);
        Match advanced = realTurnManager.advanceTurn(matchId);

        assertThat(advanced.getCurrentTurnPlayerId()).isEqualTo(actorId);
        assertThat(advanced.getTurnNumber()).isEqualTo(1);
    }

    /* ------------------------------------------------------------------ */
    /*  Actions are rejected after game over                               */
    /* ------------------------------------------------------------------ */

    @Test
    @DisplayName("game over - a further action is rejected with MATCH_NOT_ACTIVE")
    void actionRejectedAfterGameOver() {
        GameState state = seedTwo(2, 2, 1);
        byId(state, otherId).setStatus(PlayerStatus.ELIMINATED);
        winnerManager.checkAndFinish(state);

        assertThatThrownBy(() -> gameEngine.performIncome(matchId, actorId))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo("MATCH_NOT_ACTIVE");
    }

    @Test
    @DisplayName("game over - a further challenge is rejected with MATCH_NOT_ACTIVE")
    void challengeRejectedAfterGameOver() {
        GameState state = seedTwo(2, 2, 1);
        byId(state, otherId).setStatus(PlayerStatus.ELIMINATED);
        winnerManager.checkAndFinish(state);

        assertThatThrownBy(() -> challengeManager.challenge(matchId, otherId, null))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo("MATCH_NOT_ACTIVE");
    }

    @Test
    @DisplayName("game over - a further block is rejected with MATCH_NOT_ACTIVE")
    void blockRejectedAfterGameOver() {
        GameState state = seedTwo(2, 2, 1);
        byId(state, otherId).setStatus(PlayerStatus.ELIMINATED);
        winnerManager.checkAndFinish(state);

        BlockManager blockManager = new BlockManager(gameEngine, webSocketEventPublisher,
                new GameStateSyncService(gameStateMapper, webSocketEventPublisher));
        assertThatThrownBy(() -> blockManager.block(matchId, otherId, GameEngine.CHARACTER_MINISTER))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo("MATCH_NOT_ACTIVE");
    }

    /* ------------------------------------------------------------------ */
    /*  Player-safe game-over projection                                   */
    /* ------------------------------------------------------------------ */

    @Test
    @DisplayName("response - exposes the winner and final player states, never opponent cards")
    void gameOverResponse_isPlayerSafe() {
        GameState state = seedTwo(5, 3, 1);
        GamePlayerState loser = byId(state, otherId);
        loser.setCards(new ArrayList<>());
        loser.setStatus(PlayerStatus.ELIMINATED);
        winnerManager.checkAndFinish(state);

        GameStateResponse response = gameStateMapper.toResponse(state, actorId);

        assertThat(response.getStatus()).isEqualTo(MatchStatus.FINISHED);
        assertThat(response.getPhase()).isEqualTo(GameEngine.PHASE_GAME_OVER);
        assertThat(response.getWinnerUserId()).isEqualTo(actorId);
        assertThat(response.getEndedAt()).isNotNull();

        GamePlayerDto winnerDto = response.getPlayers().stream()
                .filter(p -> p.getUserId().equals(actorId)).findFirst().orElseThrow();
        assertThat(winnerDto.isAlive()).isTrue();
        assertThat(winnerDto.getCards()).hasSize(2);
        assertThat(winnerDto.getInfluenceCount()).isEqualTo(2);

        GamePlayerDto loserDto = response.getPlayers().stream()
                .filter(p -> p.getUserId().equals(otherId)).findFirst().orElseThrow();
        assertThat(loserDto.isAlive()).isFalse();
        assertThat(loserDto.getCards()).isNull();
        assertThat(loserDto.getInfluenceCount()).isZero();
    }
}
