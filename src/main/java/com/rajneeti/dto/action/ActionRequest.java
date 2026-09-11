package com.rajneeti.dto.action;

import com.rajneeti.entity.enums.CharacterType;
import com.rajneeti.entity.enums.MatchActionType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Request to perform a gameplay action during the caller's turn.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActionRequest {

    @NotNull(message = "Action type is required")
    private MatchActionType action;

    /**
     * The character claimed while performing the action, if any. Optional for
     * callers without the actual influence card — this is a bluff game.
     */
    private CharacterType claimedCharacter;

    /** Target player, when the action targets another player. */
    private UUID targetPlayerId;
}