package com.rajneeti.dto.turn;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Read-only DTO representing the current turn state of a match.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TurnInfo {

    private UUID matchId;
    private UUID currentTurnPlayerId;
    private Integer turnNumber;
    private List<UUID> turnOrder;
    private Long activePlayerCount;
}
