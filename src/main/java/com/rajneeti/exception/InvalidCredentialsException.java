package com.rajneeti.exception;

/**
 * Thrown when login authentication fails due to invalid credentials.
 */
public class InvalidCredentialsException extends RuntimeException {
    public InvalidCredentialsException(String message) {
        super(message);
    }
}