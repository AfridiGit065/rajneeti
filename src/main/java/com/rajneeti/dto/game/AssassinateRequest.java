package com.rajneeti.dto.game;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Request body for declaring an Assassination: the acting player names the
 * opposing target. The engine validates the rest (turn, cost, target rules).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssassinateRequest {

    /** The user ID of the targeted opponent. */
    private UUID targetPlayerId;
}