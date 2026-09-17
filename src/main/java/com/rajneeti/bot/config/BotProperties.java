package com.rajneeti.bot.config;

import com.rajneeti.bot.BotDifficulty;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Strongly-typed configuration for the bot pipeline (Module 25).
 *
 * <p>Values are bound from {@code application.yml} under the {@code bot} prefix
 * (which in turn reads the {@code BOT_*} environment variables).
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "bot")
public class BotProperties {

    /** When true, decisions are requested from the LLM provider. */
    private boolean useLlm = true;

    /**
     * When true, bots wait a small human-like "thinking" delay before acting.
     * Disabled in tests so simulations run instantly.
     */
    private boolean thinkDelayEnabled = true;

    /** Fallback think-delay floor (ms) when a bot has no difficulty. */
    private long minThinkDelayMs = 800;

    /** Fallback think-delay ceiling (ms) when a bot has no difficulty. */
    private long maxThinkDelayMs = 2000;

    /**
     * How long (ms) a bot actor waits after opening an action window before
     * resolving it, giving opponents a real chance to challenge or block.
     */
    private long windowGraceMs = 8000;

    /** Poll interval (ms) of the scheduler that drives registered matches. */
    private long schedulerIntervalMs = 500;

    /**
     * Returns the effective think-delay range for the given difficulty. Falls
     * back to the configured defaults when the difficulty is unknown.
     */
    public long minDelayFor(BotDifficulty difficulty) {
        return difficulty != null ? difficulty.getMinThinkDelayMs() : minThinkDelayMs;
    }

    public long maxDelayFor(BotDifficulty difficulty) {
        return difficulty != null ? difficulty.getMaxThinkDelayMs() : maxThinkDelayMs;
    }
}