package com.rajneeti.exception;

import com.rajneeti.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Global exception handler for all REST controllers.
 *
 * <p>Translates runtime exceptions into structured {@link ErrorResponse} payloads
 * with appropriate HTTP status codes. All exception handlers are logged at
 * appropriate levels:
 * <ul>
 *   <li>Client errors (4xx) → WARN
 *   <li>Server errors (5xx) → ERROR
 * </ul>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ──────────────────────────────────────────────────────────────────────────
    // 400 Bad Request
    // ──────────────────────────────────────────────────────────────────────────

    /** @{@code @Valid} / {@code @Validated} bean validation on request bodies. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        List<ErrorResponse.FieldError> fieldErrors = ex.getBindingResult()
                .getAllErrors()
                .stream()
                .map(error -> {
                    if (error instanceof FieldError fe) {
                        return ErrorResponse.FieldError.builder()
                                .field(fe.getField())
                                .rejectedValue(fe.getRejectedValue())
                                .message(fe.getDefaultMessage())
                                .build();
                    }
                    return ErrorResponse.FieldError.builder()
                            .field(error.getObjectName())
                            .message(error.getDefaultMessage())
                            .build();
                })
                .collect(Collectors.toList());

        log.warn("Validation failed for [{}]: {} errors", request.getRequestURI(), fieldErrors.size());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                ErrorResponse.builder()
                        .status(HttpStatus.BAD_REQUEST.value())
                        .error("VALIDATION_FAILED")
                        .message("Request validation failed. Please check the 'errors' field.")
                        .path(request.getRequestURI())
                        .errors(fieldErrors)
                        .build());
    }

    /** Path / query parameter type mismatches. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex,
            HttpServletRequest request) {

        String message = "Parameter '%s' should be of type '%s'"
                .formatted(ex.getName(), ex.getRequiredType() != null
                        ? ex.getRequiredType().getSimpleName() : "unknown");

        log.warn("Type mismatch [{}]: {}", request.getRequestURI(), message);

        return badRequest("TYPE_MISMATCH", message, request);
    }

    /** Missing required request parameters. */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParam(
            MissingServletRequestParameterException ex,
            HttpServletRequest request) {

        log.warn("Missing parameter [{}]: {}", request.getRequestURI(), ex.getMessage());
        return badRequest("MISSING_PARAMETER", ex.getMessage(), request);
    }

    /** Malformed or unreadable JSON body. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadableMessage(
            HttpMessageNotReadableException ex,
            HttpServletRequest request) {

        log.warn("Unreadable message [{}]: {}", request.getRequestURI(), ex.getMessage());
        return badRequest("MALFORMED_JSON", "Request body is malformed or unreadable.", request);
    }

    /** Bean validation on method parameters (e.g., path variables). */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(
            ConstraintViolationException ex,
            HttpServletRequest request) {

        List<ErrorResponse.FieldError> fieldErrors = ex.getConstraintViolations()
                .stream()
                .map((ConstraintViolation<?> v) -> ErrorResponse.FieldError.builder()
                        .field(v.getPropertyPath().toString())
                        .rejectedValue(v.getInvalidValue())
                        .message(v.getMessage())
                        .build())
                .collect(Collectors.toList());

        log.warn("Constraint violations [{}]: {}", request.getRequestURI(), fieldErrors.size());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                ErrorResponse.builder()
                        .status(HttpStatus.BAD_REQUEST.value())
                        .error("CONSTRAINT_VIOLATION")
                        .message("Constraint violation(s) detected.")
                        .path(request.getRequestURI())
                        .errors(fieldErrors)
                        .build());
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 401 Unauthorized
    // ──────────────────────────────────────────────────────────────────────────

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(
            BadCredentialsException ex,
            HttpServletRequest request) {

        log.warn("Bad credentials [{}]: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                ErrorResponse.builder()
                        .status(HttpStatus.UNAUTHORIZED.value())
                        .error("INVALID_CREDENTIALS")
                        .message("Invalid username or password.")
                        .path(request.getRequestURI())
                        .build());
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 403 Forbidden
    // ──────────────────────────────────────────────────────────────────────────

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(
            AccessDeniedException ex,
            HttpServletRequest request) {

        log.warn("Access denied [{}]: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                ErrorResponse.builder()
                        .status(HttpStatus.FORBIDDEN.value())
                        .error("ACCESS_DENIED")
                        .message(ex.getMessage())
                        .path(request.getRequestURI())
                        .build());
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 404 Not Found
    // ──────────────────────────────────────────────────────────────────────────

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(
            ResourceNotFoundException ex,
            HttpServletRequest request) {

        log.warn("Resource not found [{}]: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                ErrorResponse.builder()
                        .status(HttpStatus.NOT_FOUND.value())
                        .error("RESOURCE_NOT_FOUND")
                        .message(ex.getMessage())
                        .path(request.getRequestURI())
                        .build());
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 405 Method Not Allowed
    // ──────────────────────────────────────────────────────────────────────────

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException ex,
            HttpServletRequest request) {

        log.warn("Method not supported [{}]: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(
                ErrorResponse.builder()
                        .status(HttpStatus.METHOD_NOT_ALLOWED.value())
                        .error("METHOD_NOT_ALLOWED")
                        .message(ex.getMessage())
                        .path(request.getRequestURI())
                        .build());
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 409 Conflict
    // ──────────────────────────────────────────────────────────────────────────

    @ExceptionHandler(ResourceAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleConflict(
            ResourceAlreadyExistsException ex,
            HttpServletRequest request) {

        log.warn("Conflict [{}]: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                ErrorResponse.builder()
                        .status(HttpStatus.CONFLICT.value())
                        .error("RESOURCE_CONFLICT")
                        .message(ex.getMessage())
                        .path(request.getRequestURI())
                        .build());
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 422 Unprocessable Entity
    // ──────────────────────────────────────────────────────────────────────────

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(
            BusinessException ex,
            HttpServletRequest request) {

        log.warn("Business rule violation [{}]: [{}] {}", request.getRequestURI(),
                ex.getErrorCode(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(
                ErrorResponse.builder()
                        .status(HttpStatus.UNPROCESSABLE_ENTITY.value())
                        .error(ex.getErrorCode())
                        .message(ex.getMessage())
                        .path(request.getRequestURI())
                        .build());
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 500 Internal Server Error – catch-all
    // ──────────────────────────────────────────────────────────────────────────

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(
            Exception ex,
            HttpServletRequest request) {

        log.error("Unhandled exception [{}]:", request.getRequestURI(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ErrorResponse.builder()
                        .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                        .error("INTERNAL_SERVER_ERROR")
                        .message("An unexpected error occurred. Please try again later.")
                        .path(request.getRequestURI())
                        .build());
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Private helpers
    // ──────────────────────────────────────────────────────────────────────────

    private ResponseEntity<ErrorResponse> badRequest(String error, String message,
                                                       HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                ErrorResponse.builder()
                        .status(HttpStatus.BAD_REQUEST.value())
                        .error(error)
                        .message(message)
                        .path(request.getRequestURI())
                        .build());
    }
}
