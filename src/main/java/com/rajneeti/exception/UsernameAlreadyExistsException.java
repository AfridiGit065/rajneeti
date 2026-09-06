package com.rajneeti.exception;

/**
 * Thrown when registration fails because the requested username is already in use.
 */
public class UsernameAlreadyExistsException extends RuntimeException {
    public UsernameAlreadyExistsException(String message) {
        super(message);
    }
}