package com.rajneeti.dto.websocket;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Payload for gameplay actions performed by a player.
 *
 * <p>{@code outcome} is one of the stable string values the game layer emits:
 * {@code PENDING} (a block/challenge window opened), {@code RESOLVED} (the
 * effect was applied) or {@code CANCELLED} (blocked / bluffed). Hidden card
 * content is never included — clients re-fetch the player-safe state via REST.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GameActionPayload {

    private UUID actorUserId;
    private String actorUsername;
    private String actionType;
    private UUID targetUserId;
    private String outcome;
}