package com.rajneeti.websocket;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.security.Principal;
import java.util.UUID;

/**
 * STOMP session principal derived from a valid JWT connection.
 *
 * <p>{@link #getName()} returns the user UUID so {@code /user/{id}/queue/*}
 * destinations and {@code convertAndSendToUser} resolve by userId, and so
 * {@code @MessageMapping} handlers can read the identity from
 * {@code Principal.getName()} with zero trust in client-supplied fields.
 */
@RequiredArgsConstructor
public class WebSocketPrincipal implements Principal {

    @Getter
    private final UUID userId;

    @Getter
    private final String username;

    @Override
    public String getName() {
        return userId.toString();
    }
}