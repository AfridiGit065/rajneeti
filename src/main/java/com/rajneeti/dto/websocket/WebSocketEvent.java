package com.rajneeti.dto.websocket;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Generic envelope for every WebSocket/STOMP event on the platform.
 *
 * <p>All events — room broadcasts, match broadcasts and private per-user
 * messages — share this single shape so the frontend can parse an event with
 * one code path. {@code senderId} is always the server-side identity (the
 * authenticated user who triggered the event, or {@code null} for pure
 * server/authored events); clients never supply identity fields.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WebSocketEvent {

    /** Event discriminator (see {@link WebSocketEventType}). */
    private WebSocketEventType eventType;

    /** Room this event belongs to, when applicable. */
    private UUID roomId;

    /** Match this event belongs to, when applicable. */
    private UUID matchId;

    /** Server-derived identity of the triggering user, when applicable. */
    private UUID senderId;

    /** Server-side event timestamp. */
    private Instant timestamp;

    /** Typed payload DTO; safe/public data only, never hidden cards or deck. */
    private Object payload;
}