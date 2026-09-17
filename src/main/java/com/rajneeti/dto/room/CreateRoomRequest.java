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

    /** Module 25 — how many AI bots to seat (host + bots must not exceed maxPlayers). */
    @Min(value = 0, message = "Minimum bot count is 0")
    @Max(value = 5, message = "Maximum bot count is 5")
    @Builder.Default
    private Integer botCount = 0;

    /** Module 25 — difficulty of the seeded bots (EASY / MEDIUM / HARD). */
    @Builder.Default
    private String botDifficulty = "MEDIUM";

    /** Module 25 — personality of the seeded bots (BALANCED / AGGRESSIVE / ...). */
    @Builder.Default
    private String botPersonality = "BALANCED";
}