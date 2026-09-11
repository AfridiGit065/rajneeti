package com.rajneeti.game;

import com.rajneeti.dto.game.GameStateResponse;
import com.rajneeti.entity.enums.MatchStatus;
import com.rajneeti.entity.enums.PlayerStatus;
import com.rajneeti.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Module 19 — Block Manager.
 *
 * <p>Records a block claim against a blockable pending action. A block is a
 * character claim like any other: the blocker asserts they hold the character
 * that counteracts the action, ownership is NOT verified up front (a bluff is
 * allowed), and a later block challenge decides the truth. The decision of
 * whether the action actually gets blocked is made entirely server-side by the
 * {@link ChallengeManager}.
 *
 * <p>Blockable actions and the characters that can block them:
 * <ul>
 *   <li>FOREIGN_AID — blocked by {@code minister}</li>
 *   <li>STEAL — blocked by {@code dalal} or {@code amla}</li>
 *   <li>ASSASSINATE — blocked by {@code goyenda}</li>
 * </ul>
 *
 * <p>Income, Tax, Exchange and Coup are never blockable. Only one block is
 * accepted per pending action; a second block attempt is rejected with
 * {@code DUPLICATE_BLOCK}. The blocker must be an active member of the match
 * who is not the action's actor.
 *
 * <p>This manager only records the block and the resulting state transitions;
 * the final coin/card resolution is performed by the existing resolve* seams
 * on {@link GameEngine} once the actor resolves the (possibly blocked) action.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BlockManager {

    /** The characters able to block each action type. Keys are {@link GameEngine} action constants. */
    private static final Map<String, Set<String>> BLOCKABLE_BY = Map.of(
            GameEngine.ACTION_FOREIGN_AID, Set.of(GameEngine.CHARACTER_MINISTER),
            GameEngine.ACTION_STEAL, Set.of(GameEngine.CHARACTER_DALAL, GameEngine.CHARACTER_AMLA),
            GameEngine.ACTION_ASSASSINATE, Set.of(GameEngine.CHARACTER_GOYENDA));

    private final GameEngine gameEngine;

    /**
     * Records a block claim by {@code blockerId} against the currently pending
     * blockable action.
     *
     * @param matchId          the match ID
     * @param blockerId        the user ID of the player submitting the block
     * @param claimedCharacter the lower-case character id the blocker asserts
     *                         (must be a valid blocker for the action type)
     * @return the updated player-safe game state from the blocker's perspective
     */
    @Transactional
    public GameStateResponse block(UUID matchId, UUID blockerId, String claimedCharacter) {
        GameState state = gameEngine.getOrInitialize(matchId);

        if (state.getStatus() != MatchStatus.IN_PROGRESS
                && state.getStatus() != MatchStatus.CREATED) {
            throw new BusinessException("MATCH_NOT_ACTIVE",
                    "Cannot block: match is not active.");
        }

        PendingAction pending = state.getPendingAction();
        if (pending == null) {
            throw new BusinessException("NO_PENDING_ACTION",
                    "No pending action to block.");
        }

        if (!BLOCKABLE_BY.containsKey(pending.getType())) {
            throw new BusinessException("ACTION_NOT_BLOCKABLE",
                    "The action '" + pending.getType() + "' cannot be blocked.");
        }

        if (claimedCharacter == null || claimedCharacter.isBlank()) {
            throw new BusinessException("INVALID_BLOCKING_CHARACTER",
                    "A blocking character must be claimed.");
        }
        String normalized = claimedCharacter.toLowerCase();
        if (!BLOCKABLE_BY.get(pending.getType()).contains(normalized)) {
            throw new BusinessException("INVALID_BLOCKING_CHARACTER",
                    "The character '" + claimedCharacter + "' cannot block the action '"
                            + pending.getType() + "'.");
        }

        GamePlayerState blocker = state.getPlayers().stream()
                .filter(p -> p.getUserId().equals(blockerId))
                .findFirst()
                .orElseThrow(() -> new BusinessException("PLAYER_NOT_IN_MATCH",
                        "Cannot block: the player is not part of this match."));

        if (blocker.getStatus() != PlayerStatus.ACTIVE) {
            throw new BusinessException("PLAYER_ELIMINATED",
                    "Eliminated players cannot block.");
        }

        if (blockerId.equals(pending.getActorUserId())) {
            throw new BusinessException("CANNOT_BLOCK_SELF",
                    "A player cannot block their own action.");
        }

        if (pending.getBlockerUserId() != null) {
            throw new BusinessException("DUPLICATE_BLOCK",
                    "This action has already been blocked. Only one block is allowed per action.");
        }

        PendingAction updated = PendingAction.builder()
                .type(pending.getType())
                .actorUserId(pending.getActorUserId())
                .startedAt(pending.getStartedAt())
                .claimedCharacter(pending.getClaimedCharacter())
                .exchangePool(pending.getExchangePool())
                .originalHandCardIds(pending.getOriginalHandCardIds())
                .targetPlayerId(pending.getTargetPlayerId())
                .reservedCoins(pending.getReservedCoins())
                .challengerUserId(pending.getChallengerUserId())
                .blockerUserId(blockerId)
                .blockedCharacter(normalized)
                .build();
        state.setPendingAction(updated);
        state.getLog().add(GameLogEntry.of("block",
                blocker.getUsername() + " claimed " + normalized
                        + " to block the " + pending.getType().toLowerCase() + " (block claim open to challenge)."));

        log.info("Player '{}' claimed {} to block '{}' in match {}",
                blocker.getUsername(), normalized, pending.getType(), matchId);

        return gameEngine.getSafeGameState(matchId, blockerId);
    }
}