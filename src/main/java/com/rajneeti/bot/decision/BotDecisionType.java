package com.rajneeti.bot.decision;

/**
 * The window a bot is currently deciding for. Each map to a well-defined slice
 * of the turn lifecycle:
 *
 * <ul>
 *   <li>{@code OWN_ACTION} — bot's own turn, no pending action; pick an action.</li>
 *   <li>{@code ACTION_CHALLENGE} — an opponent claimed a character; decide to challenge or pass.</li>
 *   <li>{@code ACTION_BLOCK} — an opponent declared a blockable action; decide to block (with a character) or pass.</li>
 *   <li>{@code BLOCK_CHALLENGE} — a block claim stands and is open to challenge; decide to challenge the block or pass.</li>
 *   <li>{@code ACTOR_RESOLVE} — the bot is the actor of a pending (non-exchange) action; resolve it after the grace window.</li>
 *   <li>{@code EXCHANGE_CONFIRM} — the bot declared an Exchange and must keep exactly 2 cards.</li>
 * </ul>
 */
public enum BotDecisionType {

    OWN_ACTION,
    ACTION_CHALLENGE,
    ACTION_BLOCK,
    BLOCK_CHALLENGE,
    ACTOR_RESOLVE,
    EXCHANGE_CONFIRM
}