package com.rajneeti.game;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

/**
 * Module 18 — record of a challenge that was resolved for a pending action.
 *
 * <p>Stored on the in-memory {@link GameState} so the frontend can display the
 * authoritative verdict (who lost influence, which card was revealed, whether
 * the original action continues). Only public information is carried here; the
 * replacement card's character is deliberately absent.
 */
@Getter
@Builder
public class GameChallenge {

    /** The player who raised the challenge. */
    private final UUID challengerUserId;

    /** The player whose character claim was challenged (the action's actor). */
    private final UUID claimantUserId;

    /** The pending action type, e.g. {@code "TAX"}. */
    private final String actionType;

    /** The character the claimant asserted, e.g. {@code "minister"}. */
    private final String claimedCharacter;

    /** {@code true} when the claimant really owned the claimed character. */
    private final boolean claimTrue;

    /** The player who lost exactly one influence because of the challenge. */
    private final UUID influenceLostById;

    /** The physical card that was revealed face-up (public after resolution). */
    private final UUID revealedCardId;

    /** Lower-case character id of the revealed card (public). */
    private final String revealedCharacterId;

    /**
     * {@code true} when the claim was truthful and the original action proceeds;
     * {@code false} when the bluff was exposed and the action is cancelled.
     */
    private final boolean actionContinues;
}