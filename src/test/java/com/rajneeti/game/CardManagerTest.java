package com.rajneeti.game;

import com.rajneeti.entity.enums.PlayerStatus;
import com.rajneeti.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CardManagerTest {

    private final CardManager cardManager = new CardManager();

    private GameCard card(CharacterType character) {
        return GameCard.builder().id(UUID.randomUUID()).character(character).build();
    }

    private GamePlayerState player(UUID userId, List<GameCard> cards) {
        return GamePlayerState.builder()
                .userId(userId)
                .username("p-" + userId)
                .seatNumber(1)
                .status(PlayerStatus.ACTIVE)
                .coins(2)
                .cards(cards)
                .build();
    }

    @Test
    @DisplayName("Deck - contains exactly 15 cards")
    void createDeck_hasExactlyFifteenCards() {
        List<GameCard> deck = cardManager.createDeck();

        assertThat(deck).hasSize(CardManager.DECK_SIZE);
        assertThat(deck).hasSize(15);
    }

    @Test
    @DisplayName("Deck - each character appears exactly 3 times")
    void createDeck_hasThreeCopiesOfEachCharacter() {
        List<GameCard> deck = cardManager.createDeck();

        Map<CharacterType, Long> counts = new EnumMap<>(CharacterType.class);
        for (CharacterType character : CharacterType.values()) {
            counts.put(character, deck.stream()
                    .filter(card -> card.getCharacter() == character)
                    .count());
        }

        assertThat(counts).hasSize(5);
        for (CharacterType character : CharacterType.values()) {
            assertThat(counts.get(character)).isEqualTo(CardManager.COPIES_PER_CHARACTER);
        }
    }

    @Test
    @DisplayName("Deck - every physical card has a unique id")
    void createDeck_cardsHaveUniqueIdentity() {
        List<GameCard> deck = cardManager.createDeck();

        Set<UUID> ids = new HashSet<>();
        deck.forEach(card -> assertThat(ids.add(card.getId())).isTrue());
        assertThat(ids).hasSize(CardManager.DECK_SIZE);
    }

    @Test
    @DisplayName("Shuffle - preserves the full card pool without losing cards")
    void shuffle_preservesCardPool() {
        List<GameCard> deck = cardManager.createDeck();
        List<GameCard> original = new ArrayList<>(deck);

        cardManager.shuffle(deck, new Random(42));

        assertThat(deck).hasSize(original.size());
        assertThat(deck).containsExactlyInAnyOrderElementsOf(original);
    }

    @Test
    @DisplayName("Shuffle - reorders the deck (deterministic seeds, effectively certain)")
    void shuffle_reordersDeck() {
        List<GameCard> original = cardManager.createDeck();
        boolean reordered = false;
        for (long seed = 1; seed <= 10; seed++) {
            List<GameCard> deck = new ArrayList<>(original);
            cardManager.shuffle(deck, new Random(seed));
            reordered = reordered || !deck.equals(original);
        }
        assertThat(reordered).isTrue();
    }

    @Test
    @DisplayName("Draw - removes and returns the top card")
    void draw_removesTopCard() {
        List<GameCard> deck = cardManager.createDeck();
        GameCard drawn = cardManager.draw(deck);

        assertThat(drawn).isNotNull();
        assertThat(deck).doesNotContain(drawn);
        assertThat(deck).hasSize(CardManager.DECK_SIZE - 1);
    }

    @Test
    @DisplayName("Draw - empty deck is handled safely with an explicit error")
    void draw_emptyDeckThrows() {
        List<GameCard> deck = new ArrayList<>();

        assertThatThrownBy(() -> cardManager.draw(deck))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("deck is empty");
    }

    @Test
    @DisplayName("Return - a returned card is restored to the deck and drawable again")
    void returnToDeck_restoresCard() {
        List<GameCard> deck = cardManager.createDeck();
        GameCard drawn = cardManager.draw(deck);

        cardManager.returnToDeck(deck, drawn);

        assertThat(deck).hasSize(CardManager.DECK_SIZE);
        assertThat(deck).contains(drawn);
    }

    @Test
    @DisplayName("Return - a card already in the deck is rejected (no duplicate copies)")
    void returnToDeck_rejectsDuplicate() {
        List<GameCard> deck = cardManager.createDeck();
        GameCard existing = deck.get(0);

        assertThatThrownBy(() -> cardManager.returnToDeck(deck, existing))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("already in the deck");
    }

    @Test
    @DisplayName("Deal - every player receives 2 cards from the same shared deck")
    void dealInitialHands_givesTwoCardsPerPlayer() {
        List<GameCard> deck = cardManager.createShuffledDeck();
        List<GamePlayerState> players = List.of(
                player(UUID.randomUUID(), new ArrayList<>()),
                player(UUID.randomUUID(), new ArrayList<>()),
                player(UUID.randomUUID(), new ArrayList<>()),
                player(UUID.randomUUID(), new ArrayList<>()));

        cardManager.dealInitialHands(deck, players);

        for (GamePlayerState player : players) {
            assertThat(player.getCards()).hasSize(CardManager.STARTING_HAND);
        }
        assertThat(deck).hasSize(CardManager.DECK_SIZE
                - players.size() * CardManager.STARTING_HAND);
    }

    @Test
    @DisplayName("Deal - cards come from one 15-card deck and never overlap")
    void dealInitialHands_noDuplicatePhysicalCards() {
        List<GameCard> deck = cardManager.createShuffledDeck();
        List<GamePlayerState> players = List.of(
                player(UUID.randomUUID(), new ArrayList<>()),
                player(UUID.randomUUID(), new ArrayList<>()));

        cardManager.dealInitialHands(deck, players);

        Set<UUID> allCards = new HashSet<>();
        players.forEach(p -> p.getCards().forEach(card -> {
            assertThat(allCards.add(card.getId())).isTrue();
        }));
        deck.forEach(card -> assertThat(allCards.add(card.getId())).isTrue());
        assertThat(allCards).hasSize(CardManager.DECK_SIZE);
    }

    @Test
    @DisplayName("Integrity - valid deck + hands pass; a duplicate card in two places fails")
    void assertDeckIntegrity_detectsInvalidState() {
        List<GameCard> deck = cardManager.createShuffledDeck();
        List<GamePlayerState> players = List.of(
                player(UUID.randomUUID(), new ArrayList<>()),
                player(UUID.randomUUID(), new ArrayList<>()));
        cardManager.dealInitialHands(deck, players);
        cardManager.assertDeckIntegrity(deck, players); // no exception

        // Place the same card into two hands.
        GamePlayerState first = players.get(0);
        GamePlayerState second = players.get(1);
        first.getCards().add(second.getCards().get(0));
        List<GamePlayerState> corrupted = List.of(first, second);

        assertThatThrownBy(() -> cardManager.assertDeckIntegrity(
                new ArrayList<>(first.getCards()), corrupted))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("more than one place");
    }

    @Test
    @DisplayName("Ownership - identifies the owner of a held card and null for deck cards")
    void cardOwner_detectsOwner() {
        List<GameCard> deck = cardManager.createDeck();
        GameCard drawn = cardManager.draw(deck);
        UUID ownerId = UUID.randomUUID();
        List<GamePlayerState> players = List.of(player(ownerId, new ArrayList<>()));
        players.get(0).getCards().add(drawn);

        GameState state = GameState.builder()
                .matchId(UUID.randomUUID())
                .players(players)
                .deck(deck)
                .build();

        assertThat(cardManager.cardOwner(state, drawn.getId())).isEqualTo(ownerId);
        assertThat(cardManager.cardOwner(state, deck.get(0).getId())).isNull();
    }
}