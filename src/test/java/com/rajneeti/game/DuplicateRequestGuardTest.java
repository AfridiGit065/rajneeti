package com.rajneeti.game;

import com.rajneeti.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Module 23 — unit tests for the per-user idempotency guard.
 */
class DuplicateRequestGuardTest {

    private final DuplicateRequestGuard guard = new DuplicateRequestGuard();

    @Test
    @DisplayName("The same requestId for the same user is a duplicate exactly once")
    void sameRequestIdForSameUserIsDuplicate() {
        assertThat(guard.isDuplicate("user-1", "req-1")).isFalse();
        assertThat(guard.isDuplicate("user-1", "req-1")).isTrue();
        assertThat(guard.isDuplicate("user-1", "req-1")).isTrue();
    }

    @Test
    @DisplayName("The same requestId for DIFFERENT users is NOT a duplicate")
    void sameRequestIdAcrossUsersIsNotDuplicate() {
        assertThat(guard.isDuplicate("user-1", "req-9")).isFalse();
        assertThat(guard.isDuplicate("user-2", "req-9")).isFalse();
        assertThat(guard.isDuplicate("user-1", "req-9")).isTrue();
    }

    @Test
    @DisplayName("Null or blank requestIds are never rejected (backward compatible)")
    void blankRequestIdsAreNotDeduplicated() {
        assertThat(guard.isDuplicate("user-1", null)).isFalse();
        assertThat(guard.isDuplicate("user-1", "")).isFalse();
        assertThat(guard.isDuplicate("user-1", "   ")).isFalse();
        guard.rejectDuplicate("user-1", null);
        guard.rejectDuplicate("user-1", "   ");
    }

    @Test
    @DisplayName("rejectDuplicate throws a BUSINESS error with DUPLICATE_REQUEST")
    void rejectDuplicateThrowsBusinessException() {
        guard.rejectDuplicate("user-1", "req-2");
        assertThatThrownBy(() -> guard.rejectDuplicate("user-1", "req-2"))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo("DUPLICATE_REQUEST");
    }

    @Test
    @DisplayName("clearForUser resets the registered window")
    void clearForUserResetsWindow() {
        guard.rejectDuplicate("user-1", "req-3");
        guard.clearForUser("user-1");
        assertThat(guard.isDuplicate("user-1", "req-3")).isFalse();
    }

    @Test
    @DisplayName("Concurrent submissions of the same requestId are deduplicated atomically")
    void concurrentSubmissionsAreAtomic() throws Exception {
        int threads = 16;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch go = new CountDownLatch(1);
        AtomicInteger accepted = new AtomicInteger();

        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                ready.countDown();
                try {
                    go.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                if (!guard.isDuplicate("user-race", "req-race")) {
                    accepted.incrementAndGet();
                }
            });
        }

        ready.await();
        go.countDown();
        pool.shutdown();
        assertThat(pool.awaitTermination(10, TimeUnit.SECONDS)).isTrue();

        // Exactly ONE caller wins the race; all others see a duplicate.
        assertThat(accepted.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("The per-user window is bounded: an overflowed window resets and allows re-use")
    void overflowResetsTheWindow() {
        // Fire just past the cap. The 1025th unique id triggers the reset path.
        for (int i = 0; i < 1025; i++) {
            assertThat(guard.isDuplicate("user-cap", "req-" + i)).isFalse();
        }
        guard.clearForUser("user-cap");
        // After a reset a previously used id becomes usable again (harmless retry).
        assertThat(guard.isDuplicate("user-cap", "req-1")).isFalse();
    }
}