package com.rajneeti.bot.driver;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Module 25 — tracks which matches are driven by AI bots and holds one memory
 * container per bot per match.
 *
 * <p>Memory is the bot's own clock for window deduplication and pacing:
 * <ul>
 *   <li>{@code seenAt} — wall-clock millisecond when a decision window was first
 *       observed (the "ponder start");</li>
 *   <li>{@code decided} — window keys this bot already answered (one response
 *       per window, matching the human model);</li>
 *   <li>{@code attempts} — how often an own-action decision was rejected, so a
 *       bot cannot spin forever on an illegal LLM proposal.</li>
 * </ul>
 */
@Component
public class BotMatchRegistry {

    /** Per-bot decision memory inside a registered match. */
    public static class BotMemory {
        private final Map<String, Long> seenAt = new LinkedHashMap<>();
        private final Map<String, Boolean> decided = new ConcurrentHashMap<>();
        private final Map<String, Integer> attempts = new ConcurrentHashMap<>();

        void markSeen(String windowKey, long now) {
            seenAt.putIfAbsent(windowKey, now);
        }

        Long seenAt(String windowKey) {
            return seenAt.get(windowKey);
        }

        boolean isDecided(String windowKey) {
            return decided.containsKey(windowKey);
        }

        void markDecided(String windowKey) {
            decided.put(windowKey, Boolean.TRUE);
        }

        void clearSeen(String windowKey) {
            seenAt.remove(windowKey);
        }

        int incrementAttempt(String windowKey) {
            return attempts.merge(windowKey, 1, Integer::sum);
        }
    }

    private final Map<UUID, Map<UUID, BotMemory>> matches = new ConcurrentHashMap<>();

    /** Registers a match and the bots that live in it (idempotent). */
    public void register(UUID matchId, List<UUID> botIds) {
        Map<UUID, BotMemory> bots = matches.computeIfAbsent(matchId, id -> new ConcurrentHashMap<>());
        for (UUID botId : botIds) {
            bots.computeIfAbsent(botId, id -> new BotMemory());
        }
    }

    /** Removes a finished match so the scheduler stops polling it. */
    public void unregister(UUID matchId) {
        matches.remove(matchId);
    }

    public boolean isRegistered(UUID matchId) {
        return matches.containsKey(matchId);
    }

    public boolean isRegistered(UUID matchId, UUID botId) {
        Map<UUID, BotMemory> bots = matches.get(matchId);
        return bots != null && bots.containsKey(botId);
    }

    public List<UUID> botIds(UUID matchId) {
        Map<UUID, BotMemory> bots = matches.get(matchId);
        return bots == null ? List.of() : List.copyOf(bots.keySet());
    }

    public BotMemory memory(UUID matchId, UUID botId) {
        Map<UUID, BotMemory> bots = matches.get(matchId);
        if (bots == null) {
            throw new IllegalStateException("Match '" + matchId + "' is not registered for bot driving.");
        }
        BotMemory memory = bots.get(botId);
        if (memory == null) {
            throw new IllegalStateException("Bot '" + botId + "' is not registered for match '" + matchId + "'.");
        }
        return memory;
    }

    /** Snapshot of all registered match ids (for the scheduler loop). */
    public List<UUID> registeredMatches() {
        return List.copyOf(matches.keySet());
    }
}