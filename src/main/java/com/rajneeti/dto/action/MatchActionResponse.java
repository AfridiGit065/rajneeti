package com.rajneeti.dto.action;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.rajneeti.entity.enums.CharacterType;
import com.rajneeti.entity.enums.MatchActionType;
import com.rajneeti.entity.enums.PendingActionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response describing an accepted gameplay action that is now pending resolution.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MatchActionResponse {

    private UUID matchId;
    private UUID roomId;
    private MatchActionType action;
    private CharacterType claimedCharacter;
    private UUID actorUserId;
    private String actorUsername;
    private UUID targetUserId;
    private PendingActionStatus status;
    private Integer coinsToAward;
    private UUID currentTurnPlayerId;
    private Integer turnNumber;
    private LocalDateTime createdAt;
}