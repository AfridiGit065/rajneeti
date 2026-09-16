package com.rajneeti.websocket;

import com.rajneeti.repository.MatchPlayerRepository;
import com.rajneeti.repository.RoomPlayerRepository;
import com.rajneeti.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.UUID;

/**
 * STOMP channel interceptor that authenticates every connection and guards
 * every subscription.
 *
 * <p>Authentication is fully server-side: a CONNECT frame must carry a valid
 * JWT (the same token the REST APIs use) in its {@code Authorization} header.
 * The resolved {@link WebSocketPrincipal} is attached to the session so
 * {@code /user/**} routing and {@code Principal} injection work afterwards.
 * No identity field from the client payload is ever trusted.
 *
 * <p>Subscription policy:
 * <ul>
 *   <li>{@code /topic/lobby} — any authenticated user;</li>
 *   <li>{@code /user/**} — the user's own private queue;</li>
 *   <li>{@code /topic/rooms/{roomId}} — room members only;</li>
 *   <li>{@code /topic/matches/{matchId}} — match players only;</li>
 *   <li>anything else — rejected.</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider       jwtTokenProvider;
    private final RoomPlayerRepository   roomPlayerRepository;
    private final MatchPlayerRepository  matchPlayerRepository;

    @Override
    @SuppressWarnings("NullableProblems")
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        /*
         * Use the accessor Spring already attached to the message instead of
         * StompHeaderAccessor.wrap(message): wrap() always constructs a fresh
         * accessor in Spring Framework 6.1, so mutating it would not fire the
         * user-change callback StompSubProtocolHandler registers on its own
         * instance. That callback is what propagates the authenticated
         * principal to subsequent SUBSCRIBE/SEND frames of the session.
         */
        StompHeaderAccessor accessor = messageAccessor(message);
        StompCommand command = accessor.getCommand();

        if (StompCommand.CONNECT.equals(command) || StompCommand.STOMP.equals(command)) {
            accessor.setUser(authenticate(accessor));
            return message;
        }

        WebSocketPrincipal principal = currentPrincipal(accessor);

        if (StompCommand.SUBSCRIBE.equals(command)) {
            String destination = accessor.getDestination();
            if (!isAllowedSubscription(principal, destination)) {
                throw new MessageDeliveryException("Not authorized to subscribe to '" + destination + "'.");
            }
            return message;
        }

        if (StompCommand.SEND.equals(command)) {
            if (principal == null) {
                throw new MessageDeliveryException("A valid authentication token is required to send messages.");
            }
            return message;
        }

        return message;
    }

    private StompHeaderAccessor messageAccessor(Message<?> message) {
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        return accessor != null ? accessor : StompHeaderAccessor.wrap(message);
    }

    /**
     * Validates the JWT carried by the CONNECT frame. A missing, malformed or
     * expired token rejects the connection with an ERROR frame.
     */
    private WebSocketPrincipal authenticate(StompHeaderAccessor accessor) {
        String token = extractToken(accessor);

        if (!StringUtils.hasText(token) || !jwtTokenProvider.validateToken(token)) {
            log.warn("WebSocket CONNECT rejected: missing or invalid JWT");
            throw new MessageDeliveryException("UNAUTHORIZED: a valid Bearer token is required to connect.");
        }

        try {
            UUID userId = jwtTokenProvider.extractUserId(token);
            String username = jwtTokenProvider.extractSubject(token);
            if (userId == null || !StringUtils.hasText(username)) {
                throw new IllegalArgumentException("JWT is missing identity claims");
            }
            return new WebSocketPrincipal(userId, username);
        } catch (Exception ex) {
            log.warn("WebSocket CONNECT rejected: {}", ex.getMessage());
            throw new MessageDeliveryException("UNAUTHORIZED: the provided token is invalid.");
        }
    }

    private WebSocketPrincipal currentPrincipal(StompHeaderAccessor accessor) {
        if (accessor.getUser() instanceof WebSocketPrincipal principal) {
            return principal;
        }
        if (accessor.getUser() != null) {
            String name = accessor.getUser().getName();
            if (name != null) {
                try {
                    return new WebSocketPrincipal(UUID.fromString(name), name);
                } catch (IllegalArgumentException ignored) {
                    // fall through
                }
            }
        }
        return null;
    }

    private boolean isAllowedSubscription(WebSocketPrincipal principal, String destination) {
        if (principal == null || !StringUtils.hasText(destination)) {
            return false;
        }

        if (destination.equals("/topic/lobby")) {
            return true;
        }
        if (destination.startsWith("/user/")) {
            // Spring only routes a user's own private queue to that user's session;
            // the interceptor simply confirms a valid principal is present.
            return true;
        }
        if (destination.startsWith("/topic/rooms/")) {
            return isRoomMember(principal.getUserId(), destination.substring("/topic/rooms/".length()));
        }
        if (destination.startsWith("/topic/matches/")) {
            return isMatchPlayer(principal.getUserId(), destination.substring("/topic/matches/".length()));
        }
        return false;
    }

    private boolean isRoomMember(UUID userId, String rawRoomId) {
        UUID roomId = parseUuid(rawRoomId);
        return roomId != null && roomPlayerRepository.existsByRoomIdAndUserId(roomId, userId);
    }

    private boolean isMatchPlayer(UUID userId, String rawMatchId) {
        UUID matchId = parseUuid(rawMatchId);
        if (matchId == null) {
            return false;
        }
        try {
            return matchPlayerRepository.findByMatchIdAndUserId(matchId, userId).isPresent();
        } catch (Exception ex) {
            return false;
        }
    }

    private UUID parseUuid(String raw) {
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException | NullPointerException ex) {
            return null;
        }
    }

    private String extractToken(StompHeaderAccessor accessor) {
        if (accessor.getNativeHeader(AUTHORIZATION_HEADER) != null) {
            List<String> values = accessor.getNativeHeader(AUTHORIZATION_HEADER);
            if (!values.isEmpty() && StringUtils.hasText(values.get(0))) {
                return stripBearer(values.get(0));
            }
        }
        if (StringUtils.hasText(accessor.getFirstNativeHeader("authorization"))) {
            return stripBearer(accessor.getFirstNativeHeader("authorization"));
        }
        return null;
    }

    private String stripBearer(String header) {
        String trimmed = header.trim();
        return trimmed.startsWith(BEARER_PREFIX) ? trimmed.substring(BEARER_PREFIX.length()) : trimmed;
    }
}