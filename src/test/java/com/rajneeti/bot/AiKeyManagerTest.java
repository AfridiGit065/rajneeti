package com.rajneeti.bot;

import com.rajneeti.bot.ai.AiKeyManager;
import com.rajneeti.bot.config.AiProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Module 25 — unit tests for the AI key manager's rotation, cooldown,
 * permanent-invalid handling and concurrent safety.
 */
class AiKeyManagerTest {

    private AiProperties properties(String... keys) {
        AiProperties props = new AiProperties();
        if (keys.length > 0) props.setApiKey1(keys[0]);
        if (keys.length > 1) props.setApiKey2(keys[1]);
        if (keys.length > 2) props.setApiKey3(keys[2]);
        return props;
    }

    @Test
    @DisplayName("No keys configured -> acquisition reports -1")
    void noKeys_reportsUnavailable() {
        AiProperties props = properties();
        AiKeyManager manager = new AiKeyManager(props);

        assertThat(manager.configuredKeyCount()).isZero();
        assertThat(manager.hasUsableKey()).isFalse();
        assertThat(manager.acquire()).isEqualTo(-1);
    }

    @Test
    @DisplayName("Rotation disabled -> key 0 is always returned")
    void rotationDisabled_alwaysReturnsFirstKey() {
        AiProperties props = properties("k1", "k2", "k3");
        props.setUseKeyRotation(false);
        AiKeyManager manager = new AiKeyManager(props);

        for (int i = 0; i < 5; i++) {
            assertThat(manager.acquire()).isZero();
        }
    }

    @Test
    @DisplayName("Rotation enabled -> keys are handed out round-robin")
    void rotationEnabled_rotatesThroughKeys() {
        AiProperties props = properties("k1", "k2", "k3");
        props.setUseKeyRotation(true);
        AiKeyManager manager = new AiKeyManager(props);

        assertThat(manager.acquire()).isZero();
        assertThat(manager.acquire()).isEqualTo(1);
        assertThat(manager.acquire()).isEqualTo(2);
        assertThat(manager.acquire()).isZero();
        assertThat(manager.acquire()).isEqualTo(1);
    }

    @Test
    @DisplayName("Transient failure parks a key in cooldown, then it recovers")
    void transientFailure_cooldownThenRecovery() throws Exception {
        AiProperties props = properties("k1", "k2");
        props.setUseKeyRotation(true);
        props.setKeyCooldownSeconds(1);
        AiKeyManager manager = new AiKeyManager(props);

        int first = manager.acquire();
        manager.reportFailure(first, false); // e.g. rate limit

        // The failed key is skipped in rotation order; the healthy one is next.
        assertThat(manager.acquire()).isNotEqualTo(first);

        // After the cooldown elapses the key becomes usable again.
        Thread.sleep(1100);
        assertThat(manager.hasUsableKey()).isTrue();
    }

    @Test
    @DisplayName("Auth failure permanently invalidates a key")
    void authFailure_keyIsPermanentlyInvalid() {
        AiProperties props = properties("k1", "k2");
        props.setUseKeyRotation(true);
        AiKeyManager manager = new AiKeyManager(props);

        manager.reportFailure(0, true); // 401

        // 100 acquisitions must never hand out the dead key.
        for (int i = 0; i < 100; i++) {
            assertThat(manager.acquire()).isEqualTo(1);
        }
        manager.reportSuccess(1);
        assertThat(manager.acquire()).isEqualTo(1);
    }

    @Test
    @DisplayName("All keys down -> acquisition reports -1")
    void allKeysDown_reportsUnavailable() {
        AiProperties props = properties("k1", "k2");
        props.setUseKeyRotation(true);
        props.setKeyCooldownSeconds(3600);
        AiKeyManager manager = new AiKeyManager(props);

        assertThat(manager.acquire()).isZero();
        assertThat(manager.acquire()).isEqualTo(1);
        manager.reportFailure(0, false);
        manager.reportFailure(1, false);

        assertThat(manager.hasUsableKey()).isFalse();
        assertThat(manager.acquire()).isEqualTo(-1);
    }

    @Test
    @DisplayName("Concurrent acquisitions stay safe and in-range")
    void concurrentAcquisitions_areSafeAndInRange() throws Exception {
        AiProperties props = properties("k1", "k2", "k3");
        props.setUseKeyRotation(true);
        AiKeyManager manager = new AiKeyManager(props);

        int threads = 24;
        int perThread = 200;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger outOfRange = new AtomicInteger();

        List<Future<?>> futures = new ArrayList<>();
        for (int t = 0; t < threads; t++) {
            futures.add(pool.submit(() -> {
                ready.countDown();
                try {
                    start.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
                for (int i = 0; i < perThread; i++) {
                    int idx = manager.acquire();
                    if (idx < 0 || idx > 2) {
                        outOfRange.incrementAndGet();
                    }
                }
            }));
        }
        ready.await();
        start.countDown();
        for (Future<?> f : futures) {
            f.get();
        }
        pool.shutdownNow();

        assertThat(outOfRange.get()).isZero();
    }
}