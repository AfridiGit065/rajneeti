package com.rajneeti.exception;

/**
 * Thrown when an operation is invalid for the room's current lifecycle state.
 */
public class InvalidRoomStateException extends RuntimeException {
    public InvalidRoomStateException(String message) {
        super(message);
    }
}