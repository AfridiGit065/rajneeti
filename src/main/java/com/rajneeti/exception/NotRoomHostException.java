package com.rajneeti.exception;

/**
 * Thrown when a non-host player attempts an operation reserved strictly for the room host.
 */
public class NotRoomHostException extends RuntimeException {
    public NotRoomHostException(String message) {
        super(message);
    }
}