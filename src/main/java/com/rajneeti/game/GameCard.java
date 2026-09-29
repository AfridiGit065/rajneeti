package com.rajneeti.game;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

/**
 * A single influence card in a game of Rajneeti.
 *
 * <p>Cards are server-side only. The character of a card held by a player is
 * private information; it is never exposed to other players.
 *
 * <p>Whether a card has been revealed is tracked per match on
 * {@link GameState#getRevealedCardsCount()} rather than per card: a reveal is an
 * event on the match (it drives the deck-integrity check and the public reveal
 * tally), and the characters of revealed cards are carried on the
 * {@link GameChallenge} that caused them. Keeping the flag here would have been a
 * second, unread source of the same fact.
 */
@Getter
@Builder
public class GameCard {

    private final UUID id;

    private final CharacterType character;
}