package com.rajneeti.exception;

/**
 * Thrown when a refresh token operation fails (token expired, revoked, or not found).
 */
public class TokenRefreshException extends RuntimeException {
    public TokenRefreshException(String message) {
        super(message);
    }
}