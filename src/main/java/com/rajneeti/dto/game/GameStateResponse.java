package com.rajneeti.dto.game;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.rajneeti.entity.enums.MatchStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Player-safe projection of the full game state.
 *
 * <p>Only public information is exposed: every player's coin balance, influence
 * count and alive status, plus the requesting player's own cards. Hidden cards,
 * the remaining deck order and any other server-only state are never included.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GameStateResponse {

    private UUID matchId;

    private UUID roomId;

    private String roomCode;

    private MatchStatus status;

    /** Coarse lifecycle phase: "setup", "in_progress", "game_over". */
    private String phase;

    private UUID hostUserId;

    private List<GamePlayerDto> players;

    private UUID currentTurnPlayerId;

    private Integer turnNumber;

    private List<UUID> turnOrder;

    /** Remaining (hidden) deck size, safe to expose. */
    private Integer deckCount;

    private Integer revealedCardsCount;

    private UUID winnerUserId;

    private List<GameLogEntryDto> log;

    private LocalDateTime startedAt;

    private LocalDateTime endedAt;
}