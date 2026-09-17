package com.rajneeti.bot.decision;

/**
 * Common contract for the two bot brains: the LLM provider and the heuristic
 * fallback. Both produce a {@link BotDecision} for a given context, so the
 * driver can swap them transparently.
 */
public interface BotDecisionProvider {

    /** Returns the decision for the given window context. Never returns null. */
    BotDecision decide(BotDecisionContext context);
}