package com.rajneeti.bot;

/**
 * Behaviour profile of an AI bot. Each personality scales the heuristic
 * fallback's appetite for risk: how willingly the bot challenges claims, bluffs
 * blocks, and invests in expensive (steal / assassinate / coup) actions.
 */
public enum BotPersonality {

    /** Even-handed mix of safe and risky lines. */
    BALANCED(1.0, 1.0, 1.0),

    /** Challenges and attacks more, blocks aggressively. */
    AGGRESSIVE(1.2, 1.2, 1.2),

    /** Prefers safe income/tax/exchange, rarely bluffs. */
    CAUTIOUS(0.6, 0.7, 0.8),

    /** Bluffs claims and blocks often; uses the least information. */
    BLUFFER(0.9, 1.3, 1.15),

    /** Hunts the richest target; flexible but economically driven. */
    OPPORTUNIST(1.0, 1.1, 1.1);

    /** Multiplier for the base challenge probability. */
    private final double challengeFactor;

    /** Multiplier for bluffing (false claims and false blocks). */
    private final double bluffFactor;

    /** Multiplier for attacking (steal / assassinate / coup). */
    private final double aggressionFactor;

    BotPersonality(double challengeFactor, double bluffFactor, double aggressionFactor) {
        this.challengeFactor = challengeFactor;
        this.bluffFactor = bluffFactor;
        this.aggressionFactor = aggressionFactor;
    }

    public double getChallengeFactor() {
        return challengeFactor;
    }

    public double getBluffFactor() {
        return bluffFactor;
    }

    public double getAggressionFactor() {
        return aggressionFactor;
    }

    /** Parses a stored string, defaulting to {@link #BALANCED} on null/unknown. */
    public static BotPersonality fromNullable(String value) {
        if (value == null || value.isBlank()) {
            return BALANCED;
        }
        try {
            return valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return BALANCED;
        }
    }
}