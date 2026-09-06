package com.rajneeti.exception;

/**
 * Thrown when a room cannot be found by ID or room code.
 */
public class RoomNotFoundException extends RuntimeException {
    public RoomNotFoundException(String message) {
        super(message);
    }
}