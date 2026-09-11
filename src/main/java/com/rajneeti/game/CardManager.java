package com.rajneeti.game;

import com.rajneeti.exception.BusinessException;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/**
 * Module 10 — Card Manager.
 *
 * <p>The single owner of all influence-card logic in Rajneeti:
 * <ul>
 *   <li>creates the standard 15-card deck (5 characters x 3 copies)</li>
 *   <li>shuffles the deck before dealing</li>
 *   <li>draws cards safely (never null, explicit EMPTY_DECK error)</li>
 *   <li>returns cards to the deck without creating duplicates</li>
 *   <li>deals the initial 2 cards per player from one shared deck</li>
 *   <li>tracks card ownership and verifies deck integrity</li>
 * </ul>
 *
 * <p>GameEngine coordinates the game flow; this class manages cards. Live card
 * state stays in the in-memory {@link GameState} (deck + player hands) — no
 * per-card database table is created. Every physical card has a unique
 * server-side id (there are three copies of each character), so a single card
 * can never exist in two places at once.
 */
@Component
public class CardManager {

    /** Copies of each character in the deck. */
    public static final int COPIES_PER_CHARACTER = 3;

    /** Total physical cards: 5 characters x 3 copies. */
    public static final int DECK_SIZE = CharacterType.values().length * COPIES_PER_CHARACTER;

    /** Cards dealt to every player at match start. */
    public static final int STARTING_HAND = 2;

    /**
     * Creates the 15 unique physical cards of the standard deck, one distinct
     * server-side id per card. The order is deterministic (per character, copy
     * by copy); shuffle it before use.
     */
    public List<GameCard> createDeck() {
        List<GameCard> deck = new ArrayList<>(DECK_SIZE);
        for (CharacterType character : CharacterType.values()) {
            for (int i = 0; i < COPIES_PER_CHARACTER; i++) {
                deck.add(GameCard.builder()
                        .id(UUID.randomUUID())
                        .character(character)
                        .build());
            }
        }
        return deck;
    }

    /** Creates a fresh deck and shuffles it. */
    public List<GameCard> createShuffledDeck() {
        List<GameCard> deck = createDeck();
        shuffle(deck);
        return deck;
    }

    /** Randomizes the order of the given deck with the default random source. */
    public void shuffle(List<GameCard> deck) {
        if (deck != null) {
            Collections.shuffle(deck);
        }
    }

    /** Randomizes the order of the given deck with an explicit random source. */
    public void shuffle(List<GameCard> deck, Random random) {
        if (deck != null) {
            Collections.shuffle(deck, random);
        }
    }

    /**
     * Draws the top card of the deck.
     *
     * @param deck the live deck (in {@code GameState})
     * @return the drawn card
     * @throws BusinessException {@code EMPTY_DECK} when the deck is exhausted —
     *                           never returns null
     */
    public GameCard draw(List<GameCard> deck) {
        if (deck == null || deck.isEmpty()) {
            throw new BusinessException("EMPTY_DECK", "Cannot draw: the deck is empty.");
        }
        return deck.remove(deck.size() - 1);
    }

    /**
     * Draws up to {@code count} cards. Stops early only if the deck runs out;
     * the caller is expected to plan draws against the known deck size.
     */
    public List<GameCard> drawMany(List<GameCard> deck, int count) {
        List<GameCard> drawn = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            if (deck == null || deck.isEmpty()) {
                break;
            }
            drawn.add(draw(deck));
        }
        return drawn;
    }

    /**
     * Returns a card to the deck so it becomes available again.
     *
     * @param deck the live deck
     * @param card the card to return
     * @throws BusinessException {@code DUPLICATE_CARD} if the physical card is
     *                           already in the deck (safety guard against
     *                           accidentally creating a second copy)
     */
    public void returnToDeck(List<GameCard> deck, GameCard card) {
        if (card == null) {
            throw new BusinessException("NULL_CARD", "Cannot return a null card to the deck.");
        }
        boolean alreadyInDeck = deck.stream().anyMatch(c -> c.getId().equals(card.getId()));
        if (alreadyInDeck) {
            throw new BusinessException("DUPLICATE_CARD",
                    "Card '" + card.getId() + "' is already in the deck.");
        }
        deck.add(card);
    }

    /**
     * Deals {@link #STARTING_HAND} cards to every player from one shared deck.
     * Cards are drawn by the manager; GameEngine never allocates cards itself.
     */
    public void dealInitialHands(List<GameCard> deck, List<GamePlayerState> players) {
        for (GamePlayerState player : players) {
            player.getCards().addAll(drawMany(deck, STARTING_HAND));
        }
    }

    /**
     * Returns the owner's user id for a given card, or {@code null} when the
     * card is currently in the deck (not held by anyone).
     */
    public UUID cardOwner(GameState state, UUID cardId) {
        if (state == null || state.getPlayers() == null) {
            return null;
        }
        for (GamePlayerState player : state.getPlayers()) {
            if (player.getCards().stream().anyMatch(card -> card.getId().equals(cardId))) {
                return player.getUserId();
            }
        }
        return null;
    }

    /**
     * Verifies the single-location invariant of the whole game: every physical
     * card must exist in exactly one place (deck or exactly one player's hand)
     * and the union must equal the full 15-card deck.
     *
     * @throws BusinessException {@code DECK_INTEGRITY} or {@code DUPLICATE_CARD}
     *                           when the invariant is violated
     */
    public void assertDeckIntegrity(List<GameCard> deck, List<GamePlayerState> players) {
        Set<UUID> seen = new HashSet<>();
        for (GameCard card : deck) {
            register(seen, card, "deck");
        }
        for (GamePlayerState player : players) {
            for (GameCard card : player.getCards()) {
                register(seen, card, "player '" + player.getUserId() + "'");
            }
        }
        if (seen.size() != DECK_SIZE) {
            throw new BusinessException("DECK_INTEGRITY",
                    "Deck integrity violated: expected " + DECK_SIZE + " unique cards "
                            + "in play, found " + seen.size() + ".");
        }
    }

    private void register(Set<UUID> seen, GameCard card, String location) {
        if (!seen.add(card.getId())) {
            throw new BusinessException("DUPLICATE_CARD",
                    "Physical card '" + card.getId() + "' exists in more than one place (" + location + ").");
        }
    }
}