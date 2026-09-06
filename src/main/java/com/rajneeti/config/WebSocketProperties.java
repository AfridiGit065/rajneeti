package com.rajneeti.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Strongly-typed WebSocket configuration properties.
 *
 * <p>Bound from {@code application.yml} under the {@code websocket} prefix.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "websocket")
public class WebSocketProperties {

    /** Comma-separated list of allowed origins for WebSocket handshake. */
    private List<String> allowedOrigins = List.of("http://localhost:3000");

    /** SockJS / STOMP endpoint path (e.g., /ws). */
    private String endpoint = "/ws";

    /** STOMP application destination prefix (e.g., /app). */
    private String appDestinationPrefix = "/app";

    /** STOMP topic prefix for broadcast messages. */
    private String topicPrefix = "/topic";

    /** STOMP user-specific queue prefix. */
    private String queuePrefix = "/user";
}
