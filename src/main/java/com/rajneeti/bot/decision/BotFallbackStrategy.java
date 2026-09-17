package com.rajneeti.bot.decision;

import com.rajneeti.bot.BotDifficulty;
import com.rajneeti.bot.BotPersonality;
import com.rajneeti.game.GameEngine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Module 25 — heuristic decision strategy used when the LLM is disabled,
 * unreachable, unconfigured, or returns something illegal.
 *
 * <p>Every decision it produces is validated against the context built from the
 * live game state, so under the single-driver model the fallback's answers are
 * legal by construction: it never proposes a Coupon without coins, a Steal on a
 * coin-less target, a Block with an illegal blocker, or a challenge with no
 * staked card. This keeps a pure-bot game progressing deterministically even
 * with zero AI keys configured.
 */
@Slf4j
@Component
public class BotFallbackStrategy implements BotDecisionProvider {

    private static final int CARD_SCORE_MINISTER = 3;
    private static final int CARD_SCORE_DALAL = 3;
    private static final int CARD_SCORE_AMLA = 3;
    private static final int CARD_SCORE_GHATOK = 2;
    private static final int CARD_SCORE_GOYENDA = 1;

    @Override
    public BotDecision decide(BotDecisionContext context) {
        return switch (context.decisionType()) {
            case OWN_ACTION -> decideOwnAction(context);
            case ACTION_CHALLENGE -> decideActionChallenge(context);
            case ACTION_BLOCK -> decideActionBlock(context);
            case BLOCK_CHALLENGE -> decideBlockChallenge(context);
            case ACTOR_RESOLVE -> BotDecision.resolve(BotDecisionType.ACTOR_RESOLVE);
            case EXCHANGE_CONFIRM -> decideExchangeConfirm(context);
        };
    }

    /* ------------------------------------------------------------------ */
    /*  Own action                                                        */
    /* ------------------------------------------------------------------ */

    private BotDecision decideOwnAction(BotDecisionContext ctx) {
        if (ctx.mandatoryCoup()) {
            return new BotDecision(ctx.decisionType(), BotActionType.COUP,
                    firstTarget(ctx.legalTargets()), null, null, null, "Mandatory coup");
        }

        double aggression = contextAggression(ctx);

        // 1. Finishing coup: rich enough and someone is one card away from death.
        if (ctx.ownCoins() >= 7 && hasOneCardOpponent(ctx) && aggression >= 1.0) {
            return new BotDecision(ctx.decisionType(), BotActionType.COUP,
                    weakestOpponent(ctx), null, null, null, "Finishing coup");
        }

        // 2. Truthful tax with a minister in hand (challenge-proof, +3 coins).
        if (ctx.ownCards().stream().anyMatch(c -> c.character().equals("minister"))) {
            return new BotDecision(ctx.decisionType(), BotActionType.TAX, null, "minister",
                    null, null, "Truthful tax");
        }

        // 3. Steal from the richest target — truthful with dalal, or a bluff when aggressive.
        if (!ctx.richTargets().isEmpty() && ctx.ownCoins() >= 2
                && (ownCard(ctx, "dalal") || aggression >= 1.1)) {
            return new BotDecision(ctx.decisionType(), BotActionType.STEAL,
                    richestOpponent(ctx), "dalal", null, null, "Steal from the richest");
        }

        // 4. Exchange refreshes the hand (truthful when holding amla) — but only
        //    while both the deck supply and the bot's influence allow the draw.
        if (ownCard(ctx, "amla") && ctx.deckCount() >= GameEngine.EXCHANGE_DRAW
                && ctx.ownInfluenceCount() >= GameEngine.STARTING_INFLUENCE) {
            return new BotDecision(ctx.decisionType(), BotActionType.EXCHANGE, null, "amla",
                    null, null, "Refresh hand with exchange");
        }

        // 5. Assassinate a fragile opponent when affordable (the replacement
        //    draw also needs the deck to hold at least one card).
        if (ctx.ownCoins() >= 3 && ownCard(ctx, "ghatok") && !ctx.legalTargets().isEmpty()
                && ctx.deckCount() >= 1) {
            return new BotDecision(ctx.decisionType(), BotActionType.ASSASSINATE,
                    weakestOpponent(ctx), "ghatok", null, null, "Truthful assassination");
        }

        // 6. Aggressive bluff-steal.
        if (!ctx.richTargets().isEmpty() && ctx.ownCoins() >= 2 && aggression >= 1.3) {
            return new BotDecision(ctx.decisionType(), BotActionType.STEAL,
                    richestOpponent(ctx), "dalal", null, null, "Bluff steal");
        }

        // 7. Foreign aid (2 coins, minister-blockable) or safe income.
        if (aggression >= 1.0) {
            return new BotDecision(ctx.decisionType(), BotActionType.FOREIGN_AID, null, null,
                    null, null, "Collect foreign aid");
        }
        return new BotDecision(ctx.decisionType(), BotActionType.INCOME, null, null,
                null, null, "Safe income");
    }

    /* ------------------------------------------------------------------ */
    /*  Opponent windows                                                   */
    /* ------------------------------------------------------------------ */

    private BotDecision decideActionChallenge(BotDecisionContext ctx) {
        if (ctx.pendingAction() == null) {
            return BotDecision.pass(ctx.decisionType());
        }
        String claimed = ctx.pendingAction().claimedCharacter();
        if (!isChallengeable(ctx.pendingAction().type()) || claimed == null) {
            return BotDecision.pass(ctx.decisionType());
        }

        // Holding the claimed character yourself makes a bluff far more likely.
        boolean holdsClaim = ctx.ownCards().stream()
                .anyMatch(c -> c.character().equals(claimed));
        double chance = holdsClaim ? 0.75 : 0.25;
        chance *= ctxChallengeFactor(ctx);
        if (ctx.ownInfluenceCount() <= 1) {
            chance *= 0.3;
        }

        if (chance >= 0.28) {
            return new BotDecision(ctx.decisionType(), BotActionType.CHALLENGE,
                    null, null, null, weakestCardId(ctx),
                    "Challenging the claim of " + claimed);
        }
        return BotDecision.pass(ctx.decisionType());
    }

    private BotDecision decideActionBlock(BotDecisionContext ctx) {
        if (ctx.pendingAction() == null) {
            return BotDecision.pass(ctx.decisionType());
        }
        List<String> legalBlockers = legalBlockersFor(ctx.pendingAction().type());
        if (legalBlockers.isEmpty()) {
            return BotDecision.pass(ctx.decisionType());
        }

        // A truthful block is risk-free for the blocker: pick it when possible.
        for (String character : legalBlockers) {
            if (ownCard(ctx, character)) {
                return new BotDecision(ctx.decisionType(), BotActionType.BLOCK,
                        null, character, null, null,
                        "Truthful block with " + character);
            }
        }

        // Bluff block only when the personality is bluffy/aggressive enough.
        double bluff = ctxBluffFactor(ctx);
        if (bluff >= 1.1) {
            String character = legalBlockers.get(0);
            return new BotDecision(ctx.decisionType(), BotActionType.BLOCK,
                    null, character, null, null,
                    "Bluff block with " + character);
        }
        return BotDecision.pass(ctx.decisionType());
    }

    private BotDecision decideBlockChallenge(BotDecisionContext ctx) {
        if (ctx.pendingAction() == null || ctx.pendingAction().blockerUserId() == null
                || ctx.pendingAction().blockChallengerUserId() != null) {
            return BotDecision.pass(ctx.decisionType());
        }
        String blocked = ctx.pendingAction().blockedCharacter();
        if (blocked == null) {
            return BotDecision.pass(ctx.decisionType());
        }

        boolean holdsBlocked = ctx.ownCards().stream()
                .anyMatch(c -> c.character().equals(blocked));
        double chance = (holdsBlocked ? 0.5 : 0.2) * ctxChallengeFactor(ctx);
        if (ctx.ownInfluenceCount() <= 1) {
            chance *= 0.3;
        }

        if (chance >= 0.28) {
            return new BotDecision(ctx.decisionType(), BotActionType.CHALLENGE_BLOCK,
                    null, null, null, weakestCardId(ctx),
                    "Challenging the block claim of " + blocked);
        }
        return BotDecision.pass(ctx.decisionType());
    }

    /* ------------------------------------------------------------------ */
    /*  Actor windows                                                      */
    /* ------------------------------------------------------------------ */

    private BotDecision decideExchangeConfirm(BotDecisionContext ctx) {
        List<BotDecisionContext.PoolCard> pool = ctx.exchangePool();
        if (pool == null || pool.size() != 4) {
            return BotDecision.pass(ctx.decisionType());
        }

        List<BotDecisionContext.PoolCard> best = pool.stream()
                .sorted(Comparator
                        .comparingInt((BotDecisionContext.PoolCard c) -> cardScore(c.character()))
                        .reversed()
                        .thenComparing(c -> c.character()))
                .limit(2)
                .toList();

        return new BotDecision(ctx.decisionType(), BotActionType.CONFIRM_EXCHANGE,
                null, null,
                best.stream().map(BotDecisionContext.PoolCard::cardId).toList(),
                null, "Keeping the two strongest cards");
    }

    /* ------------------------------------------------------------------ */
    /*  Helpers                                                            */
    /* ------------------------------------------------------------------ */

    private boolean ownCard(BotDecisionContext ctx, String character) {
        return ctx.ownCards().stream().anyMatch(c -> c.character().equals(character));
    }

    private int cardScore(String character) {
        return switch (character) {
            case "minister" -> CARD_SCORE_MINISTER;
            case "dalal" -> CARD_SCORE_DALAL;
            case "amla" -> CARD_SCORE_AMLA;
            case "ghatok" -> CARD_SCORE_GHATOK;
            default -> CARD_SCORE_GOYENDA;
        };
    }

    private boolean isChallengeable(String actionType) {
        return switch (actionType) {
            case "TAX", "STEAL", "EXCHANGE", "ASSASSINATE" -> true;
            default -> false;
        };
    }

    private List<String> legalBlockersFor(String actionType) {
        return switch (actionType) {
            case "FOREIGN_AID" -> List.of("minister");
            case "STEAL" -> List.of("dalal", "amla");
            case "ASSASSINATE" -> List.of("goyenda");
            default -> List.of();
        };
    }

    private boolean hasOneCardOpponent(BotDecisionContext ctx) {
        return ctx.opponents().stream().anyMatch(o -> o.influenceCount() == 1);
    }

    private UUID weakestOpponent(BotDecisionContext ctx) {
        return ctx.opponents().stream()
                .filter(o -> ctx.legalTargets().contains(o.userId()))
                .min(Comparator
                        .comparingInt(BotDecisionContext.OpponentView::influenceCount)
                        .thenComparing(BotDecisionContext.OpponentView::coins))
                .map(BotDecisionContext.OpponentView::userId)
                .orElse(null);
    }

    private UUID richestOpponent(BotDecisionContext ctx) {
        return ctx.opponents().stream()
                .filter(o -> ctx.richTargets().contains(o.userId()))
                .max(Comparator.comparingInt(BotDecisionContext.OpponentView::coins))
                .map(BotDecisionContext.OpponentView::userId)
                .orElse(null);
    }

    private UUID firstTarget(List<UUID> targets) {
        return targets == null || targets.isEmpty() ? null : targets.get(0);
    }

    private UUID weakestCardId(BotDecisionContext ctx) {
        return ctx.ownCards().stream()
                .min(Comparator
                        .comparingInt((BotDecisionContext.OwnCard c) -> cardScore(c.character()))
                        .thenComparing(c -> c.cardId().toString()))
                .map(BotDecisionContext.OwnCard::cardId)
                .orElse(null);
    }

    private double contextAggression(BotDecisionContext ctx) {
        return difficultyFactor(ctx) * personality(ctx).getAggressionFactor();
    }

    private double ctxChallengeFactor(BotDecisionContext ctx) {
        return difficultyFactor(ctx) * personality(ctx).getChallengeFactor();
    }

    private double ctxBluffFactor(BotDecisionContext ctx) {
        return personality(ctx).getBluffFactor();
    }

    private double difficultyFactor(BotDecisionContext ctx) {
        return switch (BotDifficulty.fromNullable(ctx.difficulty())) {
            case EASY -> 0.7;
            case MEDIUM -> 1.0;
            case HARD -> 1.3;
        };
    }

    private BotPersonality personality(BotDecisionContext ctx) {
        return BotPersonality.fromNullable(ctx.personality());
    }
}