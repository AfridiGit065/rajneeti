package com.rajneeti.dto.game;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Request body for declaring a Steal: the acting player names the opposing
 * target. The engine validates the rest (turn, balance, target rules).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StealRequest {

    /** The user ID of the targeted opponent. */
    private UUID targetPlayerId;
}