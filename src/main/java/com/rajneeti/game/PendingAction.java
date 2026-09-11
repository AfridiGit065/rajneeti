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

    /**
     * Module 18 — the user ID of the opponent who successfully challenged
     * this pending action while the claim was truthful. Once set, any second
     * challenge against the same pending action is rejected.
     */
    private final UUID challengerUserId;

    /**
     * Module 19 — the user ID of the player who submitted a block claim
     * against this pending action. {@code null} while no block is in play.
     * The blocker does NOT need to own the claimed character (a bluff is
     * allowed); a later block challenge decides the truth.
     */
    private final UUID blockerUserId;

    /**
     * Module 19 — the lower-case character id the blocker asserted to stop
     * this action, e.g. {@code "minister"} for a Foreign Aid block.
     * {@code null} until a block is submitted.
     */
    private final String blockedCharacter;

    /**
     * Module 19 — the user ID of the opponent who challenged the pending
     * block claim. Once set, any second block challenge against the same
     * block is rejected.
     */
    private final UUID blockChallengerUserId;
}
