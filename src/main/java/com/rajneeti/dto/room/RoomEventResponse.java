package com.rajneeti.dto.room;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * WebSocket broadcast event payload for room updates.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RoomEventResponse {

    private String event;
    private UUID roomId;
    private String roomCode;
    private RoomPlayerResponse player;
    private UUID playerId;
    private String username;
    private String message;

    @Builder.Default
    private Instant timestamp = Instant.now();
}