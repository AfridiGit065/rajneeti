package com.rajneeti.exception;

/**
 * Thrown when a provided JWT or authentication token is malformed, invalid, or expired.
 */
public class InvalidTokenException extends RuntimeException {
    public InvalidTokenException(String message) {
        super(message);
    }
}