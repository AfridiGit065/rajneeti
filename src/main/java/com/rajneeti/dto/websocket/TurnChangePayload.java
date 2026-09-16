package com.rajneeti.dto.websocket;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Payload for turn transitions (first turn assignment and turn advance).
 * {@code previousTurnPlayerId} is null when the very first turn is assigned.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TurnChangePayload {

    private UUID previousTurnPlayerId;
    private UUID currentTurnPlayerId;
    private Integer turnNumber;
}