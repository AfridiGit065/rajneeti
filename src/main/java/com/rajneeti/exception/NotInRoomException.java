package com.rajneeti.exception;

/**
 * Thrown when an operation requires the user to be a player in the room, but they are not.
 */
public class NotInRoomException extends RuntimeException {
    public NotInRoomException(String message) {
        super(message);
    }
}