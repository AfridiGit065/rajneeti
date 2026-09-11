package com.rajneeti.game;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
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

    /**
     * The character claimed by the action, e.g. {@code "amla"} for an Exchange.
     * Null for actions that do not require a claim.
     */
    private final String claimedCharacter;

    /**
     * The temporary private card pool for an Exchange (existing hand + drawn
     * cards). Only ever serialised to the action actor; never to opponents.
     */
    private final List<GameCard> exchangePool;

    /**
     * The actor's hand card IDs before an Exchange replaced it, so a later
     * challenge resolution can restore the original hand.
     */
    private final List<UUID> originalHandCardIds;

    /**
     * The target player's user ID for single-target actions such as an
     * Assassination. Public information once the action is pending;
     * {@code null} for actions without a target.
     */
    private final UUID targetPlayerId;

    /**
     * The coins reserved from the actor when the action was declared. Deducted
     * from the actor only when the action succeeds; returned to the actor when
     * it is cancelled or fails. {@code null} for actions with no reserved cost.
     */
    private final Integer reservedCoins;
}
