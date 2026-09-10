package com.rajneeti.dto.game;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
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
}
