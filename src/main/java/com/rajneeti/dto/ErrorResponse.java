package com.rajneeti.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;

/**
 * Structured error response returned by {@link com.rajneeti.exception.GlobalExceptionHandler}.
 *
 * <p>Includes optional per-field validation errors to support rich client-side UX.
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    private final int    status;
    private final String error;
    private final String message;
    private final String path;

    @Builder.Default
    private final Instant timestamp = Instant.now();

    /** Per-field validation errors (populated for 400 Bad Request / validation failures). */
    private final List<FieldError> errors;

    // ──────────────────────────────────────────────────────────────────────────
    // Nested: per-field validation error
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Represents a single field-level validation violation.
     */
    @Getter
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class FieldError {

        /** The object/DTO field that failed validation. */
        private final String field;

        /** The rejected value (may be null if not applicable). */
        private final Object rejectedValue;

        /** Human-readable validation message. */
        private final String message;
    }
}
