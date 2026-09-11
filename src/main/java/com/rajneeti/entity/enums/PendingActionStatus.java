package com.rajneeti.entity.enums;

/**
 * Lifecycle state of a pending match action.
 */
public enum PendingActionStatus {
    /**
     * The action has been claimed and is now awaiting the challenge window.
     * Coins or other effects are NOT applied while in this state.
     */
    AWAITING_CHALLENGE,

    /**
     * The action was resolved (challenge concluded without a challenge, or
     * a successful claim). Effects may have been applied.
     */
    RESOLVED
}