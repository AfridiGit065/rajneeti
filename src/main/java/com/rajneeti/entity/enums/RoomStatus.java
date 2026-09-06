package com.rajneeti.entity.enums;

/**
 * Status lifecycle of a multiplayer game room.
 *
 * Lifecycle:
 * WAITING -> STARTING -> IN_GAME -> FINISHED / CANCELLED
 */
public enum RoomStatus {
    WAITING,
    STARTING,
    IN_GAME,
    FINISHED,
    CANCELLED
}