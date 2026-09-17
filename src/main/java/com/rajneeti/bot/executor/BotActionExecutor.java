package com.rajneeti.bot.executor;

import com.rajneeti.bot.decision.BotActionType;
import com.rajneeti.bot.decision.BotDecision;
import com.rajneeti.exception.BusinessException;
import com.rajneeti.game.ActionResolver;
import com.rajneeti.game.BlockManager;
import com.rajneeti.game.ChallengeManager;
import com.rajneeti.game.GameEngine;
import com.rajneeti.game.GameState;
import com.rajneeti.game.PendingAction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Module 25 — executes a bot's decision through the SAME engine seams a human
 * would use (GameEngine / ChallengeManager / BlockManager / ActionResolver).
 * No game rules are implemented here; the executor only maps a
 * {@link BotDecision} onto the matching public engine call and reports whether
 * the call was accepted.
 *
 * <p><b>Stale-guards</b> re-validate the live state right before each call so a
 * bot can never act on an outdated snapshot: a RESOLVE only runs while the same
 * pending action still exists, a CHALLENGE never sneaks in and turns into a
 * block-challenge when a block is standing, and every action route is double
 * checked against turn ownership before invoking the (also-validating) engine.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BotActionExecutor {

    private final GameEngine gameEngine;
    private final ChallengeManager challengeManager;
    private final BlockManager blockManager;
    private final ActionResolver actionResolver;

    /**
     * Outcome of an execution attempt.
     *
     * @param accepted  true when the engine/manager consumed the decision
     * @param windowKey the dedup-key of the decision window (for the driver)
     * @param detail    error code / free text for diagnostics
     */
    public record ExecutionOutcome(boolean accepted, String detail) {
    }

    public ExecutionOutcome execute(UUID matchId, UUID botId, BotDecision decision) {
        try {
            switch (decision.action()) {
                case PASS:
                    return new ExecutionOutcome(true, "NOOP");
                case INCOME:
                    gameEngine.performIncome(matchId, botId);
                    return new ExecutionOutcome(true, null);
                case FOREIGN_AID:
                    gameEngine.performForeignAid(matchId, botId);
                    return new ExecutionOutcome(true, null);
                case TAX:
                    gameEngine.performTax(matchId, botId);
                    return new ExecutionOutcome(true, null);
                case STEAL:
                    requireTarget(decision);
                    gameEngine.performSteal(matchId, botId, decision.targetPlayerId());
                    return new ExecutionOutcome(true, null);
                case ASSASSINATE:
                    requireTarget(decision);
                    gameEngine.performAssassinate(matchId, botId, decision.targetPlayerId());
                    return new ExecutionOutcome(true, null);
                case EXCHANGE:
                    gameEngine.performExchange(matchId, botId);
                    return new ExecutionOutcome(true, null);
                case COUP:
                    requireTarget(decision);
                    gameEngine.performCoup(matchId, botId, decision.targetPlayerId());
                    return new ExecutionOutcome(true, null);
                case CHALLENGE:
                    if (!actionChallengeOpen(matchId, botId)) {
                        return new ExecutionOutcome(false, "STALE_CHALLENGE_WINDOW");
                    }
                    challengeManager.challenge(matchId, botId, decision.loserCardId());
                    return new ExecutionOutcome(true, null);
                case CHALLENGE_BLOCK:
                    if (!blockChallengeOpen(matchId, botId)) {
                        return new ExecutionOutcome(false, "STALE_BLOCK_CHALLENGE_WINDOW");
                    }
                    challengeManager.challenge(matchId, botId, decision.loserCardId());
                    return new ExecutionOutcome(true, null);
                case BLOCK:
                    if (!blockOpen(matchId, botId)) {
                        return new ExecutionOutcome(false, "STALE_BLOCK_WINDOW");
                    }
                    blockManager.block(matchId, botId, decision.claimedCharacter());
                    return new ExecutionOutcome(true, null);
                case RESOLVE:
                    if (!actorWindowOpen(matchId, botId)) {
                        return new ExecutionOutcome(false, "STALE_ACTOR_WINDOW");
                    }
                    actionResolver.resolve(matchId, botId);
                    return new ExecutionOutcome(true, null);
                case CONFIRM_EXCHANGE:
                    if (!exchangeConfirmOpen(matchId, botId)) {
                        return new ExecutionOutcome(false, "STALE_EXCHANGE_WINDOW");
                    }
                    gameEngine.confirmExchange(matchId, botId, decision.keepCardIds());
                    return new ExecutionOutcome(true, null);
                default:
                    return new ExecutionOutcome(false, "UNSUPPORTED_ACTION");
            }
        } catch (BusinessException ex) {
            log.debug("Bot decision rejected by engine ({}): {}", ex.getErrorCode(),
                    ex.getMessage());
            return new ExecutionOutcome(false, ex.getErrorCode());
        } catch (RuntimeException ex) {
            log.warn("Bot decision execution failed in match {}", matchId, ex);
            return new ExecutionOutcome(false, "INTERNAL_ERROR");
        }
    }

    private void requireTarget(BotDecision decision) {
        if (decision.targetPlayerId() == null) {
            throw new BusinessException("TARGET_REQUIRED",
                    "Targeted actions need an explicit target.");
        }
    }

    /** Actor resolve stays open only while the very same pending action exists. */
    private boolean actorWindowOpen(UUID matchId, UUID botId) {
        GameState state = gameEngine.getGameState(matchId);
        PendingAction pending = state.getPendingAction();
        return pending != null
                && botId.equals(pending.getActorUserId())
                && !"EXCHANGE".equals(pending.getType());
    }

    private boolean exchangeConfirmOpen(UUID matchId, UUID botId) {
        GameState state = gameEngine.getGameState(matchId);
        PendingAction pending = state.getPendingAction();
        return pending != null
                && botId.equals(pending.getActorUserId())
                && "EXCHANGE".equals(pending.getType());
    }

    /**
     * A plain action challenge is only valid while no block stands (otherwise
     * the ChallengeManager would resolve the BLOCK claim instead).
     */
    private boolean actionChallengeOpen(UUID matchId, UUID botId) {
        GameState state = gameEngine.getGameState(matchId);
        PendingAction pending = state.getPendingAction();
        if (pending == null || botId.equals(pending.getActorUserId())) {
            return false;
        }
        return pending.getChallengerUserId() == null
                && !(pending.getBlockerUserId() != null && pending.getBlockChallengerUserId() == null);
    }

    private boolean blockChallengeOpen(UUID matchId, UUID botId) {
        GameState state = gameEngine.getGameState(matchId);
        PendingAction pending = state.getPendingAction();
        if (pending == null || botId.equals(pending.getActorUserId())
                || botId.equals(pending.getBlockerUserId())) {
            return false;
        }
        return pending.getBlockerUserId() != null
                && pending.getBlockChallengerUserId() == null;
    }

    private boolean blockOpen(UUID matchId, UUID botId) {
        GameState state = gameEngine.getGameState(matchId);
        PendingAction pending = state.getPendingAction();
        if (pending == null || botId.equals(pending.getActorUserId())) {
            return false;
        }
        return pending.getBlockerUserId() == null
                && pending.getChallengerUserId() == null
                && pending.getBlockChallengerUserId() == null;
    }
}