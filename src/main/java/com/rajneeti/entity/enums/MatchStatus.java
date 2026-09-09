package com.rajneeti.entity.enums;

/**
 * Status lifecycle of a game match.
 *
 * Lifecycle:
 * CREATED -> IN_PROGRESS -> FINISHED / CANCELLED
 */
public enum MatchStatus {
    CREATED,
    IN_PROGRESS,
    FINISHED,
    CANCELLED
}