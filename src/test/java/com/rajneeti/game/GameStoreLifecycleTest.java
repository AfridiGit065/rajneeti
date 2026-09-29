package com.rajneeti.game;

import com.rajneeti.entity.enums.MatchStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Module 21 — retention of the in-memory game registry.
 *
 * <p>A finished match cannot be deleted the instant it ends: clients still need
 * the final snapshot, the game-over screen, a late resync and post-match
 * history. What must not happen is that this registry grows without bound. These
 * tests pin the contract that replaces the old unbounded map: a soft cap, with
 * finished matches dropped oldest-first and live matches never sacrificed.
 */
class GameStoreLifecycleTest {

    private static GameState state(MatchStatus status, LocalDateTime endedAt) {
        return state(UUID.randomUUID(), status, endedAt);
    }

    private static GameState state(UUID matchId, MatchStatus status, LocalDateTime endedAt) {
        return GameState.builder()
                .matchId(matchId)
                .status(status)
                .endedAt(endedAt)
                .build();
    }

    /** Builds a store with a small cap, as {@code @Value} would in production. */
    private static GameStore storeWithCap(int cap) {
        GameStore store = new GameStore();
        try {
            Field field = GameStore.class.getDeclaredField("maxEntries");
            field.setAccessible(true);
            field.setInt(store, cap);
        } catch (ReflectiveOperationException ex) {
            throw new AssertionError("Unable to set the retention cap", ex);
        }
        return store;
    }

    /* ------------------------------------------------------------------ */
    /*  A plain registry still behaves like a registry                      */
    /* ------------------------------------------------------------------ */

    @Test
    @DisplayName("A - put/get/containsKey/size/remove behave like a normal map")
    void registry_behavesLikeAMap() {
        GameStore store = new GameStore();
        UUID id = UUID.randomUUID();

        assertThat(store.size()).isZero();
        assertThat(store.get(id)).isNull();

        GameState game = state(MatchStatus.IN_PROGRESS, null);
        store.put(id, game);

        assertThat(store.get(id)).isSameAs(game);
        assertThat(store.containsKey(id)).isTrue();
        assertThat(store.size()).isEqualTo(1);

        store.remove(id);
        assertThat(store.get(id)).isNull();
        assertThat(store.containsKey(id)).isFalse();
    }

    @Test
    @DisplayName("A - the default cap is the documented 500 and eviction is a no-op beneath it")
    void defaultCap_isUnenforcedWhileUnderIt() {
        GameStore store = new GameStore();
        assertThat(store.maxEntries()).isEqualTo(GameStore.DEFAULT_MAX_ENTRIES);

        for (int i = 0; i < GameStore.DEFAULT_MAX_ENTRIES; i++) {
            store.put(UUID.randomUUID(), state(MatchStatus.FINISHED, LocalDateTime.now()));
        }

        assertThat(store.size()).isEqualTo(GameStore.DEFAULT_MAX_ENTRIES);
        assertThat(store.evictedTotal()).isZero();
        assertThat(store.evictFinishedUnderPressure()).isZero();
    }

    /* ------------------------------------------------------------------ */
    /*  Finished states are reclaimed, oldest first                         */
    /* ------------------------------------------------------------------ */

    @Test
    @DisplayName("B - crossing the cap evicts finished matches oldest-first")
    void overCap_evictsOldestFinishedFirst() {
        GameStore store = storeWithCap(3);
        LocalDateTime now = LocalDateTime.now();

        UUID oldest = UUID.randomUUID();
        UUID middle = UUID.randomUUID();
        UUID newest = UUID.randomUUID();
        store.put(oldest, state(MatchStatus.FINISHED, now.minusMinutes(30)));
        store.put(middle, state(MatchStatus.FINISHED, now.minusMinutes(20)));
        store.put(newest, state(MatchStatus.FINISHED, now.minusMinutes(10)));
        assertThat(store.size()).isEqualTo(3);

        // One more live match tips the registry over the cap.
        UUID live = UUID.randomUUID();
        store.put(live, state(MatchStatus.IN_PROGRESS, null));

        assertThat(store.size()).isEqualTo(3);
        assertThat(store.get(oldest)).isNull();
        assertThat(store.get(middle)).isNotNull();
        assertThat(store.get(newest)).isNotNull();
        assertThat(store.get(live)).isNotNull();
        assertThat(store.evictedTotal()).isEqualTo(1);
    }

    @Test
    @DisplayName("B - eviction drains all finished matches when the overflow exceeds their count")
    void overCap_evictsEveryFinishedMatchBeforeTouchingLiveOnes() {
        GameStore store = storeWithCap(2);
        LocalDateTime now = LocalDateTime.now();

        UUID f1 = UUID.randomUUID();
        UUID f2 = UUID.randomUUID();
        UUID l1 = UUID.randomUUID();
        UUID l2 = UUID.randomUUID();
        store.put(f1, state(MatchStatus.FINISHED, now.minusMinutes(5)));
        store.put(f2, state(MatchStatus.FINISHED, now.minusMinutes(4)));
        store.put(l1, state(MatchStatus.IN_PROGRESS, null));
        store.put(l2, state(MatchStatus.IN_PROGRESS, null));
        assertThat(store.size()).isEqualTo(2);

        UUID l3 = UUID.randomUUID();
        store.put(l3, state(MatchStatus.IN_PROGRESS, null));

        assertThat(store.get(f1)).isNull();
        assertThat(store.get(f2)).isNull();
        assertThat(store.get(l1)).isNotNull();
        assertThat(store.get(l2)).isNotNull();
        assertThat(store.get(l3)).isNotNull();
    }

    @Test
    @DisplayName("B - a finished state with no endedAt stamp is still evictable")
    void overCap_evictsUnstampedFinishedState() {
        GameStore store = storeWithCap(1);

        UUID unstamped = UUID.randomUUID();
        store.put(unstamped, state(MatchStatus.FINISHED, null));
        store.put(UUID.randomUUID(), state(MatchStatus.FINISHED, LocalDateTime.now()));

        // The unstamped entry sorts oldest by the stable fallback, so it goes first.
        assertThat(store.get(unstamped)).isNull();
        assertThat(store.size()).isEqualTo(1);
    }

    /* ------------------------------------------------------------------ */
    /*  Live matches are never sacrificed                                   */
    /* ------------------------------------------------------------------ */

    @Test
    @DisplayName("C - a registry full of live matches exceeds the cap rather than dropping one")
    void overCap_neverEvictsALiveMatch() {
        GameStore store = storeWithCap(2);

        List<UUID> live = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            UUID id = UUID.randomUUID();
            live.add(id);
            store.put(id, state(MatchStatus.IN_PROGRESS, null));
        }

        assertThat(store.size()).isEqualTo(5);
        assertThat(store.evictedTotal()).isZero();
        for (UUID id : live) {
            assertThat(store.get(id)).as("live match %s must survive", id).isNotNull();
        }
    }

    @Test
    @DisplayName("C - non-finished statuses are all treated as live")
    void onlyFinishedStatusIsEvictable() {
        for (MatchStatus status : MatchStatus.values()) {
            if (status == MatchStatus.FINISHED) {
                continue;
            }
            GameStore store = storeWithCap(0);
            UUID id = UUID.randomUUID();
            store.put(id, state(status, LocalDateTime.now()));
            assertThat(store.get(id)).as("status %s must be retained", status).isNotNull();
        }
    }

    /* ------------------------------------------------------------------ */
    /*  Eviction must not disturb the rest of the application              */
    /* ------------------------------------------------------------------ */

    @Test
    @DisplayName("D - the retained state object is handed back unchanged, never a copy")
    void eviction_neverMutatesSurvivingState() {
        GameStore store = storeWithCap(1);
        UUID survivor = UUID.randomUUID();
        GameState live = state(MatchStatus.IN_PROGRESS, null);
        live.setStateVersion(7L);
        store.put(survivor, live);

        store.put(UUID.randomUUID(), state(MatchStatus.FINISHED, LocalDateTime.now()));

        assertThat(store.get(survivor)).isSameAs(live);
        assertThat(live.getStateVersion()).isEqualTo(7L);
    }

    @Test
    @DisplayName("D - explicit remove still works and is not confused with eviction")
    void explicitRemove_isDistinctFromEviction() {
        GameStore store = storeWithCap(3);
        UUID id = UUID.randomUUID();
        store.put(id, state(MatchStatus.FINISHED, LocalDateTime.now()));

        store.remove(id);

        assertThat(store.get(id)).isNull();
        assertThat(store.size()).isZero();
        // Removing on demand is not eviction pressure.
        assertThat(store.evictedTotal()).isZero();
    }

    /* ------------------------------------------------------------------ */
    /*  Thread safety                                                       */
    /* ------------------------------------------------------------------ */

    @Test
    @DisplayName("E - concurrent puts never lose states or corrupt the eviction counter")
    void concurrentPuts_keepStateConsistent() throws InterruptedException {
        GameStore store = storeWithCap(50);
        int threads = 8;
        int perThread = 100;

        List<UUID> allIds = new ArrayList<>();
        for (int i = 0; i < threads * perThread; i++) {
            allIds.add(UUID.randomUUID());
        }

        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        try {
            for (int t = 0; t < threads; t++) {
                final int offset = t * perThread;
                pool.submit(() -> {
                    start.await();
                    for (int i = 0; i < perThread; i++) {
                        UUID id = allIds.get(offset + i);
                        // Half finished, half live, so eviction is genuinely under pressure.
                        MatchStatus status = i % 2 == 0
                                ? MatchStatus.FINISHED
                                : MatchStatus.IN_PROGRESS;
                        store.put(id, state(id, status, LocalDateTime.now().plusNanos(i)));
                    }
                    return null;
                });
            }
            start.countDown();
            pool.shutdown();
            assertThat(pool.awaitTermination(60, TimeUnit.SECONDS)).isTrue();
        } finally {
            pool.shutdownNow();
        }

        int liveInserted = threads * perThread / 2;
        int finishedInserted = threads * perThread / 2;

        // 400 live states were inserted against a cap of 50, so the cap cannot be
        // honoured: every finished state is reclaimed and all 400 live ones stay.
        assertThat(store.size()).isEqualTo(liveInserted);
        assertThat(store.evictedTotal()).isEqualTo(finishedInserted);

        // Every survivor is retrievable, keyed correctly, and no live match was dropped.
        int liveSurvivors = 0;
        for (int i = 0; i < threads * perThread; i++) {
            GameState retained = store.get(allIds.get(i));
            if (retained != null) {
                assertThat(retained.getMatchId()).isEqualTo(allIds.get(i));
                if (retained.getStatus() != MatchStatus.FINISHED) {
                    liveSurvivors++;
                }
            }
        }
        assertThat(liveSurvivors).isEqualTo(liveInserted);
    }
}
