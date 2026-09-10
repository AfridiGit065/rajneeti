package com.rajneeti.game;

import com.rajneeti.dto.game.GamePlayerDto;
import com.rajneeti.dto.game.GameStateResponse;
import com.rajneeti.entity.Match;
import com.rajneeti.entity.enums.MatchStatus;
import com.rajneeti.entity.enums.PlayerStatus;
import com.rajneeti.exception.BusinessException;
import com.rajneeti.repository.MatchPlayerRepository;
import com.rajneeti.repository.MatchRepository;
import com.rajneeti.service.TurnManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Module 11 — Income action tests.
 *
 * <p>Seeds the in-memory {@link GameStore} directly (no match initialization)
 * and drives {@link GameEngine#performIncome(UUID, UUID)}. The TurnManager is
 * mocked so turn advancement assertions focus on synchronization, not on the
 * turn logic itself (already covered by TurnManagerTest).
 */
@ExtendWith(MockitoExtension.class)
class GameEngineIncomeTest {

    @Mock
    private MatchRepository matchRepository;

    @Mock
    private MatchPlayerRepository matchPlayerRepository;

    @Mock
    private TurnManager turnManager;

    private final GameStore gameStore = new GameStore();
    private final CardManager cardManager = new CardManager();
    private final GameStateMapper gameStateMapper = new GameStateMapper();

    private GameEngine gameEngine;

    private UUID matchId;
    private UUID actorId;
    private UUID otherId;

    @BeforeEach
    void setUp() {
        matchId = UUID.randomUUID();
        actorId = UUID.randomUUID();
        otherId = UUID.randomUUID();

        gameEngine = new GameEngine(
                matchRepository, matchPlayerRepository, gameStore, cardManager, turnManager, gameStateMapper);
        gameStore.remove(matchId);
    }

    private GameState seed(GamePlayerState actor, GamePlayerState other,
                           UUID currentTurnPlayerId, MatchStatus status, boolean actionExecuted) {
        List<GamePlayerState> players = new ArrayList<>();
        if (actor != null) {
            players.add(actor);
        }
        if (other != null) {
            players.add(other);
        }

        GameState state = GameState.builder()
                .matchId(matchId)
                .roomId(UUID.randomUUID())
                .roomCode("RAJIN")
                .status(status)
                .phase(GameEngine.PHASE_IN_PROGRESS)
                .players(players)
                .turnOrder(players.stream().map(GamePlayerState::getUserId).toList())
                .currentTurnPlayerId(currentTurnPlayerId)
                .turnNumber(1)
                .deck(new ArrayList<>())
                .actionExecuted(actionExecuted)
                .log(new ArrayList<>())
                .build();
        gameStore.put(matchId, state);
        return state;
    }

    private GamePlayerState player(UUID userId, String username, int coins,
                                   PlayerStatus status, List<GameCard> cards) {
        return GamePlayerState.builder()
                .userId(userId)
                .username(username)
                .seatNumber(1)
                .status(status)
                .coins(coins)
                .cards(cards)
                .host(false)
                .build();
    }

    private List<GameCard> twoCards() {
        return List.of(
                GameCard.builder().id(UUID.randomUUID()).character(CharacterType.MINISTER).build(),
                GameCard.builder().id(UUID.randomUUID()).character(CharacterType.GHATOK).build());
    }

    private Match advancedMatch(UUID nextPlayerId, int turnNumber) {
        return Match.builder()
                .id(matchId)
                .status(MatchStatus.IN_PROGRESS)
                .currentTurnPlayerId(nextPlayerId)
                .turnNumber(turnNumber)
                .build();
    }

    private GamePlayerDto findPlayer(GameStateResponse response, UUID userId) {
        return response.getPlayers().stream()
                .filter(p -> p.getUserId().equals(userId))
                .findFirst()
                .orElseThrow();
    }

    private void assertRejectsWith(UUID actingUserId, String errorCode) {
        assertThatThrownBy(() -> gameEngine.performIncome(matchId, actingUserId))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(errorCode);
    }

    @Test
    @DisplayName("Income - grants exactly +1 coin (2 -> 3)")
    void income_grantsExactlyOneCoin() {
        GamePlayerState actor = player(actorId, "actor", 2, PlayerStatus.ACTIVE, twoCards());
        GamePlayerState other = player(otherId, "other", 2, PlayerStatus.ACTIVE, twoCards());
        seed(actor, other, actorId, MatchStatus.IN_PROGRESS, false);
        when(turnManager.advanceTurn(eq(matchId))).thenReturn(advancedMatch(otherId, 2));

        GameStateResponse response = gameEngine.performIncome(matchId, actorId);

        assertThat(actor.getCoins()).isEqualTo(3);
        assertThat(findPlayer(response, actorId).getCoins()).isEqualTo(3);
        assertThat(findPlayer(response, otherId).getCoins()).isEqualTo(2);
    }

    @Test
    @DisplayName("Income - works at 0 coins (never goes negative, 0 -> 1)")
    void income_zeroCoinsStillGainsOne() {
        GamePlayerState actor = player(actorId, "actor", 0, PlayerStatus.ACTIVE, twoCards());
        GamePlayerState other = player(otherId, "other", 2, PlayerStatus.ACTIVE, twoCards());
        seed(actor, other, actorId, MatchStatus.IN_PROGRESS, false);
        when(turnManager.advanceTurn(eq(matchId))).thenReturn(advancedMatch(otherId, 2));

        gameEngine.performIncome(matchId, actorId);

        assertThat(actor.getCoins()).isEqualTo(1);
    }

    @Test
    @DisplayName("Income - not your turn is rejected")
    void income_notYourTurnRejected() {
        GamePlayerState actor = player(actorId, "actor", 2, PlayerStatus.ACTIVE, twoCards());
        GamePlayerState other = player(otherId, "other", 2, PlayerStatus.ACTIVE, twoCards());
        seed(actor, other, otherId, MatchStatus.IN_PROGRESS, false);

        assertRejectsWith(actorId, "NOT_YOUR_TURN");
        verifyNoInteractions(turnManager);
        assertThat(actor.getCoins()).isEqualTo(2);
    }

    @Test
    @DisplayName("Income - eliminated player is rejected")
    void income_eliminatedRejected() {
        GamePlayerState actor = player(actorId, "actor", 2, PlayerStatus.ELIMINATED, twoCards());
        GamePlayerState other = player(otherId, "other", 2, PlayerStatus.ACTIVE, twoCards());
        seed(actor, other, actorId, MatchStatus.IN_PROGRESS, false);

        assertRejectsWith(actorId, "PLAYER_ELIMINATED");
        verifyNoInteractions(turnManager);
    }

    @Test
    @DisplayName("Income - player not in match is rejected")
    void income_notInMatchRejected() {
        GamePlayerState other = player(otherId, "other", 2, PlayerStatus.ACTIVE, twoCards());
        seed(null, other, actorId, MatchStatus.IN_PROGRESS, false);

        assertRejectsWith(actorId, "PLAYER_NOT_IN_MATCH");
        verifyNoInteractions(turnManager);
    }

    @Test
    @DisplayName("Income - inactive match is rejected")
    void income_inactiveMatchRejected() {
        GamePlayerState actor = player(actorId, "actor", 2, PlayerStatus.ACTIVE, twoCards());
        GamePlayerState other = player(otherId, "other", 2, PlayerStatus.ACTIVE, twoCards());
        seed(actor, other, actorId, MatchStatus.FINISHED, false);

        assertRejectsWith(actorId, "MATCH_NOT_ACTIVE");
        verifyNoInteractions(turnManager);
    }

    @Test
    @DisplayName("Income - second action in the same turn is rejected")
    void income_actionAlreadyPerformedRejected() {
        GamePlayerState actor = player(actorId, "actor", 4, PlayerStatus.ACTIVE, twoCards());
        GamePlayerState other = player(otherId, "other", 2, PlayerStatus.ACTIVE, twoCards());
        seed(actor, other, actorId, MatchStatus.IN_PROGRESS, true);

        assertRejectsWith(actorId, "ACTION_ALREADY_PERFORMED");
        verifyNoInteractions(turnManager);
        assertThat(actor.getCoins()).isEqualTo(4);
    }

    @Test
    @DisplayName("Income - player without influence cards is rejected")
    void income_noInfluenceRejected() {
        GamePlayerState actor = player(actorId, "actor", 2, PlayerStatus.ACTIVE, List.of());
        GamePlayerState other = player(otherId, "other", 2, PlayerStatus.ACTIVE, twoCards());
        seed(actor, other, actorId, MatchStatus.IN_PROGRESS, false);

        assertRejectsWith(actorId, "NO_INFLUENCE");
        verifyNoInteractions(turnManager);
    }

    @Test
    @DisplayName("Income - resolves instantly with no block/challenge window")
    void income_resolvesInstantly() {
        GamePlayerState actor = player(actorId, "actor", 2, PlayerStatus.ACTIVE, twoCards());
        GamePlayerState other = player(otherId, "other", 2, PlayerStatus.ACTIVE, twoCards());
        GameState state = seed(actor, other, actorId, MatchStatus.IN_PROGRESS, false);
        when(turnManager.advanceTurn(eq(matchId))).thenReturn(advancedMatch(otherId, 2));

        GameStateResponse response = gameEngine.performIncome(matchId, actorId);

        verify(turnManager).advanceTurn(eq(matchId));
        assertThat(response.getPhase()).isEqualTo(GameEngine.PHASE_IN_PROGRESS);
        assertThat(state.isActionExecuted()).isFalse();
        assertThat(actor.getCoins()).isEqualTo(3);
    }

    @Test
    @DisplayName("Income - turn state is synced from the advanced match")
    void income_syncsTurnStateAfterAdvance() {
        GamePlayerState actor = player(actorId, "actor", 2, PlayerStatus.ACTIVE, twoCards());
        GamePlayerState other = player(otherId, "other", 2, PlayerStatus.ACTIVE, twoCards());
        GameState state = seed(actor, other, actorId, MatchStatus.IN_PROGRESS, false);
        when(turnManager.advanceTurn(eq(matchId))).thenReturn(advancedMatch(otherId, 3));

        gameEngine.performIncome(matchId, actorId);

        assertThat(state.getCurrentTurnPlayerId()).isEqualTo(otherId);
        assertThat(state.getTurnNumber()).isEqualTo(3);
    }

    @Test
    @DisplayName("Income - records an 'action' log entry")
    void income_logsActionEntry() {
        GamePlayerState actor = player(actorId, "actor", 2, PlayerStatus.ACTIVE, twoCards());
        GamePlayerState other = player(otherId, "other", 2, PlayerStatus.ACTIVE, twoCards());
        GameState state = seed(actor, other, actorId, MatchStatus.IN_PROGRESS, false);
        when(turnManager.advanceTurn(eq(matchId))).thenReturn(advancedMatch(otherId, 2));

        gameEngine.performIncome(matchId, actorId);

        assertThat(state.getLog())
                .anyMatch(entry -> "action".equals(entry.getKind())
                        && entry.getText().contains("Income")
                        && entry.getText().contains("+1 coin"));
    }
}