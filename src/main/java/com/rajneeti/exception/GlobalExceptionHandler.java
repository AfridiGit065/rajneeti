package com.rajneeti.exception;

import com.rajneeti.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;

/**
 * Centralized exception handler for all REST controllers.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ─── 400 Bad Request ────────────────────────────────────────────────────────

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        List<ErrorResponse.FieldError> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(fieldError -> ErrorResponse.FieldError.builder()
                        .field(fieldError.getField())
                        .rejectedValue(safeRejectedValue(fieldError))
                        .message(fieldError.getDefaultMessage())
                        .build())
                .toList();

        log.warn("Validation failed for [{}]: {} field error(s)", request.getRequestURI(), fieldErrors.size());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                ErrorResponse.builder()
                        .status(HttpStatus.BAD_REQUEST.value())
                        .error("VALIDATION_FAILED")
                        .message("Request body validation failed. See 'errors' field.")
                        .path(request.getRequestURI())
                        .errors(fieldErrors)
                        .build());
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(
            ConstraintViolationException ex,
            HttpServletRequest request) {

        List<ErrorResponse.FieldError> errors = ex.getConstraintViolations()
                .stream()
                .map(cv -> ErrorResponse.FieldError.builder()
                        .field(cv.getPropertyPath().toString())
                        .message(cv.getMessage())
                        .build())
                .toList();

        log.warn("Constraint violation on [{}]: {}", request.getRequestURI(), ex.getMessage());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                ErrorResponse.builder()
                        .status(HttpStatus.BAD_REQUEST.value())
                        .error("CONSTRAINT_VIOLATION")
                        .message("Validation constraint violated.")
                        .path(request.getRequestURI())
                        .errors(errors)
                        .build());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleNotReadable(
            HttpMessageNotReadableException ex,
            HttpServletRequest request) {
        log.warn("Malformed JSON on [{}]: {}", request.getRequestURI(), ex.getMessage());
        return badRequest("MALFORMED_JSON", "Request body is malformed or missing.", request);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParam(
            MissingServletRequestParameterException ex,
            HttpServletRequest request) {
        return badRequest("MISSING_PARAMETER",
                "Required parameter '" + ex.getParameterName() + "' is missing.", request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex,
            HttpServletRequest request) {
        return badRequest("TYPE_MISMATCH",
                "Parameter '" + ex.getName() + "' should be of type " +
                        (ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown"),
                request);
    }

    @ExceptionHandler(TokenRefreshException.class)
    public ResponseEntity<ErrorResponse> handleTokenRefresh(
            TokenRefreshException ex,
            HttpServletRequest request) {
        log.warn("Token refresh failed on [{}]: {}", request.getRequestURI(), ex.getMessage());
        return badRequest("TOKEN_REFRESH_ERROR", ex.getMessage(), request);
    }

    @ExceptionHandler(NotInRoomException.class)
    public ResponseEntity<ErrorResponse> handleNotInRoom(
            NotInRoomException ex,
            HttpServletRequest request) {
        log.warn("Not in room on [{}]: {}", request.getRequestURI(), ex.getMessage());
        return badRequest("NOT_IN_ROOM", ex.getMessage(), request);
    }

    @ExceptionHandler(RoomNotJoinableException.class)
    public ResponseEntity<ErrorResponse> handleRoomNotJoinable(
            RoomNotJoinableException ex,
            HttpServletRequest request) {
        log.warn("Room not joinable on [{}]: {}", request.getRequestURI(), ex.getMessage());
        return badRequest("ROOM_NOT_JOINABLE", ex.getMessage(), request);
    }

    @ExceptionHandler(InvalidRoomStateException.class)
    public ResponseEntity<ErrorResponse> handleInvalidRoomState(
            InvalidRoomStateException ex,
            HttpServletRequest request) {
        log.warn("Invalid room state on [{}]: {}", request.getRequestURI(), ex.getMessage());
        return badRequest("INVALID_ROOM_STATE", ex.getMessage(), request);
    }

    // ─── 401 Unauthorized ───────────────────────────────────────────────────────

    @ExceptionHandler({InvalidCredentialsException.class, BadCredentialsException.class})
    public ResponseEntity<ErrorResponse> handleInvalidCredentials(
            Exception ex,
            HttpServletRequest request) {
        log.warn("Authentication failed on [{}]: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                ErrorResponse.builder()
                        .status(HttpStatus.UNAUTHORIZED.value())
                        .error("INVALID_CREDENTIALS")
                        .message("Invalid email or password.")
                        .path(request.getRequestURI())
                        .build());
    }

    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<ErrorResponse> handleInvalidToken(
            InvalidTokenException ex,
            HttpServletRequest request) {
        log.warn("Invalid token on [{}]: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                ErrorResponse.builder()
                        .status(HttpStatus.UNAUTHORIZED.value())
                        .error("INVALID_TOKEN")
                        .message(ex.getMessage())
                        .path(request.getRequestURI())
                        .build());
    }

    // ─── 403 Forbidden ──────────────────────────────────────────────────────────

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(
            AccessDeniedException ex,
            HttpServletRequest request) {
        log.warn("Access denied on [{}]: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                ErrorResponse.builder()
                        .status(HttpStatus.FORBIDDEN.value())
                        .error("ACCESS_DENIED")
                        .message("Access is denied. You lack permissions for this resource.")
                        .path(request.getRequestURI())
                        .build());
    }

    @ExceptionHandler(NotRoomHostException.class)
    public ResponseEntity<ErrorResponse> handleNotRoomHost(
            NotRoomHostException ex,
            HttpServletRequest request) {
        log.warn("Not room host on [{}]: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                ErrorResponse.builder()
                        .status(HttpStatus.FORBIDDEN.value())
                        .error("NOT_ROOM_HOST")
                        .message(ex.getMessage())
                        .path(request.getRequestURI())
                        .build());
    }

    // ─── 404 Not Found ──────────────────────────────────────────────────────────

    @ExceptionHandler({ResourceNotFoundException.class, RoomNotFoundException.class})
    public ResponseEntity<ErrorResponse> handleNotFound(
            RuntimeException ex,
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

    // ─── 405 Method Not Allowed ──────────────────────────────────────────────────

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

    // ─── 409 Conflict ───────────────────────────────────────────────────────────

    @ExceptionHandler(UsernameAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleUsernameExists(
            UsernameAlreadyExistsException ex,
            HttpServletRequest request) {
        log.warn("Username conflict on [{}]: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                ErrorResponse.builder()
                        .status(HttpStatus.CONFLICT.value())
                        .error("USERNAME_ALREADY_EXISTS")
                        .message(ex.getMessage())
                        .path(request.getRequestURI())
                        .build());
    }

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleEmailExists(
            EmailAlreadyExistsException ex,
            HttpServletRequest request) {
        log.warn("Email conflict on [{}]: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                ErrorResponse.builder()
                        .status(HttpStatus.CONFLICT.value())
                        .error("EMAIL_ALREADY_EXISTS")
                        .message(ex.getMessage())
                        .path(request.getRequestURI())
                        .build());
    }

    @ExceptionHandler(MatchAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleMatchAlreadyExists(
            MatchAlreadyExistsException ex,
            HttpServletRequest request) {
        log.warn("Match already exists conflict on [{}]: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                ErrorResponse.builder()
                        .status(HttpStatus.CONFLICT.value())
                        .error("MATCH_ALREADY_EXISTS")
                        .message(ex.getMessage())
                        .path(request.getRequestURI())
                        .build());
    }

    @ExceptionHandler(MatchNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleMatchNotFound(
            MatchNotFoundException ex,
            HttpServletRequest request) {
        log.warn("Match not found [{}]: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                ErrorResponse.builder()
                        .status(HttpStatus.NOT_FOUND.value())
                        .error("MATCH_NOT_FOUND")
                        .message(ex.getMessage())
                        .path(request.getRequestURI())
                        .build());
    }

    @ExceptionHandler(RoomFullException.class)
    public ResponseEntity<ErrorResponse> handleRoomFull(
            RoomFullException ex,
            HttpServletRequest request) {
        log.warn("Room full conflict on [{}]: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                ErrorResponse.builder()
                        .status(HttpStatus.CONFLICT.value())
                        .error("ROOM_FULL")
                        .message(ex.getMessage())
                        .path(request.getRequestURI())
                        .build());
    }

    @ExceptionHandler(AlreadyInRoomException.class)
    public ResponseEntity<ErrorResponse> handleAlreadyInRoom(
            AlreadyInRoomException ex,
            HttpServletRequest request) {
        log.warn("Already in room conflict on [{}]: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                ErrorResponse.builder()
                        .status(HttpStatus.CONFLICT.value())
                        .error("ALREADY_IN_ROOM")
                        .message(ex.getMessage())
                        .path(request.getRequestURI())
                        .build());
    }

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

    // ─── 422 Unprocessable Entity ───────────────────────────────────────────────

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

    // ─── 500 Internal Server Error ──────────────────────────────────────────────

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

    private Object safeRejectedValue(FieldError fieldError) {
        String field = fieldError.getField().toLowerCase();
        if (field.contains("password") || field.contains("token") || field.contains("secret")) {
            return "[PROTECTED]";
        }
        return fieldError.getRejectedValue();
    }
}
