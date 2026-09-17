package com.rajneeti.bot.ai;

/**
 * Raised when the AI provider answers with a non-success HTTP status.
 * Carries the status code so the caller can distinguish permanent auth
 * failures (401/403) from transient rate-limit / server errors.
 */
public class AiHttpException extends java.io.IOException {

    private final int statusCode;

    public AiHttpException(int statusCode, String message) {
        super(message);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public boolean isAuthFailure() {
        return statusCode == 401 || statusCode == 403;
    }
}