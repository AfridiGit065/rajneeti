package com.rajneeti.game;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A single entry in the in-game event log.
 */
@Getter
@AllArgsConstructor
public class GameLogEntry {

    private final String id;

    private final LocalDateTime timestamp;

    private final String text;

    /** Generic event kind, e.g. "info", "action", "challenge", "block". */
    private final String kind;

    public static GameLogEntry info(String text) {
        return new GameLogEntry(UUID.randomUUID().toString(), LocalDateTime.now(), text, "info");
    }

    public static GameLogEntry of(String kind, String text) {
        return new GameLogEntry(UUID.randomUUID().toString(), LocalDateTime.now(), text, kind);
    }
}