package com.rajneeti.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when attempting to start a match for a room that already has an active match.
 * Maps to HTTP 409 Conflict.
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class MatchAlreadyExistsException extends RuntimeException {

    public MatchAlreadyExistsException(String message) {
        super(message);
    }
}
