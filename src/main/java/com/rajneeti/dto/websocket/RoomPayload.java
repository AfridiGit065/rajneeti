package com.rajneeti.dto.websocket;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Payload for room membership events (JOIN_ROOM, LEAVE_ROOM, READY, UNREADY,
 * HOST_CHANGED, ROOM_CANCELLED, ROOM_CREATED). Contains only public room data;
 * the new host fields are set for host-transfer scenarios.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RoomPayload {

    private UUID playerId;
    private String username;
    private Integer seatNumber;
    private Boolean ready;
    private Boolean host;
    private Integer playerCount;
    private UUID newHostId;
    private String newHostUsername;
}