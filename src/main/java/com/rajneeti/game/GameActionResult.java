package com.rajneeti.game;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

/**
 * Module 20 — record of the final result of a pending action once the
 * Action Resolver closed it.
 *
 * <p>Computed server-side by {@link ActionResolver} from the pending action's
 * recorded challenge/block outcomes and the applied final mutation. Stored on
 * the in-memory {@link GameState} (like {@link GameChallenge}) so every viewer
 * can display the same authoritative result until the next action begins:
 * what was resolved/cancelled, how many coins moved in each direction, who (if
 * anyone) lost an influence card, which block/challenge outcome decided it and
 * whose turn is next.
 */
@Getter
@Builder
public class GameActionResult {

    /** The action's final lifecycle state: fully executed. */
    public static final String RESULT_RESOLVED = "RESOLVED";

    /** The action's final lifecycle state: terminated without effect (blocked). */
    public static final String RESULT_CANCELLED = "CANCELLED";

    /** Action type identifier, e.g. {@code "STEAL"}. */
    private final String actionType;

    /** Final lifecycle state: {@code "RESOLVED"} or {@code "CANCELLED"}. */
    private final String result;

    /** The action's actor (the player whose action was resolved). */
    private final UUID actorUserId;

    /**
     * Coin delta applied to the actor. Positive for rewards (income, foreign
     * aid, tax, steal), negative for costs (assassination, coup), zero for a
     * cancelled action.
     */
    private final int coinsGained;

    /**
     * Coin delta applied to the target player. Negaive for a successful Steal
     * (money leaves the target); zero otherwise.
     */
    private final int coinsLost;

    /** Non-null only when the action was terminated by a standing block. */
    private final UUID blockedByUserId;

    /** Lower-case character id the blocker asserted, e.g. {@code "minister"}. */
    private final String blockedCharacter;

    /**
     * Module 18 — {@code true} when the action's character claim survived a
     * successfully-challenged (truthful) claim before resolution.
     */
    private final boolean claimChallenged;

    /** The player who lost exactly one influence card through the action. */
    private final UUID influenceLostById;

    /** {@code true} when the influence loss eliminated the affected player. */
    private final boolean eliminated;

    /** The next turn holder after the action was finally resolved/cancelled. */
    private final UUID nextTurnPlayerId;

    /** The next turn number after the action was finally resolved/cancelled. */
    private final int nextTurnNumber;
}