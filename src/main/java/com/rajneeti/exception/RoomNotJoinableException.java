package com.rajneeti.exception;

/**
 * Thrown when a room cannot be joined (e.g., game already started, cancelled, or finished).
 */
public class RoomNotJoinableException extends RuntimeException {
    public RoomNotJoinableException(String message) {
        super(message);
    }
}