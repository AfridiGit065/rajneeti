package com.rajneeti.dto.websocket;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Payload for a recorded block claim against a pending action.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BlockPayload {

    private UUID blockerUserId;
    private String blockerUsername;
    private UUID actorUserId;
    private String actionType;
    private String blockedCharacter;
}