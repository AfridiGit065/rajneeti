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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Module 17 — Coup tests.
 *
 * <p>Seeds the in-memory {@link GameStore} with a real 15-card deck (so deck
 * integrity assertions hold where relevant) and drives
 * {@link GameEngine#performCoup}. Coup resolves instantly: no pending action,
 * no challenge/block window. The TurnManager is mocked.
 */
@ExtendWith(MockitoExtension.class)
class GameEngineCoupTest {

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

    /**
     * Seeds a game with a full 15-card deck. All card IDs come from one deck,
     * so deck integrity always holds.
     */
    private GameState seed(int actorCoins, int targetInfluence,
                           UUID currentTurnPlayerId, PlayerStatus targetStatus) {
        List<GameCard> full = cardManager.createDeck();
        List<GameCard> actorCards = cardManager.drawMany(full, 2);
        List<GameCard> otherCards = targetInfluence > 0
                ? cardManager.drawMany(full, targetInfluence)
                : new ArrayList<>();

        GamePlayerState actor = player(actorId, "actor", actorCoins, PlayerStatus.ACTIVE, actorCards);
        GamePlayerState other = player(otherId, "other", 2, targetStatus, otherCards);
        List<GamePlayerState> players = List.of(actor, other);

        GameState state = GameState.builder()
                .matchId(matchId)
                .roomId(UUID.randomUUID())
                .roomCode("RAJASN")
                .status(MatchStatus.IN_PROGRESS)
                .phase(GameEngine.PHASE_IN_PROGRESS)
                .players(players)
                .turnOrder(players.stream().map(GamePlayerState::getUserId).toList())
                .currentTurnPlayerId(currentTurnPlayerId)
                .turnNumber(1)
                .deck(full)
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

    private void stubAdvance() {
        when(turnManager.advanceTurn(matchId)).thenReturn(advancedMatch(otherId, 2));
    }

    private void assertPerformsRejectWith(int actorCoins, int targetInfluence,
                                          PlayerStatus targetStatus, UUID targetId,
                                          String errorCode) {
        seed(actorCoins, targetInfluence, actorId, targetStatus);
        assertThatThrownBy(() -> gameEngine.performCoup(matchId, actorId, targetId))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(errorCode);
    }

    /* ------------------------------------------------------------------ */
    /*  Tests — valid Coup, cost, influence loss                          */
    /* ------------------------------------------------------------------ */

    @Test
    @DisplayName("Coup - a valid Coup resolves instantly: -7 coins, -1 influence, turn advances")
    void coup_validCoupResolvesInstantly() {
        GameState state = seed(10, 2, actorId, PlayerStatus.ACTIVE);
        stubAdvance();

        GameStateResponse response = gameEngine.performCoup(matchId, actorId, otherId);

        GamePlayerState actor = state.getPlayers().get(0);
        GamePlayerState target = state.getPlayers().get(1);
        assertThat(actor.getCoins()).isEqualTo(3);
        assertThat(target.getCards()).hasSize(1);
        assertThat(target.getStatus()).isEqualTo(PlayerStatus.ACTIVE);
        assertThat(state.getPendingAction()).isNull();
        assertThat(state.getRevealedCardsCount()).isEqualTo(1);
        assertThat(state.isActionExecuted()).isFalse();
        assertThat(response.getCurrentTurnPlayerId()).isEqualTo(otherId);
        assertThat(response.getTurnNumber()).isEqualTo(2);
        assertThat(findPlayer(response, actorId).getCoins()).isEqualTo(3);
        assertThat(findPlayer(response, otherId).getInfluenceCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("Coup - accepts a balance of exactly 7 coins")
    void coup_acceptsExactlySevenCoins() {
        GameState state = seed(7, 2, actorId, PlayerStatus.ACTIVE);
        stubAdvance();

        gameEngine.performCoup(matchId, actorId, otherId);

        assertThat(state.getPlayers().get(0).getCoins()).isZero();
        assertThat(state.getPlayers().get(1).getCards()).hasSize(1);
    }

    @Test
    @DisplayName("Coup - rejects a balance of fewer than 7 coins")
    void coup_rejectsFewerThanSevenCoins() {
        assertPerformsRejectWith(6, 2, PlayerStatus.ACTIVE, otherId, "INSUFFICIENT_COINS");
        verifyNoInteractions(turnManager);
    }

    @Test
    @DisplayName("Coup - deducts exactly 7 coins from the actor (12 -> 5)")
    void coup_deductsExactlySevenCoins() {
        GameState state = seed(12, 2, actorId, PlayerStatus.ACTIVE);
        stubAdvance();

        gameEngine.performCoup(matchId, actorId, otherId);

        assertThat(state.getPlayers().get(0).getCoins()).isEqualTo(5);
    }

    @Test
    @DisplayName("Coup - the target loses exactly 1 influence card")
    void coup_targetLosesExactlyOneInfluence() {
        GameState state = seed(10, 2, actorId, PlayerStatus.ACTIVE);
        stubAdvance();

        gameEngine.performCoup(matchId, actorId, otherId);

        GamePlayerState target = state.getPlayers().get(1);
        assertThat(target.getCards()).hasSize(1);
        assertThat(target.getStatus()).isEqualTo(PlayerStatus.ACTIVE);
        assertThat(state.getRevealedCardsCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("Coup - removing the target's last card marks the target ELIMINATED")
    void coup_lastCardEliminatesTarget() {
        GameState state = seed(10, 1, actorId, PlayerStatus.ACTIVE);
        stubAdvance();

        gameEngine.performCoup(matchId, actorId, otherId);

        GamePlayerState target = state.getPlayers().get(1);
        assertThat(target.getCards()).isEmpty();
        assertThat(target.getStatus()).isEqualTo(PlayerStatus.ELIMINATED);
        assertThat(state.getRevealedCardsCount()).isEqualTo(1);
    }

    /* ------------------------------------------------------------------ */
    /*  Tests — mandatory Coup rule                                       */
    /* ------------------------------------------------------------------ */

    @Test
    @DisplayName("Coup - a player with 10+ coins is allowed to Coup (mandatory Coup does not block Coup itself)")
    void coup_allowedAtTenPlusCoins() {
        GameState state = seed(10, 2, actorId, PlayerStatus.ACTIVE);
        stubAdvance();

        gameEngine.performCoup(matchId, actorId, otherId);

        assertThat(state.getPlayers().get(0).getCoins()).isEqualTo(3);
        assertThat(state.getPlayers().get(1).getCards()).hasSize(1);
    }

    @Test
    @DisplayName("Coup - other actions are rejected when a forced Coup applies (10+ coins)")
    void coup_rejectsOtherActionsWhenForced() {
        seed(10, 2, actorId, PlayerStatus.ACTIVE);

        assertThatThrownBy(() -> gameEngine.performIncome(matchId, actorId))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo("MANDATORY_COUP");
        verifyNoInteractions(turnManager);
    }

    @Test
    @DisplayName("Coup - other block-window actions are rejected when a forced Coup applies (10+ coins)")
    void coup_rejectsForeignAidWhenForced() {
        seed(10, 2, actorId, PlayerStatus.ACTIVE);

        assertThatThrownBy(() -> gameEngine.performForeignAid(matchId, actorId))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo("MANDATORY_COUP");
        verifyNoInteractions(turnManager);
    }

    @Test
    @DisplayName("Coup - exactly 9 coins does NOT trigger the forced Coup rule")
    void coup_noForcedCoupBelowTen() {
        GameState state = seed(9, 2, actorId, PlayerStatus.ACTIVE);
        stubAdvance();

        gameEngine.performCoup(matchId, actorId, otherId);

        GamePlayerState actor = state.getPlayers().get(0);
        assertThat(actor.getCoins()).isEqualTo(2);
        assertThat(actor.getStatus()).isEqualTo(PlayerStatus.ACTIVE);
        assertThat(state.getPlayers().get(1).getCards()).hasSize(1);
    }

    /* ------------------------------------------------------------------ */
    /*  Tests — target validation                                         */
    /* ------------------------------------------------------------------ */

    @Test
    @DisplayName("Coup - rejects targeting yourself")
    void coup_rejectsSelfTarget() {
        assertPerformsRejectWith(10, 2, PlayerStatus.ACTIVE, actorId, "TARGET_SELF");
    }

    @Test
    @DisplayName("Coup - rejects targeting an eliminated player")
    void coup_rejectsEliminatedTarget() {
        assertPerformsRejectWith(10, 2, PlayerStatus.ELIMINATED, otherId, "TARGET_ELIMINATED");
    }

    @Test
    @DisplayName("Coup - rejects a target with no influence cards")
    void coup_rejectsTargetWithoutInfluence() {
        assertPerformsRejectWith(10, 0, PlayerStatus.ACTIVE, otherId, "TARGET_NO_INFLUENCE");
    }

    @Test
    @DisplayName("Coup - rejects a target that is not in the match")
    void coup_rejectsUnknownTarget() {
        seed(10, 2, actorId, PlayerStatus.ACTIVE);
        assertThatThrownBy(() -> gameEngine.performCoup(
                matchId, actorId, UUID.randomUUID()))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo("TARGET_NOT_IN_MATCH");
    }

    @Test
    @DisplayName("Coup - rejects a missing target")
    void coup_rejectsMissingTarget() {
        seed(10, 2, actorId, PlayerStatus.ACTIVE);
        assertThatThrownBy(() -> gameEngine.performCoup(matchId, actorId, null))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo("TARGET_REQUIRED");
    }

    /* ------------------------------------------------------------------ */
    /*  Tests — turn/player/match validation                              */
    /* ------------------------------------------------------------------ */

    @Test
    @DisplayName("Coup - rejects when it is not the actor's turn")
    void coup_notYourTurnRejected() {
        seed(10, 2, otherId, PlayerStatus.ACTIVE);
        assertThatThrownBy(() -> gameEngine.performCoup(matchId, actorId, otherId))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo("NOT_YOUR_TURN");
    }

    @Test
    @DisplayName("Coup - rejects an eliminated actor")
    void coup_eliminatedActorRejected() {
        GameState state = seed(10, 2, actorId, PlayerStatus.ACTIVE);
        state.getPlayers().get(0).setStatus(PlayerStatus.ELIMINATED);

        assertThatThrownBy(() -> gameEngine.performCoup(matchId, actorId, otherId))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo("PLAYER_ELIMINATED");
    }

    @Test
    @DisplayName("Coup - rejects a second action in the same turn")
    void coup_actionAlreadyPerformedRejected() {
        GameState state = seed(10, 2, actorId, PlayerStatus.ACTIVE);
        state.setActionExecuted(true);

        assertThatThrownBy(() -> gameEngine.performCoup(matchId, actorId, otherId))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo("ACTION_ALREADY_PERFORMED");
    }

    /* ------------------------------------------------------------------ */
    /*  Tests — no challenge / no block window                            */
    /* ------------------------------------------------------------------ */

    @Test
    @DisplayName("Coup - opens no challenge window (no pending action)")
    void coup_opensNoChallengeWindow() {
        GameState state = seed(10, 2, actorId, PlayerStatus.ACTIVE);
        stubAdvance();

        gameEngine.performCoup(matchId, actorId, otherId);

        assertThat(state.getPendingAction()).isNull();
        verify(turnManager).advanceTurn(matchId);
        assertThat(state.isActionExecuted()).isFalse();
        assertThat(state.getCurrentTurnPlayerId()).isEqualTo(otherId);
    }

    @Test
    @DisplayName("Coup - opens no block window (resolves instantly, turn advances immediately)")
    void coup_opensNoBlockWindow() {
        GameState state = seed(10, 2, actorId, PlayerStatus.ACTIVE);
        stubAdvance();

        GameStateResponse response = gameEngine.performCoup(matchId, actorId, otherId);

        assertThat(state.getPendingAction()).isNull();
        assertThat(response.getPendingAction()).isNull();
        verify(turnManager).advanceTurn(matchId);
        assertThat(findPlayer(response, otherId).getInfluenceCount()).isEqualTo(1);
    }

    /* ------------------------------------------------------------------ */
    /*  Tests — earlier modules still work                                 */
    /* ------------------------------------------------------------------ */

    @Test
    @DisplayName("Coup - previous actions (income, foreign aid) still work alongside it")
    void coup_previousModulesStillWork() {
        GameState state = seed(2, 1, actorId, PlayerStatus.ACTIVE);
        stubAdvance();

        GameStateResponse income = gameEngine.performIncome(matchId, actorId);
        assertThat(findPlayer(income, actorId).getCoins()).isEqualTo(3);

        // Income advanced the turn to the opponent; bring it back so the actor
        // can demonstrate a block-window action in the same test.
        state.setCurrentTurnPlayerId(actorId);
        gameEngine.performForeignAid(matchId, actorId);
        assertThat(state.getPendingAction().getType()).isEqualTo("FOREIGN_AID");

        gameEngine.resolveForeignAid(matchId, actorId, false);
        assertThat(state.getPlayers().get(0).getCoins()).isEqualTo(5);
        assertThat(state.getPendingAction()).isNull();

        // Assassination still works too (previous module).
        state.setCurrentTurnPlayerId(actorId);
        state.getPlayers().get(0).setCoins(3);
        stubAdvance();
        gameEngine.performAssassinate(matchId, actorId, otherId);
        gameEngine.resolveAssassinate(matchId, actorId, true);
        assertThat(state.getPlayers().get(0).getCoins()).isZero();
        assertThat(state.getCurrentTurnPlayerId()).isEqualTo(otherId);
        assertThat(state.getPlayers().get(1).getStatus()).isEqualTo(PlayerStatus.ELIMINATED);
    }
}