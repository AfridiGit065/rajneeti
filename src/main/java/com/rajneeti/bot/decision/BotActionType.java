package com.rajneeti.bot.decision;

/**
 * The executable intent a bot returns from a decision window. Mirrors the
 * real engine seams: {@code INCOME..COUP} map to {@code GameEngine.perform*},
 * {@code CHALLENGE}/{@code CHALLENGE_BLOCK} to {@code ChallengeManager.challenge},
 * {@code BLOCK} to {@code BlockManager.block}, {@code RESOLVE} to
 * {@code ActionResolver.resolve} and {@code CONFIRM_EXCHANGE} to
 * {@code GameEngine.confirmExchange()}.
 */
public enum BotActionType {

    INCOME,
    FOREIGN_AID,
    TAX,
    STEAL,
    ASSASSINATE,
    EXCHANGE,
    COUP,
    CHALLENGE,
    CHALLENGE_BLOCK,
    BLOCK,
    RESOLVE,
    CONFIRM_EXCHANGE,
    PASS
}