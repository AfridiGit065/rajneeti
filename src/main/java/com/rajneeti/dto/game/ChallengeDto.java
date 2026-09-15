package com.rajneeti.dto.game;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Module 18 — player-safe projection of a resolved challenge.
 *
 * <p>Exposed through {@link GameStateResponse#lastChallenge} so clients can
 * display the verdict without ever seeing hidden cards. All fields are part of
 * the public record once a challenge resolves (the revealed card is revealed to
 * everybody); the replacement card's character is never included.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChallengeDto {

    private UUID challengerId;

    private UUID claimantId;

    private String actionType;

    /** Lower-case character id the claimant asserted, e.g. "minister". */
    private String claimedCharacter;

    /** {@code "CLAIM_TRUE"} when the claimant owned the card, {@code "CLAIM_FALSE"} when bluffing. */
    private String result;

    /** Physical card id revealed face-up during the challenge. */
    private UUID revealedCardId;

    /** Lower-case character id of the revealed card. */
    private String revealedCharacterId;

    private UUID influenceLostById;

    /** Whether the original action continues after a truthful claim. */
    private boolean actionContinues;
}