package com.rajneeti.bot.decision;

import java.util.List;
import java.util.UUID;

/**
 * A single executable decision produced by a bot's brain (LLM or fallback)
 * for a specific decision window. The {@link com.rajneeti.bot.executor.BotActionExecutor}
 * validates it against the live game state before pushing it through the real
 * engine seams.
 *
 * @param decisionType    the window this decision answers
 * @param action          the executable intent (never null)
 * @param targetPlayerId  target for STEAL / ASSASSINATE / COUP (must be an opponent)
 * @param claimedCharacter the blocking character id for BLOCK (lower-case, e.g. "minister")
 * @param keepCardIds     the exactly-2 cards to keep for CONFIRM_EXCHANGE
 * @param loserCardId     the card this bot stakes/loses for CHALLENGE / CHALLENGE_BLOCK
 * @param reason          human-readable rationale (metrics/diagnostics only)
 */
public record BotDecision(
        BotDecisionType decisionType,
        BotActionType action,
        UUID targetPlayerId,
        String claimedCharacter,
        List<UUID> keepCardIds,
        UUID loserCardId,
        String reason) {

    public BotDecision {
        if (action == null) {
            action = BotActionType.PASS;
        }
    }

    /** Convenience factory for a deliberate no-op (e.g. declining to challenge). */
    public static BotDecision pass(BotDecisionType type) {
        return new BotDecision(type, BotActionType.PASS, null, null, null, null, "no action taken");
    }

    /** Convenience factory for the actor-resolve windows. */
    public static BotDecision resolve(BotDecisionType type) {
        return new BotDecision(type, BotActionType.RESOLVE, null, null, null, null, "closing the action window");
    }
}