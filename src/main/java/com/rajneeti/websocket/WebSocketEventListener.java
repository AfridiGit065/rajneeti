package com.rajneeti.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

/**
 * Listens to WebSocket lifecycle events for logging and future presence tracking.
 *
 * <p><strong>Module 01 implementation:</strong> Logging only.
 * Player session management and game event routing will be added in later modules.
 */
@Slf4j
@Component
public class WebSocketEventListener {

    /**
     * Fired when a new STOMP client completes its handshake.
     */
    @EventListener
    public void handleWebSocketConnect(SessionConnectedEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        log.info("WebSocket client connected – sessionId: {}", accessor.getSessionId());
    }

    /**
     * Fired when a STOMP client subscribes to a destination.
     */
    @EventListener
    public void handleWebSocketSubscribe(SessionSubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        log.debug("WebSocket subscribe – sessionId: {}, destination: {}",
                accessor.getSessionId(), accessor.getDestination());
    }

    /**
     * Fired when a STOMP client disconnects.
     *
     * <p>TODO (Module 03+): Notify other players in the same room about this player leaving.
     */
    @EventListener
    public void handleWebSocketDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        log.info("WebSocket client disconnected – sessionId: {}, reason: {}",
                accessor.getSessionId(), event.getCloseStatus());
    }
}
