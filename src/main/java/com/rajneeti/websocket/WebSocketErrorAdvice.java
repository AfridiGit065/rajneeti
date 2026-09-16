package com.rajneeti.websocket;

import com.rajneeti.dto.websocket.ErrorPayload;
import com.rajneeti.dto.websocket.WebSocketEventType;
import com.rajneeti.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.web.bind.annotation.ControllerAdvice;

import java.security.Principal;
import java.time.Instant;
import java.util.UUID;

/**
 * Converts exceptions thrown inside {@code @MessageMapping} handlers into a
 * private {@link WebSocketEventType#WEBSOCKET_ERROR} delivered to the affected
 * user's queue — mirroring the REST {@code ApiError} contract on the wire.
 */
@Slf4j
@ControllerAdvice
@RequiredArgsConstructor
public class WebSocketErrorAdvice {

    private final WebSocketEventPublisher webSocketEventPublisher;

    @MessageExceptionHandler(Exception.class)
    public void handleException(Exception ex, Message<?> message, Principal principal) {
        UUID userId = resolveUserId(principal);
        if (userId == null) {
            log.warn("WebSocket message failed without an authenticated sender: {}", ex.getMessage());
            return;
        }

        String code = (ex instanceof BusinessException businessException)
                ? businessException.getErrorCode()
                : "WEBSOCKET_ERROR";

        log.info("WebSocket message failed for user {}: {} ({})", userId, ex.getMessage(), code);

        webSocketEventPublisher.sendToUser(userId, WebSocketEventType.WEBSOCKET_ERROR, null,
                ErrorPayload.builder()
                        .errorCode(code)
                        .message(ex.getMessage())
                        .timestamp(Instant.now())
                        .build());
    }

    private UUID resolveUserId(Principal principal) {
        if (principal instanceof WebSocketPrincipal wsPrincipal) {
            return wsPrincipal.getUserId();
        }
        if (principal != null && principal.getName() != null) {
            try {
                return UUID.fromString(principal.getName());
            } catch (IllegalArgumentException ignored) {
                return null;
            }
        }
        return null;
    }
}