package com.rajneeti.game;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory registry for live game state.
 *
 * <p>Deliberately not backed by Redis or other external cache for Module 08:
 * game state lives in this JVM's memory. Entries are created on match start and
 * removed when the game finishes.
 *
 * <p>Thread-safe: backed by a {@link ConcurrentHashMap}.
 */
@Component
public class GameStore {

    private final Map<UUID, GameState> games = new ConcurrentHashMap<>();

    public void put(UUID matchId, GameState state) {
        games.put(matchId, state);
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
}