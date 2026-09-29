package com.rajneeti.game;

import com.rajneeti.dto.websocket.WebSocketEventType;
import com.rajneeti.entity.enums.PlayerStatus;
import com.rajneeti.websocket.WebSocketEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Module 23 — pushes authoritative {@link GameState} snapshots to connected
 * clients.
 *
 * <p>Two channels are produced after every broadcastable mutation:
 * <ul>
 *   <li>{@code STATE_UPDATED} — a full, viewer-neutral projection ({@code null}
 *       viewer) broadcast to the match topic. It is deterministic and hides all
 *       card data, so every subscribed client can overlay it onto its local
 *       view without leaking secrets.</li>
 *   <li>{@code PRIVATE_STATE} — a per-viewer projection (own cards visible) sent
 *       to each <b>still-active</b> player's private queue. It is the
 *       authoritative full snapshot used for the initial fill and for resync
 *       replies.</li>
 * </ul>
 *
 * <p>Once a player is eliminated they stop receiving {@code PRIVATE_STATE}
 * entirely — not even their own hand is pushed to them — because a live table no
 * longer has any private state to hand out. They keep following the match on the
 * public topic, which carries everything an out-of-play viewer legitimately
 * needs (influence counts, coins, status, the log and the winner), and a resync
 * from an eliminated viewer is answered with that same public projection. See
 * {@code PrivateCardSecurityTest}.
 *
 * <p>Both channels carry the SAME {@code stateVersion} (incremented once per
 * sync) so the client can order events across the two channels without skew and
 * can detect a stale / out-of-order / missing snapshot and request a resync.
 *
 * <p>Publishing is best-effort (mirrors {@link WebSocketEventPublisher}): never
 * throws into the authoritative mutation flow.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GameStateSyncService {

    private final GameStateMapper gameStateMapper;
    private final WebSocketEventPublisher publisher;

    /**
     * Bumps {@code stateVersion} once, then publishes the public
     * {@code STATE_UPDATED} snapshot to the match topic and a per-viewer
     * {@code PRIVATE_STATE} snapshot to every still-active player's queue.
     *
     * <p>Eliminated players are deliberately skipped: see the class Javadoc. They
     * still receive {@code STATE_UPDATED} on the match topic.
     *
     * <p>The bump and both publishes are guarded by {@code synchronized(state)}
     * — the same per-match monitor the mutation seams hold — so concurrent
     * mutations cannot emit duplicate or regressed versions.
     *
     * @return the new authoritative version
     */
    public long sync(GameState state) {
        if (state == null || state.getMatchId() == null) {
            return state != null ? state.getStateVersion() : 0;
        }
        synchronized (state) {
            long next = state.getStateVersion() + 1;
            state.setStateVersion(next);

            publisher.publishToMatch(state.getMatchId(), WebSocketEventType.STATE_UPDATED, null,
                    gameStateMapper.toResponse(state, null));

            if (state.getPlayers() != null) {
                for (GamePlayerState player : state.getPlayers()) {
                    if (player.getUserId() == null) {
                        continue;
                    }
                    if (player.getStatus() != PlayerStatus.ACTIVE) {
                        // An eliminated player has left the live table: pushing a
                        // per-viewer snapshot to them would keep handing out
                        // private state for a match they no longer play.
                        continue;
                    }
                    publisher.sendToUser(player.getUserId(), state.getMatchId(),
                            WebSocketEventType.PRIVATE_STATE, null,
                            gameStateMapper.toResponse(state, player.getUserId()));
                }
            }
            return next;
        }
    }

    /**
     * Sends a {@code PRIVATE_STATE} snapshot of the CURRENT version to a single
     * user without bumping the counter. Used by the resync handler so a sync
     * reply can never be mistaken for a new mutation (and cannot cause a
     * duplicate-version clash on the client).
     *
     * <p>An eliminated viewer is answered with the viewer-neutral (public)
     * projection instead, so a resync can never be used to re-obtain private
     * state after elimination.
     */
    public void sendCurrentState(GameState state, UUID userId) {
        if (state == null || state.getMatchId() == null || userId == null) {
            return;
        }
        synchronized (state) {
            UUID viewerId = isActivePlayer(state, userId) ? userId : null;
            publisher.sendToUser(userId, state.getMatchId(),
                    WebSocketEventType.PRIVATE_STATE, null,
                    gameStateMapper.toResponse(state, viewerId));
        }
    }

    /** Whether the given user still holds an active seat in this match. */
    private boolean isActivePlayer(GameState state, UUID userId) {
        if (state.getPlayers() == null) {
            return false;
        }
        return state.getPlayers().stream()
                .anyMatch(p -> userId.equals(p.getUserId()) && p.getStatus() == PlayerStatus.ACTIVE);
    }
}