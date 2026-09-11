package com.rajneeti.game;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

/**
 * A single influence card in a game of Rajneeti.
 *
 * <p>Cards are server-side only. The character of a card held by a player is
 * private information; it is never exposed to other players.
 */
@Getter
@Builder
public class GameCard {

    private final UUID id;

    private final CharacterType character;

    /** Whether the card has been revealed (e.g. after a successful challenge). */
    @Builder.Default
    private boolean revealed = false;
}