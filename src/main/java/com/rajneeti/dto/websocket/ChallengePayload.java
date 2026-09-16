package com.rajneeti.dto.websocket;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Payload for a resolved challenge (action claim or block claim).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChallengePayload {

    private UUID challengerUserId;
    private String challengerUsername;
    private UUID claimantUserId;
    private String claimantUsername;
    private String actionType;
    private String claimedCharacter;
    private Boolean claimTrue;
    private Boolean actionContinues;
    private Boolean blockClaim;
}