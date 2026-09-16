package com.rajneeti.dto.websocket;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Payload for the START_GAME broadcast (room + match topics).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MatchStartedPayload {

    private UUID matchId;
    private UUID roomId;
    private UUID hostUserId;
    private Integer playerCount;
}