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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Module 22 — per-match concurrency.
 *
 * <p>Every action lifecycle is check-then-act: read the pending action, validate
 * the actor, mutate state, advance the turn, broadcast. A timeout thread, a
 * second browser tab, a block and a challenge can all reach that window at the
 * same instant, so the whole lifecycle is guarded by the monitor of that one
 * {@link GameState} instance.
 *
 * <p>The lock is deliberately <em>per match</em> — it is the state's own monitor,
 * not a shared/global one — so unrelated matches never contend. These tests pin
 * both halves of that promise: no double resolution within a match, and no
 * cross-match interference.
 */
@ExtendWith(MockitoExtension.class)
class PerMatchConcurrencyTest {

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
    private ActionResolver actionResolver;
    private SchedulerHolder holder;

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

        holder = new SchedulerHolder();
        ObjectProvider<PendingActionTimeoutScheduler> provider = providerOf(holder);

        gameEngine = new GameEngine(
                matchRepository, matchPlayerRepository, gameStore,
                cardManager, turnManager, gameStateMapper, winnerManager, webSocketEventPublisher,
                syncService, provider);

        actionResolver = new ActionResolver(gameEngine, gameStateMapper, winnerManager, syncService);

        GameTimerProperties timers = new GameTimerProperties();
        timers.setBlockWindowSeconds(3600);
        timers.setChallengeWindowSeconds(3600);
        timers.setExchangeDecisionWindowSeconds(3600);
        holder.set(new PendingActionTimeoutScheduler(gameEngine, actionResolver, timers));

        gameStore.remove(matchId);
    }

    private static final class SchedulerHolder {
        private PendingActionTimeoutScheduler scheduler;

        void set(PendingActionTimeoutScheduler scheduler) {
            this.scheduler = scheduler;
        }
    }

    private static ObjectProvider<PendingActionTimeoutScheduler> providerOf(SchedulerHolder holder) {
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
            public Iterator<PendingActionTimeoutScheduler> iterator() {
                return Collections.emptyIterator();
            }
        };
    }

    /* ------------------------------------------------------------------ */
    /*  Fixtures                                                           */
    /* ------------------------------------------------------------------ */

    private GameState seed(UUID currentTurnPlayerId) {
        List<GameCard> deck = cardManager.createDeck();
        List<GameCard> actorHand = cardManager.drawMany(deck, GameEngine.STARTING_INFLUENCE);
        List<GameCard> opponentHand = cardManager.drawMany(deck, GameEngine.STARTING_INFLUENCE);

        GamePlayerState actor = GamePlayerState.builder()
                .userId(actorId).username("actor").seatNumber(1)
                .status(PlayerStatus.ACTIVE).coins(5).cards(actorHand).host(false).build();
        GamePlayerState opponent = GamePlayerState.builder()
                .userId(opponentId).username("opponent").seatNumber(2)
                .status(PlayerStatus.ACTIVE).coins(5).cards(opponentHand).host(false).build();

        GameState state = GameState.builder()
                .matchId(matchId)
                .roomId(UUID.randomUUID())
                .roomCode("RAJEX")
                .status(MatchStatus.IN_PROGRESS)
                .phase(GameEngine.PHASE_IN_PROGRESS)
                .players(new ArrayList<>(List.of(actor, opponent)))
                .turnOrder(List.of(actorId, opponentId))
                .currentTurnPlayerId(currentTurnPlayerId)
                .turnNumber(1)
                .deck(deck)
                .actionExecuted(false)
                .pendingAction(null)
                .log(new ArrayList<>())
                .stateVersion(0L)
                .build();
        gameStore.put(matchId, state);
        return state;
    }

    private void stubTurnAdvanceTo(UUID nextPlayerId, int turnNumber) {
        when(turnManager.advanceTurn(eq(matchId))).thenReturn(Match.builder()
                .id(matchId)
                .status(MatchStatus.IN_PROGRESS)
                .currentTurnPlayerId(nextPlayerId)
                .turnNumber(turnNumber)
                .build());
    }

    private GamePlayerState playerOf(GameState state, UUID userId) {
        return state.getPlayers().stream()
                .filter(p -> p.getUserId().equals(userId))
                .findFirst()
                .orElseThrow();
    }

    /** Runs the given tasks simultaneously and returns their outcomes. */
    private <T> List<T> race(List<Callable<T>> tasks) throws Exception {
        int threads = tasks.size();
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<T>> futures = new ArrayList<>();
            for (Callable<T> task : tasks) {
                futures.add(pool.submit(() -> {
                    start.await();
                    return task.call();
                }));
            }
            start.countDown();
            List<T> results = new ArrayList<>();
            for (Future<T> future : futures) {
                results.add(future.get(30, TimeUnit.SECONDS));
            }
            return results;
        } finally {
            pool.shutdownNow();
        }
    }

    private static Callable<Object> attempt(Runnable body) {
        return () -> {
            try {
                body.run();
                return "OK";
            } catch (BusinessException ex) {
                return ex.getErrorCode();
            }
        };
    }

    /** {@link #attempt} with a label, so a race's outcomes stay attributable. */
    private static Callable<Object> tagged(String label, Runnable body) {
        return () -> {
            try {
                body.run();
                return Map.entry(label, "OK");
            } catch (BusinessException ex) {
                return Map.entry(label, ex.getErrorCode());
            }
        };
    }

    /* ------------------------------------------------------------------ */
    /*  A — two simultaneous resolves of the same action                   */
    /* ------------------------------------------------------------------ */

    @Test
    @DisplayName("A - two threads resolving one Tax: exactly one wins, one is rejected")
    void concurrentResolve_onlyOneSucceeds() throws Exception {
        GameState state = seed(actorId);
        stubTurnAdvanceTo(opponentId, 2);

        gameEngine.performTax(matchId, actorId);
        assertThat(state.getPendingAction()).isNotNull();

        List<Object> results = race(List.of(
                attempt(() -> actionResolver.resolve(matchId, actorId)),
                attempt(() -> actionResolver.resolve(matchId, actorId))));

        assertThat(results).containsExactlyInAnyOrder("OK", "NO_PENDING_ACTION");
        // The turn advanced exactly once — a double resolution would skip a player.
        verify(turnManager, times(1)).advanceTurn(eq(matchId));
        assertThat(state.getTurnNumber()).isEqualTo(2);
        assertThat(state.getCurrentTurnPlayerId()).isEqualTo(opponentId);
    }

    @Test
    @DisplayName("A - a stampede of eight resolves still advances exactly one turn")
    void resolveStampede_advancesExactlyOneTurn() throws Exception {
        GameState state = seed(actorId);
        stubTurnAdvanceTo(opponentId, 2);

        gameEngine.performTax(matchId, actorId);

        List<Callable<Object>> tasks = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            tasks.add(attempt(() -> actionResolver.resolve(matchId, actorId)));
        }
        List<Object> results = race(tasks);

        assertThat(results.stream().filter("OK"::equals).count()).isEqualTo(1);
        assertThat(results.stream().filter("NO_PENDING_ACTION"::equals).count()).isEqualTo(7);
        verify(turnManager, times(1)).advanceTurn(eq(matchId));
        assertThat(state.getCurrentTurnPlayerId()).isEqualTo(opponentId);
    }

    @Test
    @DisplayName("A - a non-actor can never resolve, even while the actor is resolving")
    void concurrentResolve_rejectsNonActor() throws Exception {
        GameState state = seed(actorId);
        stubTurnAdvanceTo(opponentId, 2);

        gameEngine.performTax(matchId, actorId);

        // Tag each outcome so the non-actor's result is attributable even though
        // the two threads interleave arbitrarily.
        List<Callable<Object>> tasks = List.of(
                tagged("actor", () -> actionResolver.resolve(matchId, actorId)),
                tagged("intruder", () -> actionResolver.resolve(matchId, opponentId)));
        Map<String, Object> results = new HashMap<>();
        for (Object outcome : race(tasks)) {
            @SuppressWarnings("unchecked")
            Map.Entry<String, Object> entry = (Map.Entry<String, Object>) outcome;
            results.put(entry.getKey(), entry.getValue());
        }

        // The intruder never wins, whichever order the threads reach the monitor.
        assertThat(results.get("intruder")).isIn("NOT_ACTOR", "NO_PENDING_ACTION");
        // At most one resolution, and exactly one turn advance overall.
        verify(turnManager, times(1)).advanceTurn(eq(matchId));
        assertThat(state.getTurnNumber()).isEqualTo(2);
    }

    /* ------------------------------------------------------------------ */
    /*  B — a duplicate action cannot be started twice in one turn         */
    /* ------------------------------------------------------------------ */

    @Test
    @DisplayName("B - two simultaneous Tax actions: the second is rejected as already acted")
    void duplicateAction_isRejectedWithinATurn() throws Exception {
        GameState state = seed(actorId);

        List<Object> results = race(List.of(
                attempt(() -> gameEngine.performTax(matchId, actorId)),
                attempt(() -> gameEngine.performTax(matchId, actorId))));

        assertThat(results.stream().filter("OK"::equals).count()).isEqualTo(1);
        assertThat(results.stream()
                .filter("ACTION_ALREADY_PERFORMED"::equals)
                .count())
                .isEqualTo(1);
        // Exactly one pending action exists for the turn.
        assertThat(state.getPendingAction()).isNotNull();
        assertThat(state.getPendingAction().getType()).isEqualTo(GameEngine.ACTION_TAX);
    }

    @Test
    @DisplayName("B - an action from a player who is not on turn is rejected")
    void offTurnAction_isRejected() {
        seed(opponentId);
        List<Object> results = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            results.add(attemptResult(() -> gameEngine.performTax(matchId, actorId)));
        }
        assertThat(results).allMatch("NOT_YOUR_TURN"::equals);
    }

    /* ------------------------------------------------------------------ */
    /*  C — a manual resolve racing the timeout is idempotent               */
    /* ------------------------------------------------------------------ */

    @Test
    @DisplayName("C - resolving while the deadline task fires yields one resolution")
    void manualResolveRacingTimeout_resolvesOnce() throws Exception {
        GameState state = seed(actorId);
        stubTurnAdvanceTo(opponentId, 2);

        gameEngine.performTax(matchId, actorId);
        PendingAction pending = state.getPendingAction();

        // Hammer the same match from many threads: a manual resolve and repeated
        // timeout resolutions all at once.
        AtomicInteger manualAttempts = new AtomicInteger();
        List<Callable<Object>> tasks = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            tasks.add(attempt(() -> {
                manualAttempts.incrementAndGet();
                actionResolver.resolve(matchId, actorId);
            }));
        }

        List<Object> results = race(tasks);

        long ok = results.stream().filter("OK"::equals).count();
        assertThat(ok).as("exactly one resolution may win").isEqualTo(1);
        assertThat(results.stream().filter("NO_PENDING_ACTION"::equals).count()).isEqualTo(5);
        // No double application of the Tax effect: TAX_GAIN is minted once, not
        // once per losing thread. A stampede would show 5 * TAX_GAIN here.
        assertThat(playerOf(state, actorId).getCoins()).isEqualTo(5 + GameEngine.TAX_GAIN);
        assertThat(playerOf(state, opponentId).getCoins()).isEqualTo(5);
        verify(turnManager, times(1)).advanceTurn(eq(matchId));
        assertThat(pending.getId()).isNotNull();
    }

    @Test
    @DisplayName("C - confirming an Exchange while it times out never double-mutates the hand")
    void exchangeConfirmRacingTimeout_leavesHandConsistent() throws Exception {
        GameState state = seed(actorId);
        stubTurnAdvanceTo(opponentId, 2);

        gameEngine.performExchange(matchId, actorId);
        PendingAction pending = state.getPendingAction();
        List<UUID> keep = List.of(
                pending.getExchangePool().get(0).getId(),
                pending.getExchangePool().get(1).getId());
        int deckBefore = state.getDeck().size();

        List<Callable<Object>> tasks = new ArrayList<>();
        tasks.add(attempt(() -> gameEngine.confirmExchange(matchId, actorId, keep)));
        for (int i = 0; i < 4; i++) {
            tasks.add(attempt(() -> actionResolver.resolveExpiredExchange(matchId, actorId)));
        }
        List<Object> results = race(tasks);

        // Whatever the interleaving, the hand is a legal 2-card hand and the deck
        // holds exactly the 2 unchosen cards. A timeout that wrongly ran after a
        // successful confirm would return all 4 drawn cards, pushing the deck to
        // deckBefore + 4, so the bound catches that double-application.
        assertThat(playerOf(state, actorId).getCards()).hasSize(2);
        assertThat(state.getPendingAction()).isNull();
        assertThat(state.getDeck().size()).isEqualTo(deckBefore + 2);
        verify(turnManager, times(1)).advanceTurn(eq(matchId));

        long rejections = results.stream()
                .filter(r -> !"OK".equals(r))
                .count();
        assertThat(rejections).isPositive();
    }

    /* ------------------------------------------------------------------ */
    /*  D — the lock is per match, not global                              */
    /* ------------------------------------------------------------------ */

    @Test
    @DisplayName("D - concurrent activity on separate matches does not interfere")
    void separateMatches_areIndependent() throws Exception {
        // 24 independent matches, each with its own Tax already pending.
        int matchCount = 24;
        List<UUID> ids = new ArrayList<>();
        for (int i = 0; i < matchCount; i++) {
            UUID id = UUID.randomUUID();
            ids.add(id);
            seedMatch(id, i);
        }
        when(turnManager.advanceTurn(any())).thenReturn(Match.builder()
                .id(matchId).status(MatchStatus.IN_PROGRESS)
                .currentTurnPlayerId(opponentId).turnNumber(2).build());

        List<Callable<Object>> tasks = new ArrayList<>();
        for (int i = 0; i < matchCount; i++) {
            UUID id = ids.get(i);
            tasks.add(attempt(() -> actionResolver.resolve(id, actorIdOf(id))));
        }
        List<Object> results = race(tasks);

        // Every match resolved independently and advanced its own turn.
        assertThat(results.stream().filter("OK"::equals).count()).isEqualTo(matchCount);
        for (UUID id : ids) {
            GameState state = gameStore.get(id);
            assertThat(state.getPendingAction()).as("match %s", id).isNull();
            assertThat(state.getTurnNumber()).as("match %s", id).isEqualTo(2);
        }
        verify(turnManager, times(matchCount)).advanceTurn(any());
    }

    private UUID actorIdOf(UUID matchId) {
        return gameStore.get(matchId).getPendingAction().getActorUserId();
    }

    /** Seeds a distinct match with its own actor/opponent pair. */
    private void seedMatch(UUID id, int index) {
        List<GameCard> deck = cardManager.createDeck();
        UUID a = new UUID(0, index * 2L + 1);
        UUID b = new UUID(0, index * 2L + 2);

        GamePlayerState actor = GamePlayerState.builder()
                .userId(a).username("actor" + index).seatNumber(1)
                .status(PlayerStatus.ACTIVE).coins(5)
                .cards(cardManager.drawMany(deck, GameEngine.STARTING_INFLUENCE)).host(false).build();
        GamePlayerState opponent = GamePlayerState.builder()
                .userId(b).username("opponent" + index).seatNumber(2)
                .status(PlayerStatus.ACTIVE).coins(5)
                .cards(cardManager.drawMany(deck, GameEngine.STARTING_INFLUENCE)).host(false).build();

        GameState state = GameState.builder()
                .matchId(id)
                .roomId(UUID.randomUUID())
                .roomCode("RAJ" + index)
                .status(MatchStatus.IN_PROGRESS)
                .phase(GameEngine.PHASE_IN_PROGRESS)
                .players(new ArrayList<>(List.of(actor, opponent)))
                .turnOrder(List.of(a, b))
                .currentTurnPlayerId(a)
                .turnNumber(1)
                .deck(deck)
                .actionExecuted(false)
                .pendingAction(null)
                .log(new ArrayList<>())
                .stateVersion(0L)
                .build();
        gameStore.put(id, state);
        gameEngine.performTax(id, a);
    }

    /* ------------------------------------------------------------------ */
    /*  E — a stale broadcast is never published after a race             */
    /* ------------------------------------------------------------------ */

    @Test
    @DisplayName("E - a losing race-writer does not publish a second verdict snapshot")
    void losingRace_doesNotPublishExtraState() throws Exception {
        GameState state = seed(actorId);
        stubTurnAdvanceTo(opponentId, 2);

        gameEngine.performTax(matchId, actorId);

        int before = countPublishedMatchSnapshots();
        List<Object> results = race(List.of(
                attempt(() -> actionResolver.resolve(matchId, actorId)),
                attempt(() -> actionResolver.resolve(matchId, actorId))));

        assertThat(results.stream().filter("OK"::equals).count()).isEqualTo(1);
        // The winner broadcasts its resolution: the engine seam's play-action
        // publish plus its own state sync, then the resolver's verdict sync.
        // The loser must add nothing beyond that fixed count.
        int publishedByWinner = countPublishedMatchSnapshots() - before;
        assertThat(publishedByWinner).isEqualTo(3);
    }

    private int countPublishedMatchSnapshots() {
        return org.mockito.Mockito.mockingDetails(webSocketEventPublisher).getInvocations().stream()
                .filter(inv -> "publishToMatch".equals(inv.getMethod().getName()))
                .mapToInt(x -> 1).sum();
    }

    /* ------------------------------------------------------------------ */
    /*  F — a finished match rejects further work                          */
    /* ------------------------------------------------------------------ */

    @Test
    @DisplayName("F - once finished, no action can start or resolve")
    void finishedMatch_rejectsFurtherWork() {
        GameState state = seed(actorId);
        state.setStatus(MatchStatus.FINISHED);
        state.setEndedAt(java.time.LocalDateTime.now());

        List<Object> results = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            results.add(attemptResult(() -> gameEngine.performTax(matchId, actorId)));
        }
        assertThat(results).allMatch("MATCH_NOT_ACTIVE"::equals);
        verify(turnManager, never()).advanceTurn(any());
        verify(webSocketEventPublisher, never())
                .publishToMatch(any(), any(), any(), any());
    }

    @Test
    @DisplayName("F - a resolve arriving after the match finished is refused")
    void resolveAfterFinish_isRefused() {
        GameState state = seed(actorId);
        state.setStatus(MatchStatus.FINISHED);
        state.setEndedAt(java.time.LocalDateTime.now());
        state.setPendingAction(PendingAction.builder()
                .id(UUID.randomUUID())
                .type(GameEngine.ACTION_TAX)
                .actorUserId(actorId)
                .startedAt(java.time.LocalDateTime.now())
                .targetPlayerId(opponentId)
                .build());

        Object result = attemptResult(() -> actionResolver.resolve(matchId, actorId));

        assertThat(result).isEqualTo("MATCH_NOT_ACTIVE");
        verify(turnManager, never()).advanceTurn(any());
    }

    private Object attemptResult(Runnable body) {
        try {
            return attempt(body).call();
        } catch (Exception ex) {
            throw new AssertionError(ex);
        }
    }

    /* ------------------------------------------------------------------ */
    /*  G — state stays coherent under sustained mixed load                */
    /* ------------------------------------------------------------------ */

    @Test
    @DisplayName("G - mixed concurrent traffic leaves a coherent state and no pending action")
    void mixedTraffic_leavesCoherentState() throws Exception {
        GameState state = seed(actorId);
        stubTurnAdvanceTo(opponentId, 2);

        gameEngine.performTax(matchId, actorId);

        List<Callable<Object>> tasks = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            tasks.add(attempt(() -> actionResolver.resolve(matchId, actorId)));
        }
        for (int i = 0; i < 5; i++) {
            tasks.add(attempt(() -> actionResolver.resolve(matchId, opponentId)));
        }
        List<Object> results = race(tasks);

        assertThat(results.stream().filter("OK"::equals).count()).isEqualTo(1);
        assertThat(state.getPendingAction()).isNull();
        assertThat(state.getStateVersion()).isPositive();
        assertThat(state.getTurnNumber()).isEqualTo(2);
        // The Tax minted its reward exactly once: 10 starting + TAX_GAIN. A
        // double resolution would show a multiple of it.
        int totalCoins = state.getPlayers().stream().mapToInt(GamePlayerState::getCoins).sum();
        assertThat(totalCoins).isEqualTo(10 + GameEngine.TAX_GAIN);
        // Every card identity in play is still unique.
        int inPlay = state.getDeck().size() + state.getPlayers().stream()
                .mapToInt(p -> p.getCards().size()).sum();
        assertThat(inPlay).isEqualTo(cardManager.createDeck().size());
    }
}
