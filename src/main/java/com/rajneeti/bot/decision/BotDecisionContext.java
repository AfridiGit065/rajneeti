package com.rajneeti.bot.decision;

import com.rajneeti.game.GamePlayerState;
import com.rajneeti.game.GameState;
import com.rajneeti.game.PendingAction;
import com.rajneeti.entity.enums.PlayerStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Module 25 — the (serializable) world a bot sees when it needs to decide.
 *
 * <p>This is the fairness boundary: it contains the bot's own private cards (the
 * only private information ever handed to the bot's brain) plus strictly public
 * facts — opponent coins, influence counts, and whatever the pending action
 * exposes. It never contains another player's cards, the deck order, or the
 * exchange pool of an opponent.
 *
 * <p>The context is serialized to JSON for the LLM provider and consumed
 * directly by the heuristic fallback, so both brains reason over the exact same
 * view of the game.
 */
public record BotDecisionContext(
        UUID matchId,
        UUID botUserId,
        String botUsername,
        String difficulty,
        String personality,
        BotDecisionType decisionType,
        int turnNumber,
        boolean mandatoryCoup,
        int ownCoins,
        int ownInfluenceCount,
        List<OwnCard> ownCards,
        List<OpponentView> opponents,
        List<UUID> legalTargets,
        List<UUID> richTargets,
        PendingFacts pendingAction,
        List<PoolCard> exchangePool,
        int deckCount) {

    /** The bot's own private cards. Safe: only the bot ever receives these. */
    public record OwnCard(UUID cardId, String character) {
    }

    /** Public projection of an opponent: no character information whatsoever. */
    public record OpponentView(
            UUID userId,
            String username,
            int coins,
            int influenceCount,
            boolean isBot,
            String botDifficulty) {
    }

    /** Public facts of the currently pending action (if any). */
    public record PendingFacts(
            UUID id,
            String type,
            UUID actorUserId,
            String claimedCharacter,
            UUID targetPlayerId,
            UUID blockerUserId,
            String blockedCharacter,
            UUID challengerUserId,
            UUID blockChallengerUserId) {
    }

    /** A single card of the actor's private exchange pool (EXCHANGE_CONFIRM only). */
    public record PoolCard(UUID cardId, String character) {
    }

    /**
     * Builds the decision context for one bot from the live game state. All
     * opponent information is derived from public state; only the bot's own
     * hand / exchange pool is included.
     */
    public static BotDecisionContext create(GameState state, GamePlayerState bot, BotDecisionType type) {
        PendingAction pending = state.getPendingAction();

        List<UUID> legalTargets = new ArrayList<>();
        List<UUID> richTargets = new ArrayList<>();
        List<OpponentView> opponents = new ArrayList<>();
        for (GamePlayerState other : state.getPlayers()) {
            if (other.getUserId().equals(bot.getUserId())) {
                continue;
            }
            if (other.getStatus() != PlayerStatus.ACTIVE) {
                continue;
            }
            legalTargets.add(other.getUserId());
            if (other.getCoins() > 0) {
                richTargets.add(other.getUserId());
            }
            opponents.add(new OpponentView(other.getUserId(), other.getUsername(),
                    other.getCoins(), other.getCards() != null ? other.getCards().size() : 0,
                    other.isBot(), other.getBotDifficulty()));
        }

        PendingFacts pendingFacts = null;
        if (pending != null) {
            pendingFacts = new PendingFacts(
                    pending.getId(),
                    pending.getType(),
                    pending.getActorUserId(),
                    pending.getClaimedCharacter(),
                    pending.getTargetPlayerId(),
                    pending.getBlockerUserId(),
                    pending.getBlockedCharacter(),
                    pending.getChallengerUserId(),
                    pending.getBlockChallengerUserId());
        }

        List<PoolCard> pool = null;
        if (pending != null
                && type == BotDecisionType.EXCHANGE_CONFIRM
                && bot.getUserId().equals(pending.getActorUserId())
                && pending.getExchangePool() != null) {
            pool = pending.getExchangePool().stream()
                    .map(card -> new PoolCard(card.getId(), card.getCharacter().name().toLowerCase()))
                    .toList();
        }

        List<OwnCard> ownCards = (bot.getCards() == null)
                ? List.of()
                : bot.getCards().stream()
                        .map(card -> new OwnCard(card.getId(), card.getCharacter().name().toLowerCase()))
                        .toList();

        return new BotDecisionContext(
                state.getMatchId(),
                bot.getUserId(),
                bot.getUsername(),
                bot.getBotDifficulty(),
                bot.getBotPersonality(),
                type,
                state.getTurnNumber() != null ? state.getTurnNumber() : 0,
                bot.getCoins() >= com.rajneeti.game.GameEngine.FORCED_COUP_THRESHOLD,
                bot.getCoins(),
                bot.getCards() != null ? bot.getCards().size() : 0,
                ownCards,
                List.copyOf(opponents),
                List.copyOf(legalTargets),
                List.copyOf(richTargets),
                pendingFacts,
                pool != null ? List.copyOf(pool) : null,
                state.getDeck() != null ? state.getDeck().size() : 0);
    }
}