package com.rajneeti.dto.game;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Module 20 — player-safe representation of a final action result, produced by
 * the {@link com.rajneeti.game.ActionResolver} after a pending action has been
 * closed. Exposed on {@link GameStateResponse} until the next action is
 * declared.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActionResultDto {

    /** Action type identifier, e.g. {@code "STEAL"}. */
    private String actionType;

    /** {@code "RESOLVED"} when the action's effect went through, {@code "CANCELLED"} otherwise. */
    private String result;

    /** The acting player's user ID. */
    private UUID actorUserId;

    /**
     * Coin delta applied to the actor. Positive for a reward (income, foreign
     * aid, tax, steal gain), negative for a cost (assassination, coup).
     */
    private int coinsGained;

    /** Coin delta applied to the target. Negative when coins were stolen; zero otherwise. */
    private int coinsLost;

    /** Non-null when a standing block terminated the action. */
    private UUID blockedByUserId;

    /** Lower-case character id the blocker asserted. */
    private String blockedCharacter;

    /** {@code true} when the action survived a successful challenge before resolution. */
    private boolean claimChallenged;

    /** The player who lost one influence card. */
    private UUID influenceLostById;

    /** {@code true} when the influence loss eliminated the affected player. */
    private boolean eliminated;

    /** The next turn holder after the action was resolved/cancelled. */
    private UUID nextTurnPlayerId;

    /** The next turn number after the action was resolved/cancelled. */
    private int nextTurnNumber;
}