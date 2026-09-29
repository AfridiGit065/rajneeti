package com.rajneeti.game;

import com.rajneeti.config.GameTimerProperties;
import com.rajneeti.entity.enums.MatchStatus;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Module 20 — authoritative block/challenge window scheduler.
 *
 * <p>The backend owns every window deadline. When an action is declared (or a
 * block/challenge opens a fresh window) this scheduler arms a single timer for
 * that pending action; when the timer fires it calls the existing
 * {@link ActionResolver} on the actor's behalf, which resolves the action,
 * applies its coins, advances the turn, bumps {@code stateVersion} and
 * broadcasts the authoritative state. Clients therefore never have to resolve
 * anything themselves and can never leave a match stuck at 00:00.
 *
 * <p>Every path is idempotent. The callback re-reads the live
 * {@link GameState#getPendingAction()}, and when the window has already been
 * closed by a manual resolve, a block, a challenge or a previous timeout it
 * simply returns without touching the board. Re-arming a window cancels the
 * previous future for that match, so a timer can never fire twice for one
 * action.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PendingActionTimeoutScheduler {

    private final GameEngine gameEngine;
    private final ActionResolver actionResolver;
    private final GameTimerProperties timerProperties;

    /** One armed future per match; a new window replaces the previous one. */
    private final Map<UUID, ScheduledFuture<?>> armed = new ConcurrentHashMap<>();

    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor(runnable -> {
                Thread thread = new Thread(runnable, "pending-action-timeout");
                thread.setDaemon(true);
                return thread;
            });

    /**
     * (Re)arms the window timer for a match. Called whenever a pending action
     * opens or its window changes. {@code seconds} of {@code 0} or less
     * resolves immediately, which is what an already-decided action needs.
     */
    public void armForPendingAction(UUID matchId, int seconds) {
        if (matchId == null) {
            return;
        }
        cancel(matchId);
        if (seconds <= 0) {
            resolveNow(matchId, "window-already-decided");
            return;
        }
        stampDeadline(matchId, seconds);
        ScheduledFuture<?> future = scheduler.schedule(
                () -> resolveNow(matchId, "window-timeout"),
                seconds, TimeUnit.SECONDS);
        armed.put(matchId, future);
        log.debug("Armed pending-action window for match {} in {}s", matchId, seconds);
    }

    /**
     * Writes the authoritative deadline onto the live pending action so the next
     * broadcast carries it and every client renders the same remaining time.
     */
    private void stampDeadline(UUID matchId, int seconds) {
        try {
            GameState state = gameEngine.getGameState(matchId);
            if (state == null) {
                return;
            }
            PendingAction pending = state.getPendingAction();
            if (pending == null) {
                return;
            }
            state.setPendingAction(pending.withDeadline(
                    LocalDateTime.now().plusSeconds(seconds)));
        } catch (Exception ex) {
            log.warn("Could not stamp the window deadline for match {}: {}", matchId, ex.getMessage());
        }
    }

    /** Arms the default block-window duration. */
    public void armBlockWindow(UUID matchId) {
        armForPendingAction(matchId, timerProperties.getBlockWindowSeconds());
    }

    /** Arms the default challenge-window duration. */
    public void armChallengeWindow(UUID matchId) {
        armForPendingAction(matchId, timerProperties.getChallengeWindowSeconds());
    }

    /** Cancels any armed timer for the match (window closed by a decision). */
    public void cancel(UUID matchId) {
        if (matchId == null) {
            return;
        }
        ScheduledFuture<?> future = armed.remove(matchId);
        if (future != null) {
            future.cancel(false);
        }
    }

    /**
     * The authoritative timeout path. Re-reads the live state and returns early
     * when the window is already closed, so a late timer can never resolve a
     * second time or clear a newer pending action.
     */
    private void resolveNow(UUID matchId, String reason) {
        armed.remove(matchId);
        try {
            GameState state = gameEngine.getGameState(matchId);
            if (state == null) {
                return;
            }
            if (state.getStatus() != MatchStatus.IN_PROGRESS
                    && state.getStatus() != MatchStatus.CREATED) {
                return;
            }
            PendingAction pending = state.getPendingAction();
            if (pending == null || pending.getActorUserId() == null) {
                return;
            }
            if (isExchange(pending.getType())) {
                // An Exchange needs the actor's card choice; the confirm seam
                // owns it, so this window never resolves generically.
                return;
            }
            log.info("Pending-action window '{}' expired in match {} (action={}, stateVersion={})",
                    reason, matchId, pending.getType(), state.getStateVersion());
            actionResolver.resolve(matchId, pending.getActorUserId());
        } catch (Exception ex) {
            // A timeout must never take the scheduler thread down, and must not
            // surface as a 500: the window is simply reported as unresolved.
            log.warn("Pending-action timeout failed for match {}: {}", matchId, ex.getMessage());
        }
    }

    private boolean isExchange(String actionType) {
        return GameEngine.ACTION_EXCHANGE.equals(actionType);
    }

    @PreDestroy
    void shutdown() {
        scheduler.shutdownNow();
    }
}
