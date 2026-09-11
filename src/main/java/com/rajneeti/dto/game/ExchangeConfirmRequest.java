package com.rajneeti.dto.game;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Request body for resolving a pending Exchange: the actor lists the card IDs
 * they want to keep. The backend validates the selection against the recorded
 * private pool — the client never sends card objects.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExchangeConfirmRequest {

    /** Exactly 2 unique card IDs, all drawn from the actor's exchange pool. */
    private List<UUID> keepCardIds;
}