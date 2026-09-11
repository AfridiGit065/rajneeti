package com.rajneeti.dto.game;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Player-safe projection of a single influence card.
 *
 * <p>Only ever populated for the requesting player's own hand. Never sent for
 * opponents' cards.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GameCardDto {

    private UUID cardId;

    /** Lower-case character id, e.g. "minister". */
    private String characterId;
}