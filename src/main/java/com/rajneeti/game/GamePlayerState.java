package com.rajneeti.game;

import com.rajneeti.entity.enums.PlayerStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * In-memory runtime state of a single player within an active game.
 *
 * <p>Coin and influence-card state lives here for the lifetime of the game and
 * is never persisted per-mutation. Persistent match metadata (MatchPlayer)
 * keeps only the final outcome.
 */
@Getter
@Setter
@Builder
public class GamePlayerState {

    private final UUID userId;

    private final String username;

    private final String avatarUrl;

    /** 1-based seat number, matches the MatchPlayer seat number. */
    private final Integer seatNumber;

    @Builder.Default
    private PlayerStatus status = PlayerStatus.ACTIVE;

    /** Live coin balance. Starts at 2 for every player. */
    private int coins;

    /** Private influence cards. Only the owning player sees their characters. */
    @Builder.Default
    private List<GameCard> cards = new ArrayList<>();

    private boolean host;

    /**
     * Module 25 — whether this player is an AI bot. Carried from the User
     * entity so the bot pipeline and the public projection both know which
     * seats are controlled by an AI.
     */
    @Builder.Default
    private boolean isBot = false;

    /** Difficulty level for AI bots. Null for humans. */
    private String botDifficulty;

    /** Behaviour profile for AI bots. Null for humans. */
    private String botPersonality;
}