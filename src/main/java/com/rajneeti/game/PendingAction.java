package com.rajneeti.game;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * In-memory representation of an action that is currently awaiting resolution
 * (e.g. a block window). Persisted only in the in-memory {@link GameStore};
 * never written to MySQL.
 */
@Getter
@Builder
public class PendingAction {

    /** Action type identifier, e.g. {@code "FOREIGN_AID"}. */
    private final String type;

    /** The user who declared the action. */
    private final UUID actorUserId;

    /** When the block window was opened. */
    private final LocalDateTime startedAt;
}
