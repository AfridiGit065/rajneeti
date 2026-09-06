package com.rajneeti.dto.room;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Details of a player seated inside a room.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RoomPlayerResponse {

    private UUID id;
    private String username;
    private String avatarUrl;
    private Integer seatNumber;
    private Boolean ready;
    private Boolean isHost;
    private LocalDateTime joinedAt;
}