package com.rajneeti.dto.room;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request payload for joining an existing game room via room code.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JoinRoomRequest {

    @NotBlank(message = "Room code is required")
    @Size(min = 4, max = 10, message = "Room code must be between 4 and 10 characters")
    private String roomCode;
}