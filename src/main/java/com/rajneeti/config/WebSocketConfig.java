package com.rajneeti.config;

import com.rajneeti.websocket.WebSocketAuthInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket + STOMP infrastructure configuration.
 *
 * <p>Sets up the STOMP message broker, registers the WebSocket endpoint, and
 * configures destination prefixes.  No game events are wired here – this is
 * purely infrastructure plumbing for Module 01.
 *
 * <p>Clients connect via:
 * <pre>
 *   SockJS / STOMP: ws://localhost:8080/ws
 * </pre>
 *
 * <p>Destination routing:
 * <ul>
 *   <li>{@code /app/**}   → handled by {@code @MessageMapping} methods
 *   <li>{@code /topic/**} → simple in-memory broadcast broker
 *   <li>{@code /user/**}  → per-user queue broker
 * </ul>
 */
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final WebSocketProperties wsProperties;
    private final WebSocketAuthInterceptor webSocketAuthInterceptor;

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        // Module 22 — authenticate the CONNECT frame (JWT) and authorize every
        // subscription on the inbound channel before frames reach handlers.
        registration.interceptors(webSocketAuthInterceptor);
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // In-memory simple broker for topic (broadcast) and user (point-to-point)
        registry.enableSimpleBroker(
                wsProperties.getTopicPrefix(),
                wsProperties.getQueuePrefix()
        );
        // All @MessageMapping methods must be prefixed with /app
        registry.setApplicationDestinationPrefixes(wsProperties.getAppDestinationPrefix());
        // Enable user-specific destinations (/user/...)
        registry.setUserDestinationPrefix(wsProperties.getUserDestinationPrefix());
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint(wsProperties.getEndpoint())
                .setAllowedOrigins(wsProperties.getAllowedOrigins().toArray(String[]::new))
                .withSockJS();   // SockJS fallback for browsers without native WS support
    }
}
