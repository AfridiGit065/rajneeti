package com.rajneeti.exception;

/**
 * Thrown when registration fails because the requested email is already in use.
 */
public class EmailAlreadyExistsException extends RuntimeException {
    public EmailAlreadyExistsException(String message) {
        super(message);
    }
}