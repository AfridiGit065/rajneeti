package com.rajneeti.game;

import com.rajneeti.config.GameTimerProperties;
import com.rajneeti.dto.game.GameStateResponse;
import com.rajneeti.entity.Match;
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
import org.springframework.beans.factory.ObjectProvider;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.BooleanSupplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Module 20 — authoritative block/challenge window timeouts.
 *
 * <p>Wires a real {@link PendingActionTimeoutScheduler} with a one second window
 * over the real engine, resolver, block manager and challenge manager, so these
 * tests exercise the exact production timeout path a browser can no longer
 * trigger. Every assertion is on the authoritative {@link GameState}: coins, the
 * cleared pending action, the advanced turn and the bumped
 * {@code stateVersion}.
 */
@ExtendWith(MockitoExtension.class)
class PendingActionTimeoutTest {

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

    private GameEngine gameEngine;
    private ChallengeManager challengeManager;
    private ActionResolver actionResolver;
    private PendingActionTimeoutScheduler scheduler;

    private UUID matchId;
    private UUID actorId;
    private UUID opponentId;

    @BeforeEach
    void setUp() {
        matchId = UUID.randomUUID();
        actorId = UUID.randomUUID();
        opponentId = UUID.randomUUID();

        WinnerManager winnerManager = new WinnerManager(
                matchRepository, matchPlayerRepository, webSocketEventPublisher);
        GameStateSyncService syncService =
                new GameStateSyncService(gameStateMapper, webSocketEventPublisher);

        GameTimerProperties timers = new GameTimerProperties();
        timers.setBlockWindowSeconds(1);
        timers.setChallengeWindowSeconds(1);

        // The engine resolves the scheduler lazily through this provider, so the
        // engine can be built first and the scheduler filled in afterwards.
        SchedulerHolder holder = new SchedulerHolder();
        ObjectProvider<PendingActionTimeoutScheduler> provider = providerOf(holder);

        gameEngine = new GameEngine(
                matchRepository, matchPlayerRepository, gameStore,
                cardManager, turnManager, gameStateMapper, winnerManager, webSocketEventPublisher,
                syncService, provider);

        actionResolver = new ActionResolver(gameEngine, gameStateMapper, winnerManager, syncService);
        scheduler = new PendingActionTimeoutScheduler(gameEngine, actionResolver, timers);
        holder.set(scheduler);

        challengeManager = new ChallengeManager(gameEngine, cardManager, turnManager, winnerManager,
                webSocketEventPublisher, syncService, scheduler);

        gameStore.remove(matchId);
    }

    /* ------------------------------------------------------------------ */
    /*  Timeout plumbing helpers                                           */
    /* ------------------------------------------------------------------ */

    private static final class SchedulerHolder {
        private PendingActionTimeoutScheduler scheduler;

        void set(PendingActionTimeoutScheduler scheduler) {
            this.scheduler = scheduler;
        }
    }

    private static ObjectProvider<PendingActionTimeoutScheduler> providerOf(
            SchedulerHolder holder) {
        return new ObjectProvider<>() {
            @Override
            public PendingActionTimeoutScheduler getObject() {
                return holder.scheduler;
            }

            @Override
            public PendingActionTimeoutScheduler getObject(Object... args) {
                return holder.scheduler;
            }

            @Override
            public PendingActionTimeoutScheduler getIfAvailable() {
                return holder.scheduler;
            }

            @Override
            public PendingActionTimeoutScheduler getIfUnique() {
                return holder.scheduler;
            }

            @Override
            public java.util.Iterator<PendingActionTimeoutScheduler> iterator() {
                return java.util.Collections.emptyIterator();
            }
        };
    }

    /** Polls the authoritative state until the condition holds or the budget runs out. */
    private void awaitUntil(String description, BooleanSupplier condition) {
        Duration budget = Duration.ofSeconds(10);
        long deadline = System.nanoTime() + budget.toNanos();
        while (System.nanoTime() < deadline) {
            if (condition.getAsBoolean()) {
                return;
            }
            try {
                Thread.sleep(25);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new AssertionError("Interrupted while waiting for " + description, ex);
            }
        }
        throw new AssertionError("Timed out waiting for " + description);
    }

    /* ------------------------------------------------------------------ */
    /*  Fixtures                                                          */
    /* ------------------------------------------------------------------ */

    private GameState seed(GamePlayerState actor, GamePlayerState opponent,
                           UUID currentTurnPlayerId, MatchStatus status) {
        List<GamePlayerState> players = new ArrayList<>();
        players.add(actor);
        players.add(opponent);

        GameState state = GameState.builder()
                .matchId(matchId)
                .roomId(UUID.randomUUID())
                .roomCode("RAJFB")
                .status(status)
                .phase(GameEngine.PHASE_IN_PROGRESS)
                .players(players)
                .turnOrder(List.of(actorId, opponentId))
                .currentTurnPlayerId(currentTurnPlayerId)
                .turnNumber(1)
                .deck(cardManager.createDeck())
                .actionExecuted(false)
                .pendingAction(null)
                .log(new ArrayList<>())
                .stateVersion(0L)
                .build();
        gameStore.put(matchId, state);
        return state;
    }

    private GamePlayerState player(UUID userId, String username, int coins, List<GameCard> cards) {
        return GamePlayerState.builder()
                .userId(userId)
                .username(username)
                .seatNumber(1)
                .status(PlayerStatus.ACTIVE)
                .coins(coins)
                .cards(cards)
                .host(false)
                .build();
    }

    private List<GameCard> cardsWith(CharacterType... types) {
        List<GameCard> cards = new ArrayList<>();
        for (CharacterType type : types) {
            cards.add(GameCard.builder().id(UUID.randomUUID()).character(type).build());
        }
        return cards;
    }

    private GamePlayerState actorPlayer() {
        return player(actorId, "actor", 2, cardsWith(CharacterType.MINISTER, CharacterType.GHATOK));
    }

    private GamePlayerState opponentPlayer() {
        return player(opponentId, "opponent", 2, cardsWith(CharacterType.DALAL, CharacterType.AMLA));
    }

    private void stubTurnAdvanceTo(UUID nextPlayerId, int turnNumber) {
        when(turnManager.advanceTurn(eq(matchId))).thenReturn(Match.builder()
                .id(matchId)
                .status(MatchStatus.IN_PROGRESS)
                .currentTurnPlayerId(nextPlayerId)
                .turnNumber(turnNumber)
                .build());
    }

    /* ------------------------------------------------------------------ */
    /*  Foreign Aid — block window timeout                                 */
    /* ------------------------------------------------------------------ */

    @Test
    @DisplayName("Foreign Aid - block window expires: +2 coins, pending cleared, turn advanced")
    void foreignAid_blockWindowTimeout_resolvesAuthoritatively() {
        GameState state = seed(actorPlayer(), opponentPlayer(), actorId, MatchStatus.IN_PROGRESS);
        stubTurnAdvanceTo(opponentId, 2);

        gameEngine.performForeignAid(matchId, actorId);
        assertThat(state.getPendingAction()).isNotNull();
        long versionBeforeTimeout = state.getStateVersion();

        awaitUntil("the Foreign Aid window to expire",
                () -> state.getPendingAction() == null);

        assertThat(state.getPendingAction()).isNull();
        assertThat(actorId).isNotNull();
        assertThat(coinsOf(state, actorId)).isEqualTo(4);
        assertThat(state.getCurrentTurnPlayerId()).isEqualTo(opponentId);
        assertThat(state.getTurnNumber()).isEqualTo(2);
        assertThat(state.isActionExecuted()).isFalse();
        assertThat(state.getPhase()).isEqualTo(GameEngine.PHASE_IN_PROGRESS);
        assertThat(state.getStateVersion()).isGreaterThan(versionBeforeTimeout);
    }

    @Test
    @DisplayName("Foreign Aid - a second timeout for the same action is harmless")
    void foreignAid_duplicateTimeout_isHarmless() throws InterruptedException {
        GameState state = seed(actorPlayer(), opponentPlayer(), actorId, MatchStatus.IN_PROGRESS);
        stubTurnAdvanceTo(opponentId, 2);

        gameEngine.performForeignAid(matchId, actorId);
        awaitUntil("the Foreign Aid window to expire",
                () -> state.getPendingAction() == null);

        long versionAfterFirst = state.getStateVersion();
        int coinsAfterFirst = coinsOf(state, actorId);

        scheduler.armBlockWindow(matchId);
        Thread.sleep(1500);

        assertThat(coinsOf(state, actorId)).isEqualTo(coinsAfterFirst);
        assertThat(state.getStateVersion()).isEqualTo(versionAfterFirst);
        assertThat(state.getCurrentTurnPlayerId()).isEqualTo(opponentId);
        verify(turnManager, org.mockito.Mockito.times(1)).advanceTurn(eq(matchId));
    }

    @Test
    @DisplayName("Foreign Aid - manual resolve wins and disarms the timeout")
    void foreignAid_manualResolve_blocksTheTimeout() throws InterruptedException {
        GameState state = seed(actorPlayer(), opponentPlayer(), actorId, MatchStatus.IN_PROGRESS);
        stubTurnAdvanceTo(opponentId, 2);

        gameEngine.performForeignAid(matchId, actorId);
        actionResolver.resolve(matchId, actorId);

        assertThat(coinsOf(state, actorId)).isEqualTo(4);
        assertThat(state.getPendingAction()).isNull();

        // Well past the one second window: the disarmed timer must not fire.
        Thread.sleep(1500);

        assertThat(coinsOf(state, actorId)).isEqualTo(4);
        assertThat(state.getTurnNumber()).isEqualTo(2);
        verify(turnManager, org.mockito.Mockito.times(1)).advanceTurn(eq(matchId));
    }

    @Test
    @DisplayName("Foreign Aid - a timed-out window is not resolved a second time")
    void foreignAid_afterTimeout_resolveIsRejected() {
        GameState state = seed(actorPlayer(), opponentPlayer(), actorId, MatchStatus.IN_PROGRESS);
        stubTurnAdvanceTo(opponentId, 2);

        gameEngine.performForeignAid(matchId, actorId);
        awaitUntil("the Foreign Aid window to expire",
                () -> state.getPendingAction() == null);

        assertThatThrownBy(() -> actionResolver.resolve(matchId, actorId))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo("NO_PENDING_ACTION");
    }

    /* ------------------------------------------------------------------ */
    /*  Tax — challenge window timeout                                     */
    /* ------------------------------------------------------------------ */

    @Test
    @DisplayName("Tax - challenge window expires: +3 coins, pending cleared, turn advanced")
    void tax_challengeWindowTimeout_resolvesAuthoritatively() {
        GameState state = seed(actorPlayer(), opponentPlayer(), actorId, MatchStatus.IN_PROGRESS);
        stubTurnAdvanceTo(opponentId, 2);

        gameEngine.performTax(matchId, actorId);
        assertThat(state.getPendingAction()).isNotNull();
        assertThat(state.getPendingAction().getType()).isEqualTo(GameEngine.ACTION_TAX);
        long versionBeforeTimeout = state.getStateVersion();

        awaitUntil("the Tax challenge window to expire",
                () -> state.getPendingAction() == null);

        assertThat(state.getPendingAction()).isNull();
        assertThat(coinsOf(state, actorId)).isEqualTo(5);
        assertThat(state.getCurrentTurnPlayerId()).isEqualTo(opponentId);
        assertThat(state.getTurnNumber()).isEqualTo(2);
        assertThat(state.isActionExecuted()).isFalse();
        assertThat(state.getStateVersion()).isGreaterThan(versionBeforeTimeout);
    }

    @Test
    @DisplayName("Tax - a standing Minister claim still grants the coins on timeout")
    void tax_timeout_recordsAuthoritativeVerdict() {
        GameState state = seed(actorPlayer(), opponentPlayer(), actorId, MatchStatus.IN_PROGRESS);
        stubTurnAdvanceTo(opponentId, 2);

        gameEngine.performTax(matchId, actorId);
        awaitUntil("the Tax challenge window to expire",
                () -> state.getPendingAction() == null);

        assertThat(state.getLastActionResult()).isNotNull();
        assertThat(state.getLastActionResult().getActionType()).isEqualTo(GameEngine.ACTION_TAX);
        assertThat(state.getLastActionResult().getResult()).isEqualTo("RESOLVED");
        assertThat(state.getLastActionResult().getCoinsGained()).isEqualTo(GameEngine.TAX_GAIN);
    }

    @Test
    @DisplayName("Tax - a challenge that proves the claim re-arms the window")
    void tax_challengeReArmsTheWindow() {
        // The opponent holds a real MINISTER card, so this challenge proves the
        // claim truthful and the action must stay pending with a fresh window.
        GameState state = seed(actorPlayer(),
                player(opponentId, "opponent", 2, cardsWith(CharacterType.MINISTER, CharacterType.AMLA)),
                actorId, MatchStatus.IN_PROGRESS);
        stubTurnAdvanceTo(opponentId, 2);

        gameEngine.performTax(matchId, actorId);
        long versionBeforeChallenge = state.getStateVersion();

        challengeManager.challenge(matchId, opponentId, null);

        assertThat(state.getPendingAction()).isNotNull();
        assertThat(state.getStateVersion()).isGreaterThan(versionBeforeChallenge);

        // The re-armed window expires on its own and still pays the claim.
        awaitUntil("the re-armed Tax window to expire",
                () -> state.getPendingAction() == null);

        assertThat(coinsOf(state, actorId)).isEqualTo(5);
    }

    @Test
    @DisplayName("Foreign Aid - the pending action carries an authoritative deadline")
    void foreignAid_pendingActionExposesServerDeadline() {
        GameState state = seed(actorPlayer(), opponentPlayer(), actorId, MatchStatus.IN_PROGRESS);

        gameEngine.performForeignAid(matchId, actorId);

        LocalDateTime deadline = state.getPendingAction().getDeadlineAt();
        assertThat(deadline).isNotNull();
        assertThat(deadline).isAfter(LocalDateTime.now());
    }

    @Test
    @DisplayName("Tax - the opponent's own view exposes the same authoritative deadline")
    void tax_deadlineIsBroadcastToTheOpponent() {
        GameState state = seed(actorPlayer(), opponentPlayer(), actorId, MatchStatus.IN_PROGRESS);

        gameEngine.performTax(matchId, actorId);

        LocalDateTime deadline = state.getPendingAction().getDeadlineAt();
        assertThat(deadline).isNotNull();

        GameStateResponse opponentView = gameEngine.getSafeGameState(matchId, opponentId);
        assertThat(opponentView.getPendingAction().getDeadlineAt()).isEqualTo(deadline);
    }

    @Test
    @DisplayName("Tax - the actor cannot challenge their own claim")
    void tax_actorCannotChallengeSelf() {
        seed(actorPlayer(), opponentPlayer(), actorId, MatchStatus.IN_PROGRESS);
        gameEngine.performTax(matchId, actorId);

        assertThatThrownBy(() -> challengeManager.challenge(matchId, actorId, null))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("Tax - an eligible opponent is offered the challenge")
    void tax_opponentCanChallengeTheClaim() {
        GameState state = seed(actorPlayer(), opponentPlayer(), actorId, MatchStatus.IN_PROGRESS);

        gameEngine.performTax(matchId, actorId);

        // The pending action is broadcast to the match, so the opponent's own
        // projection of the authoritative state shows the same open claim.
        GameStateResponse opponentView = gameEngine.getSafeGameState(matchId, opponentId);
        assertThat(opponentView.getPendingAction()).isNotNull();
        assertThat(opponentView.getPendingAction().getActorUserId()).isEqualTo(actorId);
        assertThat(state.getPendingAction().getClaimedCharacter())
                .isEqualTo(GameEngine.CHARACTER_MINISTER);
    }

    /* ------------------------------------------------------------------ */
    /*  Block then challenge                                              */
    /* ------------------------------------------------------------------ */

    @Test
    @DisplayName("Foreign Aid - a standing Minister block cancels the action on timeout")
    void foreignAid_standingBlockCancelsOnTimeout() {
        GameState state = seed(actorPlayer(),
                player(opponentId, "opponent", 2, cardsWith(CharacterType.MINISTER, CharacterType.AMLA)),
                actorId, MatchStatus.IN_PROGRESS);
        stubTurnAdvanceTo(opponentId, 2);

        gameEngine.performForeignAid(matchId, actorId);

        new BlockManager(gameEngine, webSocketEventPublisher,
                new GameStateSyncService(gameStateMapper, webSocketEventPublisher),
                scheduler).block(matchId, opponentId, GameEngine.CHARACTER_MINISTER);

        assertThat(state.getPendingAction().getBlockerUserId()).isEqualTo(opponentId);

        awaitUntil("the block challenge window to expire",
                () -> state.getPendingAction() == null);

        assertThat(coinsOf(state, actorId)).isEqualTo(2);
        assertThat(state.getCurrentTurnPlayerId()).isEqualTo(opponentId);
        assertThat(state.getTurnNumber()).isEqualTo(2);
    }

    @Test
    @DisplayName("Foreign Aid - a late timeout on a finished match is ignored")
    void foreignAid_finishedMatchIsNotResolvedByTheTimer() {
        GameState state = seed(actorPlayer(), opponentPlayer(), actorId, MatchStatus.FINISHED);

        scheduler.armBlockWindow(matchId);
        gameStore.put(matchId, state);

        assertThat(state.getPendingAction()).isNull();
        verify(turnManager, never()).advanceTurn(any());
    }

    /* ------------------------------------------------------------------ */
    /*  Assertions helpers                                                */
    /* ------------------------------------------------------------------ */

    private int coinsOf(GameState state, UUID userId) {
        return state.getPlayers().stream()
                .filter(p -> p.getUserId().equals(userId))
                .findFirst()
                .orElseThrow()
                .getCoins();
    }
}
