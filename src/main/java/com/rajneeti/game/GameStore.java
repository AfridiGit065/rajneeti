package com.rajneeti.game;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * In-memory registry for live game state.
 *
 * <p>Deliberately not backed by Redis or other external cache for Module 08:
 * game state lives in this JVM's memory. Entries are created on match start.
 *
 * <p><b>Lifecycle.</b> A finished match stays in this registry after
 * {@code WinnerManager} has persisted the outcome, because clients legitimately
 * still need it: the final {@code FINISHED} snapshot, the game-over screen, a
 * late resync and the post-match history all read from here. It is therefore
 * <em>not</em> removed at the instant the match ends.
 *
 * <p>To keep that retention bounded, {@link #put} evicts once the registry
 * exceeds {@link #maxEntries}: finished matches are dropped oldest-first until
 * the registry is back under the cap. Only finished matches are ever evicted —
 * a live match is never discarded, because that would silently break every
 * player at the table. If the registry is full of live matches the cap is
 * exceeded deliberately and a warning is logged, because losing a live game is
 * strictly worse than holding a few finished ones.
 *
 * <p>Persistence is unaffected by eviction: {@code matches}, {@code
 * match_players}, {@code match_history}, {@code statistics} and
 * {@code leaderboard} are all written before a match becomes
 * {@code FINISHED}, so an evicted state can never lose a recorded outcome.
 *
 * <p>Eviction is driven by match creation rather than by a background sweeper,
 * so there is no extra thread to reason about and no timer to leak.
 *
 * <p>Thread-safe: backed by a {@link ConcurrentHashMap}.
 */
@Slf4j
@Component
public class GameStore {

    private final Map<UUID, GameState> games = new ConcurrentHashMap<>();

    /** Retention cap used when no property is supplied (e.g. in unit tests). */
    public static final int DEFAULT_MAX_ENTRIES = 500;

    /**
     * Soft cap on retained states. Beyond this, finished matches are evicted
     * oldest-first on the next {@link #put}.
     */
    @Value("${game.store.max-retained-matches:500}")
    private int maxEntries = DEFAULT_MAX_ENTRIES;

    /**
     * Atomic because {@link #put} is called from concurrent request threads and
     * the eviction bookkeeping must not lose increments.
     */
    private final AtomicLong evictedTotal = new AtomicLong();

    public void put(UUID matchId, GameState state) {
        games.put(matchId, state);
        evictFinishedUnderPressure();
    }

    public GameState get(UUID matchId) {
        return games.get(matchId);
    }

    public boolean containsKey(UUID matchId) {
        return games.containsKey(matchId);
    }

    public void remove(UUID matchId) {
        games.remove(matchId);
    }

    public int size() {
        return games.size();
    }

    /** The configured retention cap. */
    public int maxEntries() {
        return maxEntries;
    }

    /** How many finished states this store has evicted since startup. */
    public long evictedTotal() {
        return evictedTotal.get();
    }

    /**
     * Drops finished states, oldest first, until the registry is back within
     * {@link #maxEntries}. No-op while the registry is under the cap.
     *
     * @return the number of states evicted
     */
    public int evictFinishedUnderPressure() {
        int overflow = games.size() - maxEntries;
        if (overflow <= 0) {
            return 0;
        }

        List<UUID> finished = games.entrySet().stream()
                .filter(e -> isFinished(e.getValue()))
                .sorted(Comparator.comparing(e -> endedAtOf(e.getValue())))
                .limit(overflow)
                .map(Map.Entry::getKey)
                .toList();

        for (UUID matchId : finished) {
            if (games.remove(matchId) != null) {
                evictedTotal.incrementAndGet();
            }
        }

        if (finished.isEmpty()) {
            log.warn("In-memory game store holds {} live matches, above the {} cap; "
                            + "no finished match is available to evict, so the cap is exceeded "
                            + "on purpose to keep every live game running.",
                    games.size(), maxEntries);
        } else {
            log.info("Evicted {} finished match state(s) to stay within the {} retained-match cap "
                            + "(registry now holds {}).", finished.size(), maxEntries, games.size());
        }
        return finished.size();
    }

    private boolean isFinished(GameState state) {
        return state != null
                && state.getStatus() == com.rajneeti.entity.enums.MatchStatus.FINISHED;
    }

    /** Ended-at stamp, with a stable fallback for states that never recorded one. */
    private LocalDateTime endedAtOf(GameState state) {
        return state.getEndedAt() != null ? state.getEndedAt() : LocalDateTime.MIN;
    }
}
