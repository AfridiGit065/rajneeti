package com.rajneeti.bot;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rajneeti.game.CharacterType;
import com.rajneeti.game.GameCard;
import com.rajneeti.game.GamePlayerState;
import com.rajneeti.game.GameState;
import com.rajneeti.bot.decision.BotDecisionContext;
import com.rajneeti.bot.decision.BotDecisionType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Module 25 — the fairness boundary. The serialized decision context handed to
 * a bot's brain must never leak an opponent's private cards, the deck order, or
 * an opponent's exchange pool.
 */
class BotDecisionContextFairnessTest {

    @Test
    @DisplayName("Context JSON exposes the bot's own cards but never opponent cards or the deck")
    void context_neverLeaksOpponentPrivateCards() throws Exception {
        UUID botId = UUID.randomUUID();
        UUID oppId = UUID.randomUUID();

        UUID secretCard1 = UUID.randomUUID();
        UUID secretCard2 = UUID.randomUUID();

        GamePlayerState bot = BotTestFixtures.botPlayer(botId, "bot-alpha", 2,
                List.of(BotTestFixtures.card(CharacterType.MINISTER),
                        BotTestFixtures.card(CharacterType.GHATOK)));
        GamePlayerState opp = BotTestFixtures.player(oppId, "human-omega", 5,
                List.of(GameCard.builder().id(secretCard1).character(CharacterType.AMLA).build(),
                        GameCard.builder().id(secretCard2).character(CharacterType.DALAL).build()));

        GameState state = BotTestFixtures.baseState(bot, opp)
                .deck(List.of(BotTestFixtures.card(CharacterType.MINISTER),
                        GameCard.builder().id(UUID.randomUUID()).character(CharacterType.DALAL).build()))
                .build();

        BotDecisionContext ctx = BotDecisionContext.create(state, bot, BotDecisionType.OWN_ACTION);

        assertThat(ctx.opponents()).hasSize(1);
        assertThat(ctx.legalTargets()).containsExactly(oppId);
        assertThat(ctx.richTargets()).containsExactly(oppId);
        assertThat(ctx.deckCount()).isEqualTo(2);

        String json = new ObjectMapper().writeValueAsString(ctx);

        // The opponent's secret card ids must not appear anywhere.
        assertThat(json).doesNotContain(secretCard1.toString());
        assertThat(json).doesNotContain(secretCard2.toString());

        // The bot's own card id is visible (it is the bot's private hand).
        assertThat(json).contains(ctx.ownCards().get(0).cardId().toString());

        // The opponents projection carries no cards array at all.
        JsonNode root = new ObjectMapper().readTree(json);
        JsonNode opponent = root.path("opponents").get(0);
        assertThat(opponent.path("cards").isMissingNode()).isTrue();
        assertThat(opponent.path("influenceCount").asInt()).isEqualTo(2);
        assertThat(opponent.path("coins").asInt()).isEqualTo(5);
        // A plain human opponent carries no bot metadata (and certainly no cards).
        assertThat(opponent.path("botDifficulty").isNull()).isTrue();
    }

    @Test
    @DisplayName("Exchange confirm exposes only the actor's own pool")
    void exchangeConfirm_poolIsOnlyTheActorsOwn() throws Exception {
        UUID botId = UUID.randomUUID();
        UUID oppId = UUID.randomUUID();

        UUID poolCard = UUID.randomUUID();

        GamePlayerState bot = BotTestFixtures.botPlayer(botId, "bot-alpha", 2,
                List.of(BotTestFixtures.card(CharacterType.GHATOK)));

        GameState state = BotTestFixtures.baseState(bot,
                        BotTestFixtures.player(oppId, "human-omega", 5,
                                List.of(BotTestFixtures.card(CharacterType.AMLA))))
                .build();

        String json = new ObjectMapper().writeValueAsString(
                BotDecisionContext.create(state, bot, BotDecisionType.EXCHANGE_CONFIRM));

        // The actor's own (pool-empty) exchange view has no pool.
        assertThat(json).doesNotContain(poolCard.toString());
    }
}