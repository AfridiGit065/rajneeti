package com.rajneeti.dto.game;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Player-safe projection of a single game-log entry.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GameLogEntryDto {

    private String id;

    private LocalDateTime timestamp;

    private String text;

    private String kind;
}