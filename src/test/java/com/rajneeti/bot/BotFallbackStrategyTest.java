package com.rajneeti.bot;

import com.rajneeti.bot.decision.BotActionType;
import com.rajneeti.bot.decision.BotDecision;
import com.rajneeti.bot.decision.BotDecisionContext;
import com.rajneeti.bot.decision.BotDecisionType;
import com.rajneeti.bot.decision.BotFallbackStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Module 25 — validates the deterministic heuristic fallback produces legal
 * decisions for every window type (own action, action challenge, action block,
 * block challenge, exchange confirm, actor resolve).
 */
class BotFallbackStrategyTest {

    private static final UUID MATCH = UUID.randomUUID();
    private static final UUID BOT = UUID.randomUUID();
    private static final UUID OPP = UUID.randomUUID();
    private static final UUID OPP2 = UUID.randomUUID();

    private BotFallbackStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new BotFallbackStrategy();
    }

    private BotDecisionContext.OwnCard card(String id, String character) {
        return new BotDecisionContext.OwnCard(UUID.fromString(id), character);
    }

    private BotDecisionContext context(BotDecisionType type, int coins, int influence,
                                       List<BotDecisionContext.OwnCard> cards,
                                       List<BotDecisionContext.OpponentView> opponents,
                                       BotDecisionContext.PendingFacts pending,
                                       List<BotDecisionContext.PoolCard> pool) {
        List<UUID> legal = opponents.stream().map(BotDecisionContext.OpponentView::userId).toList();
        List<UUID> rich = opponents.stream()
                .filter(o -> o.coins() > 0).map(BotDecisionContext.OpponentView::userId).toList();
        return new BotDecisionContext(MATCH, BOT, "bot", "MEDIUM", "BALANCED",
                type, 3, coins >= com.rajneeti.game.GameEngine.FORCED_COUP_THRESHOLD, coins,
                influence,
                cards, opponents, legal, rich, pending, pool, 20);
    }

    private BotDecisionContext.OpponentView opponent(UUID id, int coins, int influence) {
        return new BotDecisionContext.OpponentView(id, id.toString().substring(0, 8), coins,
                influence, false, null);
    }

    private BotDecisionContext.PendingFacts pending(String actionType, String claimed) {
        return new BotDecisionContext.PendingFacts(UUID.randomUUID(), actionType, OPP, claimed,
                BOT, null, null, null, null);
    }

    /* ---------------- Own action ---------------- */

    @Test
    @DisplayName("Mandatory coup is chosen at 10 coins with a legal target")
    void ownAction_mandatoryCoup() {
        BotDecision d = strategy.decide(context(BotDecisionType.OWN_ACTION, 10, 2,
                List.of(card("a0000000-0000-0000-0000-000000000001", "ghatok")),
                List.of(opponent(OPP, 1, 2)), null, null));
        assertThat(d.action()).isEqualTo(BotActionType.COUP);
        assertThat(d.targetPlayerId()).isEqualTo(OPP);
    }

    @Test
    @DisplayName("Truthful tax with minister in hand")
    void ownAction_truthfulTax() {
        BotDecision d = strategy.decide(context(BotDecisionType.OWN_ACTION, 2, 2,
                List.of(card("a0000000-0000-0000-0000-000000000002", "minister")),
                List.of(opponent(OPP, 3, 2)), null, null));
        assertThat(d.action()).isEqualTo(BotActionType.TAX);
        assertThat(d.claimedCharacter()).isEqualTo("minister");
    }

    @Test
    @DisplayName("Finishing coup when rich and an opponent has one card")
    void ownAction_finishingCoup() {
        BotDecision d = strategy.decide(context(BotDecisionType.OWN_ACTION, 8, 2,
                List.of(card("a0000000-0000-0000-0000-000000000003", "ghatok")),
                List.of(opponent(OPP, 1, 1)), null, null));
        assertThat(d.action()).isEqualTo(BotActionType.COUP);
        assertThat(d.targetPlayerId()).isEqualTo(OPP);
    }

    @Test
    @DisplayName("Truthful steal targets the richest opponent")
    void ownAction_stealRichest() {
        BotDecisionContext.OpponentView poor = opponent(OPP, 1, 2);
        BotDecisionContext.OpponentView rich = opponent(OPP2, 9, 2);
        BotDecision d = strategy.decide(context(BotDecisionType.OWN_ACTION, 2, 2,
                List.of(card("a0000000-0000-0000-0000-000000000004", "dalal")),
                List.of(poor, rich), null, null));
        assertThat(d.action()).isEqualTo(BotActionType.STEAL);
        assertThat(d.targetPlayerId()).isEqualTo(OPP2);
    }

    @Test
    @DisplayName("Exchange refreshes the hand when holding amla")
    void ownAction_exchange() {
        BotDecision d = strategy.decide(context(BotDecisionType.OWN_ACTION, 2, 2,
                List.of(card("a0000000-0000-0000-0000-000000000005", "amla")),
                List.of(opponent(OPP, 0, 2)), null, null));
        assertThat(d.action()).isEqualTo(BotActionType.EXCHANGE);
        assertThat(d.claimedCharacter()).isEqualTo("amla");
    }

    @Test
    @DisplayName("Truthful assassination when affordable")
    void ownAction_assassinate() {
        BotDecision d = strategy.decide(context(BotDecisionType.OWN_ACTION, 3, 2,
                List.of(card("a0000000-0000-0000-0000-000000000006", "ghatok")),
                List.of(opponent(OPP, 2, 2)), null, null));
        assertThat(d.action()).isEqualTo(BotActionType.ASSASSINATE);
        assertThat(d.targetPlayerId()).isEqualTo(OPP);
    }

    @Test
    @DisplayName("Foreign aid when balanced and no better option")
    void ownAction_foreignAid() {
        BotDecision d = strategy.decide(context(BotDecisionType.OWN_ACTION, 2, 2,
                List.of(card("a0000000-0000-0000-0000-000000000007", "goyenda")),
                List.of(opponent(OPP, 3, 2)), null, null));
        assertThat(d.action()).isEqualTo(BotActionType.FOREIGN_AID);
    }

    @Test
    @DisplayName("Cautious bot falls back to income")
    void ownAction_cautiousIncome() {
        BotDecisionContext ctx = new BotDecisionContext(MATCH, BOT, "bot",
                "EASY", "BALANCED", BotDecisionType.OWN_ACTION, 3, false, 2, 2,
                List.of(card("a0000000-0000-0000-0000-000000000008", "goyenda")),
                List.of(opponent(OPP, 3, 2)), List.of(OPP), List.of(OPP), null, null, 20);
        BotDecision d = strategy.decide(ctx);
        assertThat(d.action()).isEqualTo(BotActionType.INCOME);
    }

    /* ---------------- Action challenge ---------------- */

    @Test
    @DisplayName("Challenge a claimed character when likely bluffing")
    void actionChallenge_challenge() {
        UUID goyenda = UUID.fromString("b0000000-0000-0000-0000-000000000001");
        BotDecision d = strategy.decide(context(BotDecisionType.ACTION_CHALLENGE, 2, 2,
                List.of(new BotDecisionContext.OwnCard(goyenda, "goyenda"),
                        card("b0000000-0000-0000-0000-000000000002", "minister")),
                List.of(opponent(OPP, 3, 2)),
                pending("TAX", "minister"), null));
        assertThat(d.action()).isEqualTo(BotActionType.CHALLENGE);
        assertThat(d.loserCardId()).isEqualTo(goyenda);
    }

    @Test
    @DisplayName("Pass on a challenge when unlikely to win")
    void actionChallenge_pass() {
        BotDecision d = strategy.decide(context(BotDecisionType.ACTION_CHALLENGE, 2, 2,
                List.of(card("b0000000-0000-0000-0000-000000000003", "goyenda")),
                List.of(opponent(OPP, 3, 2)),
                pending("STEAL", "dalal"), null));
        assertThat(d.action()).isEqualTo(BotActionType.PASS);
    }

    @Test
    @DisplayName("Never challenge an unchallengeable action")
    void actionChallenge_unChallengeablePass() {
        BotDecision d = strategy.decide(context(BotDecisionType.ACTION_CHALLENGE, 2, 2,
                List.of(card("b0000000-0000-0000-0000-000000000004", "goyenda")),
                List.of(opponent(OPP, 3, 2)),
                pending("FOREIGN_AID", null), null));
        assertThat(d.action()).isEqualTo(BotActionType.PASS);
    }

    /* ---------------- Action block ---------------- */

    @Test
    @DisplayName("Truthful minister block on foreign aid")
    void actionBlock_truthfulMinister() {
        BotDecision d = strategy.decide(context(BotDecisionType.ACTION_BLOCK, 2, 2,
                List.of(card("c0000000-0000-0000-0000-000000000001", "minister")),
                List.of(opponent(OPP, 3, 2)),
                pending("FOREIGN_AID", null), null));
        assertThat(d.action()).isEqualTo(BotActionType.BLOCK);
        assertThat(d.claimedCharacter()).isEqualTo("minister");
    }

    @Test
    @DisplayName("Pass on an illegal block for the action")
    void actionBlock_illegalPass() {
        BotDecision d = strategy.decide(context(BotDecisionType.ACTION_BLOCK, 2, 2,
                List.of(card("c0000000-0000-0000-0000-000000000002", "dalal")),
                List.of(opponent(OPP, 3, 2)),
                pending("ASSASSINATE", "ghatok"), null));
        assertThat(d.action()).isEqualTo(BotActionType.PASS);
    }

    /* ---------------- Block challenge ---------------- */

    @Test
    @DisplayName("Challenge a standing block claim when impossible")
    void blockChallenge_challenge() {
        BotDecisionContext.PendingFacts blocked = new BotDecisionContext.PendingFacts(
                UUID.randomUUID(), "FOREIGN_AID", OPP, null, BOT, OPP2, "goyenda",
                null, null);
        BotDecision d = strategy.decide(context(BotDecisionType.BLOCK_CHALLENGE, 2, 2,
                List.of(card("d0000000-0000-0000-0000-000000000001", "goyenda"),
                        card("d0000000-0000-0000-0000-000000000002", "amla")),
                List.of(opponent(OPP, 3, 2)),
                blocked, null));
        assertThat(d.action()).isEqualTo(BotActionType.CHALLENGE_BLOCK);
        assertThat(d.loserCardId()).isEqualTo(
                UUID.fromString("d0000000-0000-0000-0000-000000000001"));
    }

    @Test
    @DisplayName("Pass once a block challenge is already decided")
    void blockChallenge_alreadyDecided() {
        BotDecisionContext.PendingFacts blocked = new BotDecisionContext.PendingFacts(
                UUID.randomUUID(), "FOREIGN_AID", OPP, null, BOT, OPP2, "minister",
                null, BOT);
        BotDecision d = strategy.decide(context(BotDecisionType.BLOCK_CHALLENGE, 2, 2,
                List.of(card("d0000000-0000-0000-0000-000000000003", "goyenda")),
                List.of(opponent(OPP, 3, 2)),
                blocked, null));
        assertThat(d.action()).isEqualTo(BotActionType.PASS);
    }

    /* ---------------- Actor windows ---------------- */

    @Test
    @DisplayName("Exchange confirm keeps the two strongest pool cards")
    void exchangeConfirm_keepsTwoBest() {
        UUID ghatok = UUID.fromString("e0000000-0000-0000-0000-000000000001");
        UUID minister = UUID.fromString("e0000000-0000-0000-0000-000000000002");
        List<BotDecisionContext.PoolCard> pool = List.of(
                new BotDecisionContext.PoolCard(ghatok, "ghatok"),
                new BotDecisionContext.PoolCard(minister, "minister"),
                new BotDecisionContext.PoolCard(
                        UUID.fromString("e0000000-0000-0000-0000-000000000003"), "goyenda"),
                new BotDecisionContext.PoolCard(
                        UUID.fromString("e0000000-0000-0000-0000-000000000004"), "goyenda"));
        BotDecision d = strategy.decide(new BotDecisionContext(MATCH, BOT, "bot",
                "MEDIUM", "BALANCED", BotDecisionType.EXCHANGE_CONFIRM, 3, false, 2, 2,
                List.of(card("e0000000-0000-0000-0000-000000000005", "goyenda")),
                List.of(opponent(OPP, 3, 2)), List.of(OPP), List.of(OPP), null, pool, 20));
        assertThat(d.action()).isEqualTo(BotActionType.CONFIRM_EXCHANGE);
        assertThat(d.keepCardIds()).hasSize(2);
        assertThat(d.keepCardIds()).containsExactlyInAnyOrder(ghatok, minister);
    }

    @Test
    @DisplayName("Actor resolve is a plain resolve")
    void actorResolve_resolves() {
        BotDecision d = strategy.decide(context(BotDecisionType.ACTOR_RESOLVE, 2, 2,
                List.of(card("f0000000-0000-0000-0000-000000000001", "goyenda")),
                List.of(opponent(OPP, 3, 2)), null, null));
        assertThat(d.action()).isEqualTo(BotActionType.RESOLVE);
    }
}