package com.rajneeti.game;

import com.rajneeti.entity.enums.MatchStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * In-memory authoritative state of a running game.
 *
 * <p>This object is stored in the in-memory {@link GameStore} keyed by match
 * ID. It holds the live deck, each player's secret cards and coin balance, and
 * the current turn reference. Nothing here is written to MySQL on mutation;
 * persistent match metadata remains in the {@code matches} / {@code match_players}
 * tables for final outcome reporting.
 */
@Getter
@Setter
@Builder
public class GameState {

    private final UUID matchId;

    private final UUID roomId;

    private final String roomCode;

    private MatchStatus status;

    /**
     * Coarse lifecycle phase.
     * "setup" while players receive their initial cards/coins.
     */
    private String phase;

    private UUID hostUserId;

    /** Players ordered by seat number (deterministic turn order). */
    @Builder.Default
    private List<GamePlayerState> players = new ArrayList<>();

    private List<UUID> turnOrder;

    private UUID currentTurnPlayerId;

    private Integer turnNumber;

    /**
     * Remaining draw deck. Server-side only: the hidden order and the remaining
     * card characters are never exposed to clients.
     */
    @Builder.Default
    private List<GameCard> deck = new ArrayList<>();

    /** Number of cards revealed so far (informational, safe to expose). */
    @Builder.Default
    private int revealedCardsCount = 0;

    private UUID winnerUserId;

    @Builder.Default
    private List<GameLogEntry> log = new ArrayList<>();

    private LocalDateTime startedAt;

    private LocalDateTime endedAt;
}