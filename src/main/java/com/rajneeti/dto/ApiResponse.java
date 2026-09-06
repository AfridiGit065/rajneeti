package com.rajneeti.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Unified API response envelope used across all REST endpoints.
 *
 * <p>Success responses:
 * <pre>
 * {
 *   "success": true,
 *   "message": "Operation successful",
 *   "data": { ... },
 *   "timestamp": "2024-01-01T00:00:00Z"
 * }
 * </pre>
 *
 * <p>Error responses (via {@link ErrorResponse}):
 * <pre>
 * {
 *   "success": false,
 *   "status": 400,
 *   "error": "BAD_REQUEST",
 *   "message": "Validation failed",
 *   "errors": [ { "field": "email", "message": "must not be blank" } ],
 *   "timestamp": "2024-01-01T00:00:00Z"
 * }
 * </pre>
 *
 * @param <T> the type of the payload in the {@code data} field
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private final boolean success;
    private final String  message;
    private final T       data;
    private final Integer status;
    private final String  error;

    @Builder.Default
    private final Instant timestamp = Instant.now();

    // ──────────────────────────────────────────────────────────────────────────
    // Factory helpers
    // ──────────────────────────────────────────────────────────────────────────

    /** Constructs a success response with data payload. */
    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .message("Operation successful")
                .data(data)
                .build();
    }

    /** Constructs a success response with a custom message and data payload. */
    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .build();
    }

    /** Constructs a success response with only a message (no data body). */
    public static ApiResponse<Void> success(String message) {
        return ApiResponse.<Void>builder()
                .success(true)
                .message(message)
                .build();
    }

    /** Constructs an error response. */
    public static <T> ApiResponse<T> error(int status, String error, String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .status(status)
                .error(error)
                .message(message)
                .build();
    }
}
