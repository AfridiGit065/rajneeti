package com.rajneeti.exception;

/**
 * Thrown when a player is already a member of the room they are attempting to join.
 */
public class AlreadyInRoomException extends RuntimeException {
    public AlreadyInRoomException(String message) {
        super(message);
    }
}