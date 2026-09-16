package com.rajneeti.game;

import com.rajneeti.dto.game.GameStateResponse;
import com.rajneeti.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Module 20 — Action Resolver.
 *
 * <p>Closes the window of a pending action after every challenge (Module 18)
 * and block (Module 19) opportunity has passed. The outcome is derived entirely
 * server-side from the flags the Challenge/Block Managers recorded on the
 * pending action — the actor never supplies a {@code blocked}/{@code granted}/
 * {@code succeeded} boolean — and the actual coin/card mutation is delegated to
 * the existing authoritative resolve* seams on {@link GameEngine}, so no phase
 * or influence logic is duplicated here.
 *
 * <p>The single decision made here is whether the pending action is blocked:
 * <ul>
 *   <li>{@code pending.blockerUserId != null} — a standing block cancels the
 *       action ({@link BlockManager} kept the blocker because their claim was
 *       truthful, or the block fight is still standing until challenged);</li>
 *   <li>{@code pending.blockerUserId == null} — the action proceeds. This
 *       covers the untouched action, the bluffed block that was exposed (the
 *       blocker already lost an influence card, the block was removed, the
 *       action stays pending) and the challenge that merely proved a truthful
 *       claim (the challenger already lost an influence card).</li>
 * </ul>
 *
 * <p>Bluffed claims that are successfully challenged are dropped by
 * {@link ChallengeManager} at challenge time (the pending action is cancelled
 * and the turn advances), so any pending action that still exists here always
 * resolves — a bluffed claim can never "sneak through" to resolution.
 *
 * <p>Income and Coup never open a pending window (they resolve instantly), so
 * they can no longer be present at resolution time. Exchange is the special
 * case: its outcome still requires the actor to pick which cards to keep, which
 * only {@link GameEngine#confirmExchange} may decide, so a generic resolve of
 * an Exchange is rejected with {@code EXCHANGE_CARD_CHOICE_REQUIRED}.
 *
 * <p>Every closed action is recorded on {@link GameState#getLastActionResult()}
 * so the whole table can display the same authoritative verdict until the next
 * action is declared.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ActionResolver {

    private final GameEngine gameEngine;
    private final GameStateMapper gameStateMapper;
    private final WinnerManager winnerManager;

    /**
     * Resolves the currently pending action on behalf of its actor.
     *
     * @param matchId the match ID
     * @param userId  the actor's user ID (must match the pending action)
     * @return the updated player-safe game state (pending cleared, the verdict
     *         exposed as {@code lastActionResult})
     */
    @Transactional
    public GameStateResponse resolve(UUID matchId, UUID userId) {
        GameState state = gameEngine.getGameState(matchId);

        PendingAction pending = state.getPendingAction();
        if (pending == null) {
            throw new BusinessException("NO_PENDING_ACTION",
                    "No pending action to resolve.");
        }

        if (!userId.equals(pending.getActorUserId())) {
            throw new BusinessException("NOT_ACTOR",
                    "Only the action's actor can resolve the pending action.");
        }

        String type = pending.getType();
        switch (type) {
            case GameEngine.ACTION_FOREIGN_AID:
                resolveForeignAid(state, pending);
                break;
            case GameEngine.ACTION_TAX:
                resolveTax(state, pending);
                break;
            case GameEngine.ACTION_STEAL:
                resolveSteal(state, pending);
                break;
            case GameEngine.ACTION_ASSASSINATE:
                resolveAssassinate(state, pending);
                break;
            case GameEngine.ACTION_EXCHANGE:
                throw new BusinessException("EXCHANGE_CARD_CHOICE_REQUIRED",
                        "Exchange requires the actor to choose which cards to keep. "
                                + "Use /exchange/confirm instead.");
            default:
                throw new BusinessException("ACTION_NOT_RESOLVABLE",
                        "The action '" + type + "' cannot be resolved through the generic resolver.");
        }

        // Module 21 — after the action has fully resolved, hand over to the
        // Winner Manager. It is the single authority that decides whether the
        // last opponent was eliminated and finishes the match.
        winnerManager.checkAndFinish(state);

        return gameStateMapper.toResponse(state, pending.getActorUserId());
    }

    /**
     * Foreign Aid: 2 coins when no Minister block stands, 0 when a standing
     * block cancelled it. The turn advances regardless (handled by the seam).
     */
    private void resolveForeignAid(GameState state, PendingAction pending) {
        UUID actorId = pending.getActorUserId();
        boolean blocked = pending.getBlockerUserId() != null;

        int coinsBefore = coinsOf(state, actorId);
        int revealedBefore = state.getRevealedCardsCount();

        gameEngine.resolveForeignAid(state.getMatchId(), actorId, blocked);

        state.setLastActionResult(buildRecording(state, pending,
                blocked ? GameActionResult.RESULT_CANCELLED : GameActionResult.RESULT_RESOLVED,
                coinsOf(state, actorId) - coinsBefore,
                0, revealedBefore));

    }

    /**
     * Tax: a truthful Minister claim (unchallenged, or its challenge already
     * decided) always awards 3 coins here; a bluffed claim is dropped at the
     * challenge stage and never reaches this point.
     */
    private void resolveTax(GameState state, PendingAction pending) {
        UUID actorId = pending.getActorUserId();

        int coinsBefore = coinsOf(state, actorId);
        int revealedBefore = state.getRevealedCardsCount();

        gameEngine.resolveTax(state.getMatchId(), actorId, true);

        state.setLastActionResult(buildRecording(state, pending,
                GameActionResult.RESULT_RESOLVED,
                coinsOf(state, actorId) - coinsBefore,
                0, revealedBefore));

    }

    /**
     * Steal: a standing block cancels it (no transfer), otherwise up to 2 coins
     * move from the target to the actor (capped by the target's balance).
     */
    private void resolveSteal(GameState state, PendingAction pending) {
        UUID actorId = pending.getActorUserId();
        UUID targetId = pending.getTargetPlayerId();
        boolean blocked = pending.getBlockerUserId() != null;

        int actorCoinsBefore = coinsOf(state, actorId);
        int targetCoinsBefore = coinsOf(state, targetId);
        int revealedBefore = state.getRevealedCardsCount();

        gameEngine.resolveSteal(state.getMatchId(), actorId, !blocked);

        state.setLastActionResult(buildRecording(state, pending,
                blocked ? GameActionResult.RESULT_CANCELLED : GameActionResult.RESULT_RESOLVED,
                coinsOf(state, actorId) - actorCoinsBefore,
                targetCoinsBefore - coinsOf(state, targetId),
                revealedBefore));

    }

    /**
     * Assassinate: a standing block cancels it (no coins paid, no card lost);
     * otherwise the 3 reserved coins are deducted and the target loses exactly
     * one influence card, eliminating them if that was their last card.
     */
    private void resolveAssassinate(GameState state, PendingAction pending) {
        UUID actorId = pending.getActorUserId();
        UUID targetId = pending.getTargetPlayerId();
        boolean blocked = pending.getBlockerUserId() != null;

        int coinsBefore = coinsOf(state, actorId);
        int revealedBefore = state.getRevealedCardsCount();

        gameEngine.resolveAssassinate(state.getMatchId(), actorId, !blocked);

        state.setLastActionResult(buildRecording(state, pending,
                blocked ? GameActionResult.RESULT_CANCELLED : GameActionResult.RESULT_RESOLVED,
                coinsOf(state, actorId) - coinsBefore,
                0, revealedBefore));

    }

    /**
     * Builds the single authoritative recording of what the resolution just did,
     * reading the (already mutated) state so the coins/influence deltas are the
     * de-facto transfer that happened.
     */
    private GameActionResult buildRecording(GameState state, PendingAction pending,
                                            String result, int coinsGained, int coinsLost,
                                            int revealedBefore) {
        UUID targetId = pending.getTargetPlayerId();

        UUID influenceLostBy = state.getRevealedCardsCount() > revealedBefore ? targetId : null;
        boolean eliminated = influenceLostBy != null
                && isEliminated(state, influenceLostBy);
        boolean blocked = pending.getBlockerUserId() != null;

        return GameActionResult.builder()
                .actionType(pending.getType())
                .result(result)
                .actorUserId(pending.getActorUserId())
                .coinsGained(coinsGained)
                .coinsLost(coinsLost)
                .blockedByUserId(blocked ? pending.getBlockerUserId() : null)
                .blockedCharacter(blocked ? pending.getBlockedCharacter() : null)
                .claimChallenged(pending.getChallengerUserId() != null)
                .influenceLostById(influenceLostBy)
                .eliminated(eliminated)
                .nextTurnPlayerId(state.getCurrentTurnPlayerId())
                .nextTurnNumber(state.getTurnNumber())
                .build();
    }

    private int coinsOf(GameState state, UUID userId) {
        return state.getPlayers().stream()
                .filter(p -> p.getUserId().equals(userId))
                .findFirst()
                .map(GamePlayerState::getCoins)
                .orElse(0);
    }

    private boolean isEliminated(GameState state, UUID userId) {
        return state.getPlayers().stream()
                .filter(p -> p.getUserId().equals(userId))
                .findFirst()
                .map(p -> p.getStatus() == com.rajneeti.entity.enums.PlayerStatus.ELIMINATED)
                .orElse(false);
    }
}