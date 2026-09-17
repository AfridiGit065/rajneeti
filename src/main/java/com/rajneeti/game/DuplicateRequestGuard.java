package com.rajneeti.game;

import com.rajneeti.exception.BusinessException;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Module 23 — idempotency guard for client-initiated sync requests.
 *
 * <p>Every game-affecting request (the REST action calls and the STOMP
 * {@code /app/matches/{matchId}/sync} command) carries a client-generated
 * {@code requestId}. The guard atomically checks-and-registers the id per
 * user so duplicate or delayed retries of the SAME intent are rejected exactly
 * once with {@code DUPLICATE_REQUEST} instead of being re-applied.
 *
 * <p>Memory is bounded: each user's registered ids are capped; when a user's
 * window overflows, the whole window is reset (a cleared id only causes a
 * harmless one-off re-application, never a correctness problem).
 */
@Component
public class DuplicateRequestGuard {

    /** Hard cap on retained request ids per user (matches worst-case burst pacing). */
    private static final int MAX_IDS_PER_USER = 1024;

    private final ConcurrentHashMap<String, Set<String>> registered = new ConcurrentHashMap<>();

    /**
     * Atomically checks and registers {@code requestId} for {@code userId}.
     *
     * @return {@code true} if the id was ALREADY registered (a duplicate)
     */
    public boolean isDuplicate(String userId, String requestId) {
        if (userId == null || requestId == null || requestId.isBlank()) {
            return false;
        }
        Set<String> ids = registered.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet());
        boolean first = ids.add(requestId);
        if (ids.size() > MAX_IDS_PER_USER) {
            // Bounded window: replace with a fresh set; old ids become effectively reusable.
            registered.put(userId, ConcurrentHashMap.newKeySet());
        }
        return !first;
    }

    /**
     * Rejects the request as a duplicate, as a domain-level business error.
     *
     * @throws BusinessException {@code DUPLICATE_REQUEST}
     */
    public void rejectDuplicate(String userId, String requestId) {
        if (isDuplicate(userId, requestId)) {
            throw new BusinessException("DUPLICATE_REQUEST",
                    "A request with this id was already processed");
        }
    }

    /** Clears every registered id for a user (used on disconnect / in tests). */
    public void clearForUser(String userId) {
        if (userId != null) {
            registered.remove(userId);
        }
    }
}