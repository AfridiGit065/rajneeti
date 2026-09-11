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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Module 12 — Foreign Aid tests.
 *
 * <p>Seeds the in-memory {@link GameStore} directly and drives
 * {@link GameEngine#performForeignAid} and
 * {@link GameEngine#resolveForeignAid}. The TurnManager is mocked.
 */
@ExtendWith(MockitoExtension.class)
class GameEngineForeignAidTest {

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
                matchRepository, matchPlayerRepository, gameStore,
                cardManager, turnManager, gameStateMapper);
        gameStore.remove(matchId);
    }

    /* ------------------------------------------------------------------ */
    /*  Helpers                                                           */
    /* ------------------------------------------------------------------ */

    private GameState seed(GamePlayerState actor, GamePlayerState other,
                           UUID currentTurnPlayerId, MatchStatus status,
                           boolean actionExecuted, PendingAction pending) {
        List<GamePlayerState> players = new ArrayList<>();
        if (actor != null) players.add(actor);
        if (other != null) players.add(other);

        GameState state = GameState.builder()
                .matchId(matchId)
                .roomId(UUID.randomUUID())
                .roomCode("RAJFG")
                .status(status)
                .phase(GameEngine.PHASE_IN_PROGRESS)
                .players(players)
                .turnOrder(players.stream().map(GamePlayerState::getUserId).toList())
                .currentTurnPlayerId(currentTurnPlayerId)
                .turnNumber(1)
                .deck(new ArrayList<>())
                .actionExecuted(actionExecuted)
                .pendingAction(pending)
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
        assertThatThrownBy(() -> gameEngine.performForeignAid(matchId, actingUserId))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(errorCode);
    }

    /* ------------------------------------------------------------------ */
    /*  Tests — performForeignAid                                         */
    /* ------------------------------------------------------------------ */

    @Test
    @DisplayName("Foreign Aid - opens block window without awarding coins")
    void foreignAid_opensBlockWindow_noCoinsYet() {
        GamePlayerState actor = player(actorId, "actor", 2, PlayerStatus.ACTIVE, twoCards());
        GamePlayerState other = player(otherId, "other", 2, PlayerStatus.ACTIVE, twoCards());
        seed(actor, other, actorId, MatchStatus.IN_PROGRESS, false, null);

        GameStateResponse response = gameEngine.performForeignAid(matchId, actorId);

        assertThat(actor.getCoins()).isEqualTo(2);
        assertThat(findPlayer(response, actorId).getCoins()).isEqualTo(2);
    }

    @Test
    @DisplayName("Foreign Aid - pending action state is correctly prepared")
    void foreignAid_blockWindowStateCorrect() {
        GamePlayerState actor = player(actorId, "actor", 2, PlayerStatus.ACTIVE, twoCards());
        GamePlayerState other = player(otherId, "other", 2, PlayerStatus.ACTIVE, twoCards());
        GameState state = seed(actor, other, actorId, MatchStatus.IN_PROGRESS, false, null);

        gameEngine.performForeignAid(matchId, actorId);

        assertThat(state.getPendingAction()).isNotNull();
        assertThat(state.getPendingAction().getType()).isEqualTo("FOREIGN_AID");
        assertThat(state.getPendingAction().getActorUserId()).isEqualTo(actorId);
        assertThat(state.getPendingAction().getStartedAt()).isBeforeOrEqualTo(LocalDateTime.now());
    }

    @Test
    @DisplayName("Foreign Aid - actionExecuted flag is set")
    void foreignAid_setsActionExecuted() {
        GamePlayerState actor = player(actorId, "actor", 2, PlayerStatus.ACTIVE, twoCards());
        GamePlayerState other = player(otherId, "other", 2, PlayerStatus.ACTIVE, twoCards());
        GameState state = seed(actor, other, actorId, MatchStatus.IN_PROGRESS, false, null);

        gameEngine.performForeignAid(matchId, actorId);

        assertThat(state.isActionExecuted()).isTrue();
    }

    @Test
    @DisplayName("Foreign Aid - not your turn is rejected")
    void foreignAid_notYourTurnRejected() {
        GamePlayerState actor = player(actorId, "actor", 2, PlayerStatus.ACTIVE, twoCards());
        GamePlayerState other = player(otherId, "other", 2, PlayerStatus.ACTIVE, twoCards());
        seed(actor, other, otherId, MatchStatus.IN_PROGRESS, false, null);

        assertRejectsWith(actorId, "NOT_YOUR_TURN");
        verifyNoInteractions(turnManager);
    }

    @Test
    @DisplayName("Foreign Aid - eliminated player is rejected")
    void foreignAid_eliminatedRejected() {
        GamePlayerState actor = player(actorId, "actor", 2, PlayerStatus.ELIMINATED, twoCards());
        GamePlayerState other = player(otherId, "other", 2, PlayerStatus.ACTIVE, twoCards());
        seed(actor, other, actorId, MatchStatus.IN_PROGRESS, false, null);

        assertRejectsWith(actorId, "PLAYER_ELIMINATED");
        verifyNoInteractions(turnManager);
    }

    @Test
    @DisplayName("Foreign Aid - player not in match is rejected")
    void foreignAid_notInMatchRejected() {
        GamePlayerState other = player(otherId, "other", 2, PlayerStatus.ACTIVE, twoCards());
        seed(null, other, actorId, MatchStatus.IN_PROGRESS, false, null);

        assertRejectsWith(actorId, "PLAYER_NOT_IN_MATCH");
        verifyNoInteractions(turnManager);
    }

    @Test
    @DisplayName("Foreign Aid - inactive match is rejected")
    void foreignAid_inactiveMatchRejected() {
        GamePlayerState actor = player(actorId, "actor", 2, PlayerStatus.ACTIVE, twoCards());
        GamePlayerState other = player(otherId, "other", 2, PlayerStatus.ACTIVE, twoCards());
        seed(actor, other, actorId, MatchStatus.FINISHED, false, null);

        assertRejectsWith(actorId, "MATCH_NOT_ACTIVE");
        verifyNoInteractions(turnManager);
    }

    @Test
    @DisplayName("Foreign Aid - duplicate action in the same turn is rejected")
    void foreignAid_duplicateRejected() {
        GamePlayerState actor = player(actorId, "actor", 2, PlayerStatus.ACTIVE, twoCards());
        GamePlayerState other = player(otherId, "other", 2, PlayerStatus.ACTIVE, twoCards());
        seed(actor, other, actorId, MatchStatus.IN_PROGRESS, true, null);

        assertRejectsWith(actorId, "ACTION_ALREADY_PERFORMED");
        verifyNoInteractions(turnManager);
    }

    /* ------------------------------------------------------------------ */
    /*  Tests — resolveForeignAid                                         */
    /* ------------------------------------------------------------------ */

    @Test
    @DisplayName("Resolve Foreign Aid - unblocked: grants +2 coins and advances turn")
    void resolveForeignAid_unblocked_grantsTwoCoins() {
        GamePlayerState actor = player(actorId, "actor", 2, PlayerStatus.ACTIVE, twoCards());
        GamePlayerState other = player(otherId, "other", 2, PlayerStatus.ACTIVE, twoCards());
        GameState state = seed(actor, other, actorId, MatchStatus.IN_PROGRESS, true,
                PendingAction.builder()
                        .type("FOREIGN_AID").actorUserId(actorId)
                        .startedAt(LocalDateTime.now()).build());
        when(turnManager.advanceTurn(eq(matchId))).thenReturn(advancedMatch(otherId, 2));

        GameStateResponse response = gameEngine.resolveForeignAid(matchId, actorId, false);

        assertThat(actor.getCoins()).isEqualTo(4);
        assertThat(findPlayer(response, actorId).getCoins()).isEqualTo(4);
        assertThat(state.getPendingAction()).isNull();
        verify(turnManager).advanceTurn(eq(matchId));
    }

    @Test
    @DisplayName("Resolve Foreign Aid - blocked: action cancelled, no coins, turn advances")
    void resolveForeignAid_blocked_cancelsAction() {
        GamePlayerState actor = player(actorId, "actor", 2, PlayerStatus.ACTIVE, twoCards());
        GamePlayerState other = player(otherId, "other", 2, PlayerStatus.ACTIVE, twoCards());
        GameState state = seed(actor, other, actorId, MatchStatus.IN_PROGRESS, true,
                PendingAction.builder()
                        .type("FOREIGN_AID").actorUserId(actorId)
                        .startedAt(LocalDateTime.now()).build());
        when(turnManager.advanceTurn(eq(matchId))).thenReturn(advancedMatch(otherId, 2));

        GameStateResponse response = gameEngine.resolveForeignAid(matchId, actorId, true);

        assertThat(actor.getCoins()).isEqualTo(2);
        assertThat(findPlayer(response, actorId).getCoins()).isEqualTo(2);
        assertThat(state.getPendingAction()).isNull();
        verify(turnManager).advanceTurn(eq(matchId));
    }

    @Test
    @DisplayName("Resolve Foreign Aid - pending action required")
    void resolveForeignAid_noPendingActionRejected() {
        GamePlayerState actor = player(actorId, "actor", 2, PlayerStatus.ACTIVE, twoCards());
        GamePlayerState other = player(otherId, "other", 2, PlayerStatus.ACTIVE, twoCards());
        seed(actor, other, actorId, MatchStatus.IN_PROGRESS, false, null);

        assertThatThrownBy(() -> gameEngine.resolveForeignAid(matchId, actorId, false))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo("NO_PENDING_ACTION");
    }

    @Test
    @DisplayName("Resolve Foreign Aid - only the actor may resolve")
    void resolveForeignAid_actorOnly() {
        GamePlayerState actor = player(actorId, "actor", 2, PlayerStatus.ACTIVE, twoCards());
        GamePlayerState other = player(otherId, "other", 2, PlayerStatus.ACTIVE, twoCards());
        seed(actor, other, actorId, MatchStatus.IN_PROGRESS, true,
                PendingAction.builder()
                        .type("FOREIGN_AID").actorUserId(actorId)
                        .startedAt(LocalDateTime.now()).build());

        assertThatThrownBy(() -> gameEngine.resolveForeignAid(matchId, otherId, false))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo("NOT_ACTOR");
    }

    @Test
    @DisplayName("Resolve Foreign Aid - turn state synced from advanced match")
    void resolveForeignAid_syncsTurnState() {
        GamePlayerState actor = player(actorId, "actor", 2, PlayerStatus.ACTIVE, twoCards());
        GamePlayerState other = player(otherId, "other", 2, PlayerStatus.ACTIVE, twoCards());
        GameState state = seed(actor, other, actorId, MatchStatus.IN_PROGRESS, true,
                PendingAction.builder()
                        .type("FOREIGN_AID").actorUserId(actorId)
                        .startedAt(LocalDateTime.now()).build());
        when(turnManager.advanceTurn(eq(matchId))).thenReturn(advancedMatch(otherId, 3));

        gameEngine.resolveForeignAid(matchId, actorId, false);

        assertThat(state.getCurrentTurnPlayerId()).isEqualTo(otherId);
        assertThat(state.getTurnNumber()).isEqualTo(3);
        assertThat(state.isActionExecuted()).isFalse();
        assertThat(state.getPhase()).isEqualTo(GameEngine.PHASE_IN_PROGRESS);
    }

    @Test
    @DisplayName("Existing Income still works in a fresh game state")
    void existingIncome_stillWorks() {
        GamePlayerState actor = player(actorId, "actor", 2, PlayerStatus.ACTIVE, twoCards());
        GamePlayerState other = player(otherId, "other", 2, PlayerStatus.ACTIVE, twoCards());
        seed(actor, other, actorId, MatchStatus.IN_PROGRESS, false, null);
        when(turnManager.advanceTurn(eq(matchId))).thenReturn(advancedMatch(otherId, 2));

        GameStateResponse response = gameEngine.performIncome(matchId, actorId);

        assertThat(actor.getCoins()).isEqualTo(3);
        assertThat(findPlayer(response, actorId).getCoins()).isEqualTo(3);
        verify(turnManager).advanceTurn(eq(matchId));
    }
}
