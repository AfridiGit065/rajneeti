package com.rajneeti.game;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Builds and shuffles the standard 15-card Rajneeti deck:
 * 5 characters x 3 copies each.
 */
@Component
public class DeckFactory {

    public static final int CHARACTER_COUNT = CharacterType.values().length;

    public static final int COPIES_PER_CHARACTER = 3;

    public static final int DECK_SIZE = CHARACTER_COUNT * COPIES_PER_CHARACTER;

    /**
     * Creates a fresh 15-card deck and shuffles it.
     *
     * @return a shuffled deck with one unique card per copy
     */
    public List<GameCard> createShuffledDeck() {
        List<GameCard> deck = new ArrayList<>(DECK_SIZE);
        for (CharacterType character : CharacterType.values()) {
            for (int i = 0; i < COPIES_PER_CHARACTER; i++) {
                deck.add(GameCard.builder()
                        .id(UUID.randomUUID())
                        .character(character)
                        .build());
            }
        }
        Collections.shuffle(deck);
        return deck;
    }

    /**
     * Draws {@code count} cards from the top of the deck.
     *
     * @param deck  mutable deck; top cards are removed
     * @param count number of cards to draw
     * @return the drawn cards, in draw order
     */
    public List<GameCard> draw(List<GameCard> deck, int count) {
        List<GameCard> drawn = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            if (deck.isEmpty()) {
                break;
            }
            drawn.add(deck.remove(deck.size() - 1));
        }
        return drawn;
    }
}