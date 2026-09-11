package com.rajneeti.dto.game;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * DTO for a pending action awaiting block-window resolution.
 * Exposed to the frontend so it can display the pending state.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PendingActionDto {

    /** Action type, e.g. {@code "FOREIGN_AID"}. */
    private String type;

    /** The actor's user ID. */
    private UUID actorUserId;

    /** When the block window opened. */
    private LocalDateTime startedAt;

    /** The claimed character, e.g. {@code "amla"} for an Exchange. */
    private String claimedCharacter;

    /**
     * The temporary exchange card pool. Only populated for the action's own
     * actor; always {@code null} (and absent from JSON) for opponents.
     */
    private List<GameCardDto> exchangePool;

    /**
     * The target player's user ID for single-target actions such as an
     * Assassination. Public information; {@code null} otherwise.
     */
    private UUID targetPlayerId;

    /** Module 18 — user ID of the opponent who challenged a truthful claim. */
    private UUID challengerUserId;
}
