package com.rajneeti.dto.room;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request payload for creating a new game room lobby.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateRoomRequest {

    @Min(value = 2, message = "Minimum players in a room is 2")
    @Max(value = 6, message = "Maximum players in a room is 6")
    @Builder.Default
    private Integer maxPlayers = 6;
}