package com.rajneeti.bot.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Module 25 — AI API key manager.
 *
 * <p>Tracks up to three provider API keys and makes the LLM path resilient:
 * <ul>
 *   <li><b>Rotation</b> — when {@code ai.use-key-rotation} is enabled the next
 *       healthy key is picked round-robin, spreading load across keys.</li>
 *   <li><b>Cooldown</b> — after a transient failure (rate limit, 5xx, timeout)
 *       a key is parked for {@code ai.key-cooldown-seconds} before being tried
 *       again, so a bad key cannot hammer the provider.</li>
 *   <li><b>Invalid keys</b> — after an auth failure (401/403) a key is marked
 *       permanently invalid and never used again for the process lifetime.</li>
 *   <li><b>No keys</b> — when no key is configured or every key is unavailable,
 *       {@link #acquire()} returns empty and the caller transparently falls back
 *       to the heuristic strategy. A missing key never breaks a game.</li>
 * </ul>
 *
 * <p>State transitions are governed by the caller: a successful request calls
 * {@link #reportSuccess(int)}, every other outcome calls
 * {@link #reportFailure(int, boolean)}.
 */
@Slf4j
@Component
public class AiKeyManager {

    private enum Status { ACTIVE, COOLDOWN, INVALID }

    private static final class KeyHandle {
        volatile Status status = Status.ACTIVE;
        volatile long cooldownUntilMs = 0;
    }

    private final List<String> keys;
    private final List<KeyHandle> handles;
    private final boolean rotationEnabled;
    private final long cooldownMs;
    private final AtomicInteger cursor = new AtomicInteger(0);
    private final AtomicLong nextLog = new AtomicLong(0);

    public AiKeyManager(com.rajneeti.bot.config.AiProperties properties) {
        this.keys = properties.getApiKeys();
        this.rotationEnabled = properties.isUseKeyRotation();
        this.cooldownMs = properties.getKeyCooldownSeconds() * 1000L;
        this.handles = keys.stream().map(k -> new KeyHandle()).toList();
    }

    /**
     * Tracks whether at least one usable key exists.
     */
    public boolean hasUsableKey() {
        long now = System.currentTimeMillis();
        for (KeyHandle handle : handles) {
            if (handle.status == Status.ACTIVE) {
                return true;
            }
            if (handle.status == Status.COOLDOWN && now >= handle.cooldownUntilMs) {
                return true;
            }
        }
        return false;
    }

    /** Total number of configured (non-blank) keys. */
    public int configuredKeyCount() {
        return keys.size();
    }

    /**
     * Returns the index of the next usable key, or {@code -1} when no key is
     * available right now. When rotation is disabled only key 0 is ever returned
     * (it may still be in cooldown).
     */
    public int acquire() {
        long now = System.currentTimeMillis();
        if (keys.isEmpty()) {
            return -1;
        }

        synchronized (this) {
            for (int attempt = 0; attempt < keys.size(); attempt++) {
                int index = rotationEnabled
                        ? Math.floorMod(cursor.getAndIncrement(), keys.size())
                        : 0;
                KeyHandle handle = handles.get(index);
                if (handle.status == Status.INVALID) {
                    continue;
                }
                if (handle.status == Status.COOLDOWN && now < handle.cooldownUntilMs) {
                    continue;
                }
                return index;
            }
        }

        log.debug("No usable AI API key available ({} configured)", keys.size());
        return -1;
    }

    /**
     * Marks the key healthy again after a successful request.
     */
    public void reportSuccess(int index) {
        if (index < 0 || index >= handles.size()) {
            return;
        }
        handles.get(index).status = Status.ACTIVE;
        handles.get(index).cooldownUntilMs = 0;
    }

    /**
     * Records a failed request for the key. Auth failures (401/403) mark the key
     * permanently invalid; any other failure parks it in cooldown.
     *
     * @param index     the key index
     * @param permanent true for auth failures that will never recover
     */
    public void reportFailure(int index, boolean permanent) {
        if (index < 0 || index >= handles.size()) {
            return;
        }
        KeyHandle handle = handles.get(index);
        if (permanent) {
            handle.status = Status.INVALID;
            handle.cooldownUntilMs = 0;
            log.warn("AI API key #{} marked INVALID (auth failure).", index + 1);
        } else {
            handle.status = Status.COOLDOWN;
            handle.cooldownUntilMs = System.currentTimeMillis() + cooldownMs;
            log.warn("AI API key #{} moved to cooldown for {}ms.", index + 1, cooldownMs);
        }
    }
}