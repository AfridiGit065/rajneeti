package com.rajneeti.dto.websocket;

/**
 * Discriminator of every event carried by the {@link WebSocketEvent} envelope.
 *
 * <p>Module 22 — these are the stable, wire-level event type identifiers the
 * frontend subscribes to. The required game events are: JOIN_ROOM, LEAVE_ROOM,
 * READY, UNREADY, START_GAME, PLAYER_ACTION, TURN_CHANGE, GAME_OVER, CHALLENGE,
 * BLOCK, CARD_REVEAL, CHAT_MESSAGE and WEBSOCKET_ERROR. A few room-lifecycle
 * types (ROOM_CREATED, ROOM_CANCELLED, HOST_CHANGED, ROOM_UPDATED) are kept so
 * the existing Module 04 room broadcasts map onto the same envelope instead of
 * producing two incompatible event shapes.
 *
 * <p>Module 23 — STATE_UPDATED (full, viewer-neutral GameStateResponse snapshot
 * broadcast to the match topic after every broadcastable mutation) and
 * PRIVATE_STATE (full per-viewer snapshot routed to a single user's queue, used
 * for resync replies and for private card/exchange-pool data).
 */
public enum WebSocketEventType {
    JOIN_ROOM,
    LEAVE_ROOM,
    READY,
    UNREADY,
    START_GAME,
    PLAYER_ACTION,
    TURN_CHANGE,
    GAME_OVER,
    CHALLENGE,
    BLOCK,
    CARD_REVEAL,
    CHAT_MESSAGE,
    WEBSOCKET_ERROR,
    ROOM_CREATED,
    ROOM_CANCELLED,
    HOST_CHANGED,
    ROOM_UPDATED,
    STATE_UPDATED,
    PRIVATE_STATE
}