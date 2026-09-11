package com.rajneeti.entity;

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
 * Represents a gameplay action that has been claimed but not yet resolved.
 *
 * <p>This is an embedded value stored as JSON on {@link Match#getPendingAction()}.
 * When an action is claimed (e.g. Tax claims the Minister), the action enters
 * this pending state and awaits the challenge window. Coin rewards are applied
 * only when the action is finally resolved.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PendingAction {

    private MatchActionType actionType;

    /** The character claimed while performing the action, if any. */
    private CharacterType claimedCharacter;

    private UUID actorUserId;

    /** The player targeted by the action, if any. */
    private UUID targetUserId;

    @Builder.Default
    private PendingActionStatus status = PendingActionStatus.AWAITING_CHALLENGE;

    /** Coins to be awarded once the action resolves successfully. */
    private Integer coinsToAward;

    private LocalDateTime createdAt;
}