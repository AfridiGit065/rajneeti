package com.rajneeti.game;

import com.rajneeti.config.GameTimerProperties;
import com.rajneeti.dto.game.GameStateResponse;
import com.rajneeti.dto.websocket.WebSocketEventType;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.BooleanSupplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Module 20 — the Exchange card-choice deadline.
 *
 * <p>An Exchange is the only window whose outcome the server cannot decide on its
 * own: the actor has to choose which 2 of the 4 pooled cards to keep. These tests
 * pin the safe, deterministic behaviour that replaced the old "no timer at all"
 * path: the actor gets a finite authoritative decision window, and if it lapses the
 * Exchange is <em>cancelled</em> — drawn cards go back to the deck, the hand is
 * restored, the turn advances. No card is ever chosen on the actor's behalf.
 */
@ExtendWith(MockitoExtension.class)
class ExchangeTimeoutTest {

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

    /** Seconds used by every test in this class; overridden per test where needed. */
    private int exchangeWindowSeconds = 1;

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
        rebuildScheduler();

        gameStore.remove(matchId);
    }

    /** Rebuilds the scheduler with the currently configured Exchange window. */
    private void rebuildScheduler() {
        GameTimerProperties timers = new GameTimerProperties();
        timers.setBlockWindowSeconds(3600);
        timers.setChallengeWindowSeconds(3600);
        timers.setExchangeDecisionWindowSeconds(exchangeWindowSeconds);
        holder.set(new PendingActionTimeoutScheduler(gameEngine, actionResolver, timers));
    }

    /* ------------------------------------------------------------------ */
    /*  Plumbing                                                           */
    /* ------------------------------------------------------------------ */

    private static final class SchedulerHolder {
        private PendingActionTimeoutScheduler scheduler;

        void set(PendingActionTimeoutScheduler scheduler) {
            this.scheduler = scheduler;
        }

        PendingActionTimeoutScheduler get() {
            return scheduler;
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
            public java.util.Iterator<PendingActionTimeoutScheduler> iterator() {
                return java.util.Collections.emptyIterator();
            }
        };
    }

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
    /*  Fixtures                                                           */
    /* ------------------------------------------------------------------ */

    private GameState seed(UUID currentTurnPlayerId) {
        // The deck is dealt from, so every hand card must come out of it —
        // otherwise the deck-integrity assertion is violated by the fixture
        // itself rather than by the code under test.
        List<GameCard> deck = cardManager.createDeck();
        List<GameCard> actorHand = cardManager.drawMany(deck, GameEngine.STARTING_INFLUENCE);
        List<GameCard> opponentHand = cardManager.drawMany(deck, GameEngine.STARTING_INFLUENCE);

        List<GamePlayerState> players = new ArrayList<>();
        players.add(player(actorId, "actor", 5, actorHand));
        players.add(player(opponentId, "opponent", 5, opponentHand));

        GameState state = GameState.builder()
                .matchId(matchId)
                .roomId(UUID.randomUUID())
                .roomCode("RAJEX")
                .status(MatchStatus.IN_PROGRESS)
                .phase(GameEngine.PHASE_IN_PROGRESS)
                .players(players)
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

    private int coinsOf(GameState state, UUID userId) {
        return playerOf(state, userId).getCoins();
    }

    private int deckSize(GameState state) {
        return state.getDeck().size();
    }

    /* ------------------------------------------------------------------ */
    /*  A — confirmed before the deadline                                  */
    /* ------------------------------------------------------------------ */

    @Test
    @DisplayName("A - an Exchange confirmed before the deadline resolves normally")
    void exchange_confirmedBeforeTimeout_resolvesNormally() {
        exchangeWindowSeconds = 30;
        rebuildScheduler();

        GameState state = seed(actorId);
        stubTurnAdvanceTo(opponentId, 2);

        gameEngine.performExchange(matchId, actorId);
        PendingAction pending = state.getPendingAction();
        assertThat(pending).isNotNull();
        assertThat(pending.getType()).isEqualTo(GameEngine.ACTION_EXCHANGE);

        // The card choice carries the private pool to the actor only.
        assertThat(pending.getExchangePool()).hasSize(4);
        assertThat(pending.getDeadlineAt()).isNotNull();

        List<UUID> keep = List.of(
                pending.getExchangePool().get(0).getId(),
                pending.getExchangePool().get(1).getId());
        gameEngine.confirmExchange(matchId, actorId, keep);

        assertThat(state.getPendingAction()).isNull();
        assertThat(playerOf(state, actorId).getCards()).hasSize(2);
        assertThat(state.getCurrentTurnPlayerId()).isEqualTo(opponentId);
        assertThat(state.getTurnNumber()).isEqualTo(2);
        // Coins never move for an Exchange.
        assertThat(coinsOf(state, actorId)).isEqualTo(5);
        // A confirmed Exchange resolves, it is not cancelled.
        assertThat(state.getLastActionResult()).isNull();
    }

    /* ------------------------------------------------------------------ */
    /*  B — the deadline lapses                                            */
    /* ------------------------------------------------------------------ */

    @Test
    @DisplayName("B - an unconfirmed Exchange is cancelled when the deadline lapses")
    void exchange_timeout_cancelsTheAction() {
        GameState state = seed(actorId);
        stubTurnAdvanceTo(opponentId, 2);

        List<UUID> originalHand = playerOf(state, actorId).getCards().stream()
                .map(GameCard::getId).toList();
        int deckBefore = deckSize(state);

        gameEngine.performExchange(matchId, actorId);
        assertThat(playerOf(state, actorId).getCards()).hasSize(4);
        assertThat(deckSize(state)).isEqualTo(deckBefore - 2);

        awaitUntil("the Exchange deadline to lapse",
                () -> state.getPendingAction() == null);

        // The drawn cards went back to the deck; the hand is exactly as it was.
        assertThat(playerOf(state, actorId).getCards().stream().map(GameCard::getId).toList())
                .containsExactlyInAnyOrderElementsOf(originalHand);
        assertThat(deckSize(state)).isEqualTo(deckBefore);
        // No coins moved and no influence card was lost.
        assertThat(coinsOf(state, actorId)).isEqualTo(5);
        assertThat(state.getRevealedCardsCount()).isZero();
    }

    @Test
    @DisplayName("B - a cancelled Exchange is recorded as CANCELLED with no coin delta")
    void exchange_timeout_recordsCancelledVerdict() {
        GameState state = seed(actorId);
        stubTurnAdvanceTo(opponentId, 2);

        gameEngine.performExchange(matchId, actorId);
        awaitUntil("the Exchange deadline to lapse",
                () -> state.getPendingAction() == null);

        assertThat(state.getLastActionResult()).isNotNull();
        assertThat(state.getLastActionResult().getResult())
                .isEqualTo(GameActionResult.RESULT_CANCELLED);
        assertThat(state.getLastActionResult().getActionType())
                .isEqualTo(GameEngine.ACTION_EXCHANGE);
        assertThat(state.getLastActionResult().getCoinsGained()).isZero();
        assertThat(state.getLastActionResult().getCoinsLost()).isZero();
    }

    /* ------------------------------------------------------------------ */
    /*  C — no duplicate resolution                                        */
    /* ------------------------------------------------------------------ */

    @Test
    @DisplayName("C - a lapsed Exchange is not resolved a second time")
    void exchange_duplicateTimeout_doesNotResolveTwice() throws InterruptedException {
        GameState state = seed(actorId);
        stubTurnAdvanceTo(opponentId, 2);

        gameEngine.performExchange(matchId, actorId);
        awaitUntil("the Exchange deadline to lapse",
                () -> state.getPendingAction() == null);

        long versionAfterFirst = state.getStateVersion();
        int deckAfterFirst = deckSize(state);
        int handAfterFirst = playerOf(state, actorId).getCards().size();

        // Re-arm a window on the now-closed action: it must find nothing to do.
        holder.get().armExchangeDecisionWindow(matchId);
        Thread.sleep(1500);

        assertThat(state.getStateVersion()).isEqualTo(versionAfterFirst);
        assertThat(deckSize(state)).isEqualTo(deckAfterFirst);
        assertThat(playerOf(state, actorId).getCards()).hasSize(handAfterFirst);
        verify(turnManager, times(1)).advanceTurn(eq(matchId));

        // A late confirm is rejected outright rather than resolving a dead action.
        assertThatThrownBy(() -> gameEngine.confirmExchange(matchId, actorId,
                List.of(UUID.randomUUID(), UUID.randomUUID())))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo("NO_PENDING_ACTION");
    }

    /* ------------------------------------------------------------------ */
    /*  D — confirm racing the timeout is idempotent                        */
    /* ------------------------------------------------------------------ */

    @Test
    @DisplayName("D - confirming wins the race and disarms the timer")
    void exchange_confirmRacingTimeout_isIdempotent() throws InterruptedException {
        GameState state = seed(actorId);
        stubTurnAdvanceTo(opponentId, 2);

        gameEngine.performExchange(matchId, actorId);
        PendingAction pending = state.getPendingAction();
        List<UUID> keep = List.of(
                pending.getExchangePool().get(0).getId(),
                pending.getExchangePool().get(1).getId());
        gameEngine.confirmExchange(matchId, actorId, keep);

        assertThat(state.getPendingAction()).isNull();
        int handAfterConfirm = playerOf(state, actorId).getCards().size();
        long versionAfterConfirm = state.getStateVersion();

        // Well past the one second window: the disarmed timer must not fire.
        Thread.sleep(1500);

        assertThat(state.getPendingAction()).isNull();
        assertThat(playerOf(state, actorId).getCards()).hasSize(handAfterConfirm);
        assertThat(state.getStateVersion()).isEqualTo(versionAfterConfirm);
        assertThat(coinsOf(state, actorId)).isEqualTo(5);
        verify(turnManager, times(1)).advanceTurn(eq(matchId));
    }

    /* ------------------------------------------------------------------ */
    /*  E — the turn advances after a timeout                              */
    /* ------------------------------------------------------------------ */

    @Test
    @DisplayName("E - the game advances to a valid next turn after the timeout")
    void exchange_timeout_advancesTheTurn() {
        GameState state = seed(actorId);
        stubTurnAdvanceTo(opponentId, 2);

        gameEngine.performExchange(matchId, actorId);
        assertThat(state.getCurrentTurnPlayerId()).isEqualTo(actorId);

        awaitUntil("the Exchange deadline to lapse",
                () -> state.getPendingAction() == null);

        assertThat(state.getCurrentTurnPlayerId()).isEqualTo(opponentId);
        assertThat(state.getTurnNumber()).isEqualTo(2);
        assertThat(state.isActionExecuted()).isFalse();
        assertThat(state.getPhase()).isEqualTo(GameEngine.PHASE_IN_PROGRESS);
        verify(turnManager, times(1)).advanceTurn(eq(matchId));
    }

    @Test
    @DisplayName("E - the match can never be left stuck in a pending Exchange")
    void exchange_timeout_leavesNoPendingState() {
        GameState state = seed(actorId);
        stubTurnAdvanceTo(opponentId, 2);

        gameEngine.performExchange(matchId, actorId);
        awaitUntil("the Exchange deadline to lapse",
                () -> state.getPendingAction() == null);

        // No pending action, no exchange pool left dangling, no armed timer left
        // holding a reference to the match.
        assertThat(state.getPendingAction()).isNull();
        assertThat(state.getLastActionResult()).isNotNull();
    }

    /* ------------------------------------------------------------------ */
    /*  F — stateVersion moves correctly                                    */
    /* ------------------------------------------------------------------ */

    @Test
    @DisplayName("F - stateVersion advances across declare, cancel and verdict")
    void exchange_timeout_incrementsStateVersion() {
        GameState state = seed(actorId);
        stubTurnAdvanceTo(opponentId, 2);

        gameEngine.performExchange(matchId, actorId);
        long afterDeclare = state.getStateVersion();
        assertThat(afterDeclare).isPositive();

        awaitUntil("the Exchange deadline to lapse",
                () -> state.getPendingAction() == null);

        // cancel seam sync + the resolver's verdict sync.
        assertThat(state.getStateVersion()).isGreaterThan(afterDeclare);
    }

    @Test
    @DisplayName("G - the Exchange carries an authoritative deadline onto the broadcast state")
    void exchange_declared_exposesDeadlineToTheActorOnly() {
        GameState state = seed(actorId);

        gameEngine.performExchange(matchId, actorId);

        assertThat(state.getPendingAction().getDeadlineAt()).isNotNull();

        GameStateResponse actorView = gameStateMapper.toResponse(state, actorId);
        GameStateResponse opponentView = gameStateMapper.toResponse(state, opponentId);

        assertThat(actorView.getPendingAction()).isNotNull();
        assertThat(actorView.getPendingAction().getExchangePool()).hasSize(4);
        assertThat(actorView.getPendingAction().getDeadlineAt()).isNotNull();

        // The opponent sees the action is pending but never the pool.
        assertThat(opponentView.getPendingAction()).isNotNull();
        assertThat(opponentView.getPendingAction().getExchangePool()).isNull();
    }

    /* ------------------------------------------------------------------ */
    /*  G — publication stays authoritative                                */
    /* ------------------------------------------------------------------ */

    @Test
    @DisplayName("G - the cancellation is broadcast as a FINISHED-version snapshot")
    void exchange_timeout_publishesAuthoritativeState() {
        GameState state = seed(actorId);
        stubTurnAdvanceTo(opponentId, 2);

        gameEngine.performExchange(matchId, actorId);
        awaitUntil("the Exchange deadline to lapse",
                () -> state.getPendingAction() == null);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<GameStateResponse> publicCaptor =
                ArgumentCaptor.forClass(GameStateResponse.class);

        verify(webSocketEventPublisher, atLeastOnce()).publishToMatch(
                eq(matchId), eq(WebSocketEventType.STATE_UPDATED), any(), publicCaptor.capture());

        List<GameStateResponse> published = publicCaptor.getAllValues();

        // Every public snapshot is card-free and carries a positive version, so
        // the client can order it and nothing hidden ever rides the public topic.
        for (GameStateResponse snapshot : published) {
            assertThat(snapshot.getStateVersion()).isPositive();
            for (var dto : snapshot.getPlayers()) {
                assertThat(dto.getCards()).isNull();
            }
            // The public topic never carries a private exchange pool either.
            if (snapshot.getPendingAction() != null) {
                assertThat(snapshot.getPendingAction().getExchangePool()).isNull();
            }
        }

        // The final published snapshot reflects the closed, cancelled Exchange.
        GameStateResponse last = published.get(published.size() - 1);
        assertThat(last.getPendingAction()).isNull();
        assertThat(last.getCurrentTurnPlayerId()).isEqualTo(opponentId);
    }

    @Test
    @DisplayName("G - a cancelled Exchange is never pushed to the opponent as private state")
    void exchange_timeout_doesNotLeakThePoolAfterwards() {
        GameState state = seed(actorId);
        stubTurnAdvanceTo(opponentId, 2);

        gameEngine.performExchange(matchId, actorId);
        awaitUntil("the Exchange deadline to lapse",
                () -> state.getPendingAction() == null);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<GameStateResponse> privateCaptor =
                ArgumentCaptor.forClass(GameStateResponse.class);
        verify(webSocketEventPublisher, atLeastOnce()).sendToUser(
                eq(opponentId), eq(matchId), eq(WebSocketEventType.PRIVATE_STATE),
                any(), privateCaptor.capture());

        for (GameStateResponse snapshot : privateCaptor.getAllValues()) {
            // The opponent never receives the actor's exchange pool, in any
            // snapshot taken while or after the Exchange was open.
            if (snapshot.getPendingAction() != null) {
                assertThat(snapshot.getPendingAction().getExchangePool()).isNull();
            }
            // And never the identity of the actor's cards — only the opponent's
            // own hand is ever populated in their private projection.
            for (var dto : snapshot.getPlayers()) {
                if (!dto.getUserId().equals(opponentId)) {
                    assertThat(dto.getCards()).isNull();
                }
            }
        }
    }

    @Test
    @DisplayName("G - the pending Exchange is never published to a non-member")
    void exchange_pending_isNotVisibleToStrangers() {
        GameState state = seed(actorId);

        gameEngine.performExchange(matchId, actorId);

        GameStateResponse stranger = gameStateMapper.toResponse(state, UUID.randomUUID());
        assertThat(stranger.getPendingAction()).isNotNull();
        assertThat(stranger.getPendingAction().getExchangePool()).isNull();
        verify(webSocketEventPublisher, never())
                .sendToUser(eq(UUID.randomUUID()), any(), any(), any(), any());
    }
}
