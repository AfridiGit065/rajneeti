package com.rajneeti.dto.room;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.rajneeti.entity.enums.RoomStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Complete details of a room lobby and its current participants.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RoomResponse {

    private UUID id;
    private String roomCode;
    private UUID hostId;
    private String hostUsername;
    private RoomStatus status;
    private Integer maxPlayers;
    private Integer currentPlayers;
    private Boolean canStart;
    private List<RoomPlayerResponse> players;
    private LocalDateTime createdAt;
}