package com.rajneeti.dto.websocket;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Payload for card reveal / private draw notifications.
 *
 * <p>{@code reason} is {@code "revealed"} when a card becomes public knowledge
 * (broadcast to the match) or {@code "drawn"} when a replacement card is dealt
 * privately to its owner (sent only to that player's queue).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CardRevealPayload {

    private UUID playerId;
    private String username;
    private String characterId;
    private String reason;
}