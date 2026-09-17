package com.rajneeti.bot.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Strongly-typed configuration for the LLM provider behind AI bots.
 *
 * <p>Values are bound from {@code application.yml} under the {@code ai} prefix
 * (which in turn reads the {@code AI_*} environment variables). Up to three API
 * keys are supported so the {@link com.rajneeti.bot.ai.AiKeyManager} can rotate
 * between them and fail over when one is rate-limited or revoked. Keys are
 * always read from the environment – never hardcoded.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "ai")
public class AiProperties {

    /** Provider name, e.g. {@code openai}. Only used for logging/metrics. */
    private String provider = "openai";

    /** Base URL of the OpenAI-compatible chat-completions endpoint. */
    private String baseUrl = "https://api.openai.com/v1";

    /** Model identifier, e.g. {@code gpt-4o-mini}. */
    private String model = "gpt-4o-mini";

    /** First API key (optional). */
    private String apiKey1;

    /** Second API key (optional) – used when rotation is enabled. */
    private String apiKey2;

    /** Third API key (optional) – used when rotation is enabled. */
    private String apiKey3;

    /** Whether to rotate through multiple keys and cool down failed ones. */
    private boolean useKeyRotation = true;

    /** Seconds a key stays in cooldown after a non-auth (e.g. rate-limit) failure. */
    private long keyCooldownSeconds = 60;

    /** Maximum acquire+retry attempts before a decision falls back to heuristics. */
    private int maxKeyRetries = 3;

    /** Per-request HTTP timeout in milliseconds. */
    private long requestTimeoutMs = 5000;

    /**
     * Returns the configured, non-blank API keys in declaration order. Bots
     * whose provider is unreachable or unconfigured transparently use the
     * heuristic fallback strategy, so a missing key never breaks a game.
     */
    public List<String> getApiKeys() {
        List<String> keys = new ArrayList<>(3);
        if (apiKey1 != null && !apiKey1.isBlank()) {
            keys.add(apiKey1.trim());
        }
        if (apiKey2 != null && !apiKey2.isBlank()) {
            keys.add(apiKey2.trim());
        }
        if (apiKey3 != null && !apiKey3.isBlank()) {
            keys.add(apiKey3.trim());
        }
        return keys;
    }
}