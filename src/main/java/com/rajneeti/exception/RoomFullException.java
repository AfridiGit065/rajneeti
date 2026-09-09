package com.rajneeti.exception;

/**
 * Thrown when a player attempts to join a room that has reached maximum capacity.
 */
public class RoomFullException extends RuntimeException {
    public RoomFullException(String message) {
        super(message);
    }
}