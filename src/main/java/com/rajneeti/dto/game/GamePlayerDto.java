package com.rajneeti.dto.game;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.rajneeti.entity.enums.PlayerStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Player-safe projection of a player's runtime game state.
 *
 * <p>{@code cards} is populated only for the requesting player's own hand.
 * Opponents only ever receive {@code influenceCount}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GamePlayerDto {

    private UUID userId;

    private String username;

    private String avatarUrl;

    private Integer seatIndex;

    private PlayerStatus status;

    private boolean host;

    private boolean alive;

    private boolean turn;

    private int coins;

    private int influenceCount;

    /** Own cards only; null (omitted) for opponents. */
    private List<GameCardDto> cards;
}