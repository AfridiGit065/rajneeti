package com.rajneeti.dto.websocket;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Payload for the client-initiated resync command
 * {@code /app/matches/{matchId}/sync} (Module 23).
 *
 * <p>The client sends the highest state version it currently holds together
 * with a monotonically increasing {@code requestId} (per user). The server
 * replies with a {@link WebSocketEventType#PRIVATE_STATE} snapshot carrying the
 * current authoritative version. {@code requestId} lets the server de-duplicate
 * duplicate/delayed retries so every unique request is answered exactly once.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchSyncRequest {

    /** Client-generated unique id (UUID, or an ever increasing counter). */
    private String requestId;

    /**
     * The highest {@code stateVersion} the client currently holds. Informational
     * (the server always answers with the authoritative version), used for
     * diagnostic logging.
     */
    private Long version;
}