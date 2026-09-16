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
    ROOM_UPDATED
}