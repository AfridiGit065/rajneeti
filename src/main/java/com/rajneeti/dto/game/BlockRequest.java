package com.rajneeti.dto.game;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Module 19 — request body for submitting a block claim: the blocker names the
 * character they assert to stop the pending action. The engine validates the
 * rest (action blockable, character valid for the action, blocker eligibility).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlockRequest {

    /** Lower-case character id the blocker claims, e.g. {@code "minister"}. */
    private String claimedCharacter;
}