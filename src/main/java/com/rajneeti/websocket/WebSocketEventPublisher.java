package com.rajneeti.websocket;

import com.rajneeti.config.WebSocketProperties;
import com.rajneeti.dto.websocket.WebSocketEvent;
import com.rajneeti.dto.websocket.WebSocketEventType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.util.UUID;

/**
 * Single, server-side entry point for publishing {@link WebSocketEvent}s.
 *
 * <p>Module 22 — every room broadcast, match broadcast and private per-user
 * message funnels through here. Publishing is best-effort (mirroring the
 * existing RoomService pattern): a broker hiccup logs a warning and never
 * breaks the authoritative REST operation that triggered the event.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketEventPublisher {

    private final SimpMessagingTemplate messagingTemplate;
    private final WebSocketProperties wsProperties;

    private static final String ROOM_QUEUE = "/queue/events";

    /**
     * Defers a broadcast until the surrounding transaction has committed.
     *
     * <p>Subscribers treat every room event as "re-read the room now". If the
     * frame left the server before the commit, that re-read races the very write
     * the event announces and observes the previous state, which the client
     * then caches as if it were authoritative (a stale readiness snapshot, for
     * example). Sending after commit makes the announced state the only state a
     * subscriber can observe. Outside a transaction the send is immediate, so
     * non-transactional callers behave exactly as before.
     */
    private void afterCommit(Runnable broadcast) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    broadcast.run();
                }
            });
            return;
        }
        broadcast.run();
    }

    private WebSocketEvent build(WebSocketEventType type, UUID roomId, UUID matchId,
                                 UUID senderId, Object payload) {
        return WebSocketEvent.builder()
                .eventType(type)
                .roomId(roomId)
                .matchId(matchId)
                .senderId(senderId)
                .timestamp(Instant.now())
                .payload(payload)
                .build();
    }

    /** Broadcasts an event to everyone subscribed to a room topic. */
    public void publishToRoom(UUID roomId, WebSocketEventType type, UUID senderId, Object payload) {
        if (roomId == null) {
            return;
        }
        String destination = wsProperties.getTopicPrefix() + "/rooms/" + roomId;
        afterCommit(() -> {
            try {
                messagingTemplate.convertAndSend(destination,
                        build(type, roomId, null, senderId, payload));
            } catch (Exception ex) {
                log.warn("Failed to broadcast {} to {}: {}", type, destination, ex.getMessage());
            }
        });
    }

    /** Broadcasts an event to everyone subscribed to a match topic. */
    public void publishToMatch(UUID matchId, WebSocketEventType type, UUID senderId, Object payload) {
        if (matchId == null) {
            return;
        }
        String destination = wsProperties.getTopicPrefix() + "/matches/" + matchId;
        afterCommit(() -> {
            try {
                messagingTemplate.convertAndSend(destination,
                        build(type, null, matchId, senderId, payload));
            } catch (Exception ex) {
                log.warn("Failed to broadcast {} to {}: {}", type, destination, ex.getMessage());
            }
        });
    }

    /** Sends an event to a single user's private queue ({@code /user/{id}/queue/events}). */
    public void sendToUser(UUID userId, WebSocketEventType type, UUID senderId, Object payload) {
        sendToUser(userId, null, type, senderId, payload);
    }

    /**
     * Sends an event to a single user's private queue, carrying a matchId.
     *
     * <p>Module 23 — private events that pertain to a specific match (resync
     * replies, private card draws, per-viewer snapshots) must carry the matchId
     * so the frontend can route them to the right match's store.
     */
    public void sendToUser(UUID userId, UUID matchId, WebSocketEventType type, UUID senderId, Object payload) {
        if (userId == null) {
            return;
        }
        // The STOMP principal name is the user UUID (see WebSocketPrincipal),
        // so convertAndSendToUser resolves the private queue by userId.
        afterCommit(() -> {
            try {
                messagingTemplate.convertAndSendToUser(userId.toString(), ROOM_QUEUE,
                        build(type, null, matchId, senderId, payload));
            } catch (Exception ex) {
                log.warn("Failed to send {} to user {}: {}", type, userId, ex.getMessage());
            }
        });
    }
}