package com.rajneeti.bot;

/**
 * Skill level of an AI bot. Drives both the "thinking" delay (how long the bot
 * appears to ponder before acting) and the aggressiveness of the heuristic
 * fallback when no LLM decision is available.
 */
public enum BotDifficulty {

    /** Slow, cautious, mostly safe actions. */
    EASY(500, 1200),

    /** Balanced speed and risk appetite. */
    MEDIUM(800, 1800),

    /** Fast, aggressive, higher bluff/challenge rates. */
    HARD(1000, 2500);

    private final int minThinkDelayMs;
    private final int maxThinkDelayMs;

    BotDifficulty(int minThinkDelayMs, int maxThinkDelayMs) {
        this.minThinkDelayMs = minThinkDelayMs;
        this.maxThinkDelayMs = maxThinkDelayMs;
    }

    public int getMinThinkDelayMs() {
        return minThinkDelayMs;
    }

    public int getMaxThinkDelayMs() {
        return maxThinkDelayMs;
    }

    /** Parses a stored string, defaulting to {@link #MEDIUM} on null/unknown. */
    public static BotDifficulty fromNullable(String value) {
        if (value == null || value.isBlank()) {
            return MEDIUM;
        }
        try {
            return valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return MEDIUM;
        }
    }
}