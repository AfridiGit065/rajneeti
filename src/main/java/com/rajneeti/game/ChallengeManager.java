package com.rajneeti.game;

import com.rajneeti.dto.game.ChallengeDto;
import com.rajneeti.dto.game.GameStateResponse;
import com.rajneeti.entity.Match;
import com.rajneeti.entity.enums.MatchStatus;
import com.rajneeti.entity.enums.PlayerStatus;
import com.rajneeti.exception.BusinessException;
import com.rajneeti.service.TurnManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Module 18 — Challenge Manager.
 *
 * <p>Resolves challenges against the in-memory {@link GameState}, which is the
 * authoritative owner of every hidden influence card. The backend alone decides
 * whether a claim is truthful, who loses influence, which card is revealed and
 * whether the original action continues — the client never supplies card
 * ownership.
 *
 * <p>Challengeable claims: TAX claims {@code minister}, STEAL claims
 * {@code dalal}, EXCHANGE claims {@code amla}, ASSASSINATE claims
 * {@code ghatok}. Income, Foreign Aid and Coup carry no claim and are not
 * challengeable.
 *
 * <p>Flow per challenge:
 * <ul>
 *   <li>TRUTHFUL claim: the challenger loses exactly one influence, the
 *       claimant reveals the claimed card, the card returns to the deck and a
 *       replacement is drawn (hand size preserved), and the original action
 *       continues through its normal resolution seam.</li>
 *   <li>BLUFF claim: the claimant loses exactly one influence and the action is
 *       cancelled — no reward/effect is applied and no reserved coins are
 *       deducted.</li>
 * </ul>
 *
 * <p>Only one challenge is accepted per pending action; a second challenge is
 * rejected with {@code DUPLICATE_CHALLENGE}. A challenger must be an eligible,
 * non-eliminated opponent of the claimant.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChallengeManager {

    /** The four claim-based action types that can be challenged. */
    private static final Set<String> CHALLENGEABLE_ACTIONS = Set.of(
            GameEngine.ACTION_TAX,
            GameEngine.ACTION_STEAL,
            GameEngine.ACTION_EXCHANGE,
            GameEngine.ACTION_ASSASSINATE);

    private static final String RESULT_CLAIM_TRUE = "CLAIM_TRUE";
    private static final String RESULT_CLAIM_FALSE = "CLAIM_FALSE";

    private final GameEngine gameEngine;
    private final CardManager cardManager;
    private final TurnManager turnManager;

    /**
     * Resolves a challenge raised by {@code challengerId} against the currently
     * pending claim.
     *
     * <p>The resolution is fully server-side and atomic. For a truthful claim the
     * pending action stays open (now marked as challenged) so the actor can
     * proceed with the original action's normal resolution seam; for a bluff the
     * pending action is cleared, the action is cancelled and the turn advances.
     *
     * @param matchId      the match ID
     * @param challengerId the user ID of the challenging opponent
     * @param loserCardId  optional physical card ID the challenger loses/stakes if
     *                     the claim turns out truthful (must be one of the
     *                     challenger's own cards); when absent the first card of
     *                     the challenger's hand is removed, matching the existing
     *                     card-loss design
     * @return the updated player-safe game state from the challenger's perspective
     */
    @Transactional
    public GameStateResponse challenge(UUID matchId, UUID challengerId, UUID loserCardId) {
        GameState state = gameEngine.getOrInitialize(matchId);

        if (state.getStatus() != MatchStatus.IN_PROGRESS
                && state.getStatus() != MatchStatus.CREATED) {
            throw new BusinessException("MATCH_NOT_ACTIVE",
                    "Cannot challenge: match is not active.");
        }

        PendingAction pending = state.getPendingAction();
        if (pending == null) {
            throw new BusinessException("NO_PENDING_ACTION",
                    "No pending action to challenge.");
        }

        if (!CHALLENGEABLE_ACTIONS.contains(pending.getType())) {
            throw new BusinessException("ACTION_NOT_CHALLENGEABLE",
                    "The action '" + pending.getType() + "' does not claim a character and cannot be challenged.");
        }

        GamePlayerState challenger = findPlayer(state, challengerId, "PLAYER_NOT_IN_MATCH");
        GamePlayerState claimant = findPlayer(state, pending.getActorUserId(), "CLAIMANT_NOT_IN_MATCH");

        requireActive(challenger, "PLAYER_ELIMINATED",
                "Eliminated players cannot challenge.");
        requireActive(claimant, "CLAIMANT_ELIMINATED",
                "The claimant is no longer active and their action cannot be challenged.");

        if (challengerId.equals(pending.getActorUserId())) {
            throw new BusinessException("CANNOT_CHALLENGE_SELF",
                    "A player cannot challenge their own action.");
        }

        if (pending.getChallengerUserId() != null) {
            throw new BusinessException("DUPLICATE_CHALLENGE",
                    "This action has already been challenged. Only one challenge is allowed per action.");
        }

        if (challenger.getCards() == null || challenger.getCards().isEmpty()) {
            throw new BusinessException("NO_INFLUENCE",
                    "You need at least one influence card to challenge.");
        }

        CharacterType claimed = parseClaimedCharacter(pending.getClaimedCharacter());
        boolean claimTrue = claimant.getCards().stream()
                .anyMatch(card -> card.getCharacter() == claimed);

        GameChallenge.GameChallengeBuilder result = GameChallenge.builder()
                .challengerUserId(challengerId)
                .claimantUserId(pending.getActorUserId())
                .actionType(pending.getType())
                .claimedCharacter(pending.getClaimedCharacter());

        if (claimTrue) {
            result = resolveTruthfulClaim(state, pending, challenger, claimant,
                    claimed, loserCardId, result);
        } else {
            result = resolveBluff(state, pending, challenger, claimant,
                    claimed, result);
        }

        GameChallenge challenge = result.build();
        state.setLastChallenge(challenge);
        state.getLog().add(GameLogEntry.of("challenge",
                describe(challenge, challenger.getUsername(),
                        claimant.getUsername())));

        log.info("Challenge on '{}' resolved in match {}: claim={} loser={} continues={}",
                pending.getType(), matchId, challenge.isClaimTrue() ? "TRUE" : "FALSE",
                challenge.getInfluenceLostById(), challenge.isActionContinues());

        return gameEngine.getSafeGameState(matchId, challengerId);
    }

    /* ------------------------------------------------------------------ */
    /*  Branches                                                          */
    /* ------------------------------------------------------------------ */

    /**
     * Truthful claim: challenger loses 1 influence, claimant reveals the claimed
     * card, the card returns to the deck, a replacement is drawn, and the action
     * stays pending (marked as challenged) so the original flow can continue.
     */
    private GameChallenge.GameChallengeBuilder resolveTruthfulClaim(
            GameState state, PendingAction pending,
            GamePlayerState challenger, GamePlayerState claimant,
            CharacterType claimed, UUID loserCardId,
            GameChallenge.GameChallengeBuilder result) {

        // 1. Challenger loses exactly one influence (uses staked card if valid).
        GameCard lost = removeCard(challenger, loserCardId);
        state.getLog().add(GameLogEntry.of("reveal",
                challenger.getUsername() + " revealed " + cardName(lost)
                        + " and lost 1 influence."));
        if (challenger.getCards().isEmpty()) {
            eliminate(state, challenger, "after losing their last influence card to a failed challenge.");
        }

        // 2. Claimant proves the claim: reveal the claimed card, return it to the
        //    deck, and draw a replacement so the hand size is preserved.
        GameCard claimedCard = claimant.getCards().stream()
                .filter(card -> card.getCharacter() == claimed)
                .findFirst()
                .orElseThrow(() -> new BusinessException("INVALID_GAME_STATE",
                        "Claim verified as truthful but the claimed card is missing from the claimant's hand."));
        claimant.getCards().remove(claimedCard);
        cardManager.returnToDeck(state.getDeck(), claimedCard);
        GameCard replacement = cardManager.drawFirst(state.getDeck());
        claimant.getCards().add(replacement);
        state.setRevealedCardsCount(state.getRevealedCardsCount() + 1);
        state.getLog().add(GameLogEntry.of("reveal",
                claimant.getUsername() + " proved the claim and revealed " + cardName(claimedCard)
                        + ". The card returns to the deck and a replacement is drawn."));

        // 3. The action continues. Keep the pending action open, record the
        //    challenge (blocks a second challenge) and refresh the private
        //    exchange pool so the exchange's later confirm step sees the new hand.
        PendingAction updated = PendingAction.builder()
                .type(pending.getType())
                .actorUserId(pending.getActorUserId())
                .startedAt(pending.getStartedAt())
                .claimedCharacter(pending.getClaimedCharacter())
                .exchangePool(GameEngine.ACTION_EXCHANGE.equals(pending.getType())
                        ? new ArrayList<>(claimant.getCards())
                        : pending.getExchangePool())
                .originalHandCardIds(pending.getOriginalHandCardIds())
                .targetPlayerId(pending.getTargetPlayerId())
                .reservedCoins(pending.getReservedCoins())
                .challengerUserId(challenger.getUserId())
                .build();
        state.setPendingAction(updated);

        return result
                .claimTrue(true)
                .influenceLostById(challenger.getUserId())
                .revealedCardId(claimedCard.getId())
                .revealedCharacterId(claimed.name().toLowerCase())
                .actionContinues(true);
    }

    /**
     * Bluff claim: the claimant loses exactly one influence, the pending action
     * is cancelled (no reward/effect, no reserved coins deducted) and the turn
     * advances.
     */
    private GameChallenge.GameChallengeBuilder resolveBluff(
            GameState state, PendingAction pending,
            GamePlayerState challenger, GamePlayerState claimant,
            CharacterType claimed, GameChallenge.GameChallengeBuilder result) {

        // Cancel an Exchange first (restore the actor's original hand and
        // return the two drawn cards to the deck), then take the influence
        // penalty from the restored hand — the penalty is one original card.
        if (GameEngine.ACTION_EXCHANGE.equals(pending.getType())) {
            restoreExchangeHand(state, claimant, pending);
        }
        GameCard lost = removeCard(claimant, null);
        state.getLog().add(GameLogEntry.of("reveal",
                claimant.getUsername() + " was caught bluffing and revealed "
                        + cardName(lost) + " (lost 1 influence)."));
        if (claimant.getCards().isEmpty()) {
            eliminate(state, claimant, "after losing their last influence card to a successful challenge.");
        }

        // Cancel the action: everything else has no effect yet (reserved
        // assassination coins were never deducted).
        state.setPendingAction(null);

        Match match = turnManager.advanceTurn(matchIdFixture(state));
        state.setCurrentTurnPlayerId(match.getCurrentTurnPlayerId());
        state.setTurnNumber(match.getTurnNumber());
        state.setActionExecuted(false);
        state.setPhase(GameEngine.PHASE_IN_PROGRESS);

        return result
                .claimTrue(false)
                .influenceLostById(claimant.getUserId())
                .revealedCardId(lost.getId())
                .revealedCharacterId(lost.getCharacter().name().toLowerCase())
                .actionContinues(false);
    }

    /* ------------------------------------------------------------------ */
    /*  Card helpers                                                      */
    /* ------------------------------------------------------------------ */

    /**
     * Removes one influence card from a player's hand. A preferred card (staked
     * by the caller) wins when it belongs to the player; otherwise the first
     * card of the hand is removed, matching the existing card-loss design.
     */
    private GameCard removeCard(GamePlayerState player, UUID preferredCardId) {
        List<GameCard> hand = player.getCards();
        if (hand == null || hand.isEmpty()) {
            throw new BusinessException("NO_INFLUENCE",
                    "A player with no influence cards cannot take part in a challenge.");
        }
        if (preferredCardId != null) {
            GameCard picked = hand.stream()
                    .filter(card -> card.getId().equals(preferredCardId))
                    .findFirst()
                    .orElseThrow(() -> new BusinessException("INVALID_CARD_SELECTION",
                            "The selected card is not part of your hand."));
            hand.remove(picked);
            return picked;
        }
        return hand.remove(0);
    }

    /** Restores the actor's original hand for a cancelled Exchange, returning the two drawn cards to the deck. */
    private void restoreExchangeHand(GameState state, GamePlayerState actor, PendingAction pending) {
        List<UUID> originalIds = pending.getOriginalHandCardIds();
        List<GameCard> keep = new ArrayList<>();
        List<GameCard> returnToDeck = new ArrayList<>();
        for (GameCard card : actor.getCards()) {
            if (originalIds != null && originalIds.contains(card.getId())) {
                keep.add(card);
            } else {
                returnToDeck.add(card);
            }
        }
        actor.setCards(keep);
        for (GameCard card : returnToDeck) {
            cardManager.returnToDeck(state.getDeck(), card);
        }
    }

    private void eliminate(GameState state, GamePlayerState player, String reason) {
        player.setStatus(PlayerStatus.ELIMINATED);
        state.getLog().add(GameLogEntry.of("elimination",
                player.getUsername() + " was eliminated " + reason));
    }

    private String cardName(GameCard card) {
        return card.getCharacter().name().toLowerCase();
    }

    private String describe(GameChallenge challenge, String challengerName, String claimantName) {
        StringBuilder sb = new StringBuilder();
        if (challenge.isClaimTrue()) {
            sb.append(challengerName).append(" challenged ").append(claimantName)
                    .append("'s claim of ").append(challenge.getClaimedCharacter())
                    .append(" — the claim was TRUE. ").append(challengerName)
                    .append(" lost 1 influence; the action continues.");
        } else {
            sb.append(challengerName).append(" challenged ").append(claimantName)
                    .append("'s claim of ").append(challenge.getClaimedCharacter())
                    .append(" — the claim was a BLUFF. ").append(claimantName)
                    .append(" lost 1 influence; the action was cancelled.");
        }
        return sb.toString();
    }

    /* ------------------------------------------------------------------ */
    /*  Validation helpers                                                */
    /* ------------------------------------------------------------------ */

    private CharacterType parseClaimedCharacter(String claimedCharacter) {
        if (claimedCharacter == null || claimedCharacter.isBlank()) {
            throw new BusinessException("INVALID_CLAIM",
                    "The pending action does not carry a character claim.");
        }
        try {
            return CharacterType.valueOf(claimedCharacter.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("INVALID_CLAIM",
                    "Unknown claimed character '" + claimedCharacter + "'.");
        }
    }

    private GamePlayerState findPlayer(GameState state, UUID userId, String errorCode) {
        return state.getPlayers().stream()
                .filter(player -> player.getUserId().equals(userId))
                .findFirst()
                .orElseThrow(() -> new BusinessException(errorCode,
                        "The player is not part of this match."));
    }

    private void requireActive(GamePlayerState player, String errorCode, String message) {
        if (player.getStatus() != PlayerStatus.ACTIVE) {
            throw new BusinessException(errorCode, message);
        }
    }

    /**
     * Returns the match id the current pending action belongs to. Kept as a
     * small seam so the turn-advance tail can be exercised by unit tests with a
     * mocked TurnManager.
     */
    private UUID matchIdFixture(GameState state) {
        return state.getMatchId();
    }
}