package com.rajneeti.game;

import com.rajneeti.dto.game.GamePlayerDto;
import com.rajneeti.dto.game.GameStateResponse;
import com.rajneeti.dto.game.PendingActionDto;
import com.rajneeti.entity.Match;
import com.rajneeti.entity.enums.MatchStatus;
import com.rajneeti.entity.enums.PlayerStatus;
import com.rajneeti.exception.BusinessException;
import com.rajneeti.repository.MatchPlayerRepository;
import com.rajneeti.repository.MatchRepository;
import com.rajneeti.service.TurnManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Module 18 — Challenge Manager tests.
 *
 * <p>Seeds the in-memory {@link GameStore} with a real 15-card deck whose hands
 * are carved out of that same deck (so deck-integrity assertions always hold),
 * then drives {@link GameEngine} actions and {@link ChallengeManager#challenge}.
 * The TurnManager is mocked; on a successful bluff the challenge advances the
 * turn through it.
 */
@ExtendWith(MockitoExtension.class)
class ChallengeManagerTest {

    @Mock
    private MatchRepository matchRepository;

    @Mock
    private MatchPlayerRepository matchPlayerRepository;

    @Mock
    private TurnManager turnManager;

    private final GameStore gameStore = new GameStore();
    private final CardManager cardManager = new CardManager();
    private final GameStateMapper gameStateMapper = new GameStateMapper();

    private GameEngine gameEngine;
    private ChallengeManager challengeManager;

    private UUID matchId;
    private UUID actorId;
    private UUID challengerId;

    @BeforeEach
    void setUp() {
        matchId = UUID.randomUUID();
        actorId = UUID.randomUUID();
        challengerId = UUID.randomUUID();

        gameEngine = new GameEngine(
                matchRepository, matchPlayerRepository, gameStore,
                cardManager, turnManager, gameStateMapper);
        challengeManager = new ChallengeManager(gameEngine, cardManager, turnManager);
        gameStore.remove(matchId);
    }

    /* ------------------------------------------------------------------ */
    /*  Helpers                                                           */
    /* ------------------------------------------------------------------ */

    private static class PlayerSpec {
        final UUID userId;
        final String username;
        final int coins;
        final PlayerStatus status;
        final CharacterType[] characters;

        PlayerSpec(UUID userId, String username, int coins, PlayerStatus status,
                   CharacterType... characters) {
            this.userId = userId;
            this.username = username;
            this.coins = coins;
            this.status = status;
            this.characters = characters;
        }
    }

    private GameCard takeCard(List<GameCard> deck, CharacterType type) {
        GameCard card = deck.stream()
                .filter(c -> c.getCharacter() == type)
                .findFirst()
                .orElseThrow(() -> new AssertionError("No " + type + " card left in test deck."));
        deck.remove(card);
        return card;
    }

    /**
     * Builds a game where every hand is carved from the same 15-card deck that
     * becomes the state's deck, keeping card counts exact.
     */
    private GameState seed(MatchStatus status, UUID currentTurnPlayerId,
                           PlayerSpec... specs) {
        List<GameCard> deck = cardManager.createDeck();
        List<GamePlayerState> players = new ArrayList<>();
        for (PlayerSpec spec : specs) {
            List<GameCard> cards = new ArrayList<>();
            for (CharacterType type : spec.characters) {
                cards.add(takeCard(deck, type));
            }
            players.add(GamePlayerState.builder()
                    .userId(spec.userId)
                    .username(spec.username)
                    .seatNumber(players.size() + 1)
                    .status(spec.status)
                    .coins(spec.coins)
                    .cards(cards)
                    .host(false)
                    .build());
        }

        GameState state = GameState.builder()
                .matchId(matchId)
                .roomId(UUID.randomUUID())
                .roomCode("RAJNAS")
                .status(status)
                .phase(GameEngine.PHASE_IN_PROGRESS)
                .players(players)
                .turnOrder(players.stream().map(GamePlayerState::getUserId).toList())
                .currentTurnPlayerId(currentTurnPlayerId)
                .turnNumber(1)
                .deck(deck)
                .log(new ArrayList<>())
                .build();
        gameStore.put(matchId, state);
        return state;
    }

    private PlayerSpec actor(int coins, PlayerStatus status, CharacterType... chars) {
        return new PlayerSpec(actorId, "actor", coins, status, chars);
    }

    private PlayerSpec challenger(int coins, PlayerStatus status, CharacterType... chars) {
        return new PlayerSpec(challengerId, "challenger", coins, status, chars);
    }

    private Match advancedMatch(UUID nextPlayerId, int turnNumber) {
        return Match.builder()
                .id(matchId)
                .status(MatchStatus.IN_PROGRESS)
                .currentTurnPlayerId(nextPlayerId)
                .turnNumber(turnNumber)
                .build();
    }

    private void stubAdvance() {
        when(turnManager.advanceTurn(matchId)).thenReturn(advancedMatch(challengerId, 2));
    }

    private GamePlayerDto findPlayer(GameStateResponse response, UUID userId) {
        return response.getPlayers().stream()
                .filter(p -> p.getUserId().equals(userId))
                .findFirst()
                .orElseThrow();
    }

    private PendingAction pending(UUID actorId, String type, String claimed,
                                  UUID targetPlayerId) {
        return PendingAction.builder()
                .type(type)
                .actorUserId(actorId)
                .claimedCharacter(claimed)
                .targetPlayerId(targetPlayerId)
                .build();
    }

    private void assertChallengeRejectsWith(UUID challenger, String errorCode) {
        assertThatThrownBy(() -> challengeManager.challenge(matchId, challenger, null))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(errorCode);
    }

    /* ------------------------------------------------------------------ */
    /*  Tests — every claim-based action is challengeable                 */
    /* ------------------------------------------------------------------ */

    @Test
    @DisplayName("Tax can be challenged — truthful Minister reveals and the action continues")
    void tax_canBeChallenged_truthful() {
        GameState state = seed(MatchStatus.IN_PROGRESS, actorId,
                actor(5, PlayerStatus.ACTIVE, CharacterType.MINISTER, CharacterType.GOYENDA),
                challenger(2, PlayerStatus.ACTIVE, CharacterType.AMLA, CharacterType.DALAL));

        gameEngine.performTax(matchId, actorId);
        GameStateResponse response = challengeManager.challenge(matchId, challengerId, null);

        GameChallenge challenge = state.getLastChallenge();
        assertThat(challenge).isNotNull();
        assertThat(challenge.isClaimTrue()).isTrue();
        assertThat(challenge.getActionType()).isEqualTo("TAX");
        assertThat(challenge.getClaimedCharacter()).isEqualTo("minister");
        assertThat(challenge.getInfluenceLostById()).isEqualTo(challengerId);
        assertThat(challenge.isActionContinues()).isTrue();

        assertThat(response.getLastChallenge()).isNotNull();
        assertThat(response.getLastChallenge().getResult()).isEqualTo("CLAIM_TRUE");
        assertThat(response.getLastChallenge().getRevealedCharacterId()).isEqualTo("minister");
        assertThat(response.getLastChallenge().isActionContinues()).isTrue();

        // Action stays pending (blocked from a second challenge), no coins yet.
        assertThat(state.getPendingAction()).isNotNull();
        assertThat(state.getPendingAction().getChallengerUserId()).isEqualTo(challengerId);
        verifyNoInteractions(turnManager);
    }

    @Test
    @DisplayName("Tax truthful challenge resolves to +3 coins when the actor resolves it afterwards")
    void tax_truthfulChallenge_thenResolves() {
        GameState state = seed(MatchStatus.IN_PROGRESS, actorId,
                actor(2, PlayerStatus.ACTIVE, CharacterType.MINISTER, CharacterType.AMLA),
                challenger(2, PlayerStatus.ACTIVE, CharacterType.DALAL, CharacterType.GOYENDA));
        stubAdvance();

        gameEngine.performTax(matchId, actorId);
        challengeManager.challenge(matchId, challengerId, null);
        GameStateResponse response = gameEngine.resolveTax(matchId, actorId, true);

        assertThat(state.getPlayers().get(0).getCoins()).isEqualTo(5);
        assertThat(state.getPendingAction()).isNull();
        assertThat(state.getLastChallenge()).isNotNull();
        assertThat(state.isActionExecuted()).isFalse();
        assertThat(findPlayer(response, actorId).getCoins()).isEqualTo(5);
    }

    @Test
    @DisplayName("Steal can be challenged — truthful Dalal keeps the pending target and action continues")
    void steal_canBeChallenged_truthful() {
        GameState state = seed(MatchStatus.IN_PROGRESS, actorId,
                actor(2, PlayerStatus.ACTIVE, CharacterType.DALAL, CharacterType.GOYENDA),
                challenger(4, PlayerStatus.ACTIVE, CharacterType.AMLA, CharacterType.AMLA));

        gameEngine.performSteal(matchId, actorId, challengerId);
        GameStateResponse response = challengeManager.challenge(matchId, challengerId, null);

        assertThat(state.getLastChallenge().isClaimTrue()).isTrue();
        assertThat(state.getLastChallenge().getInfluenceLostById()).isEqualTo(challengerId);
        assertThat(state.getLastChallenge().isActionContinues()).isTrue();

        // The pending Steal survives with its target for the actor to resolve.
        assertThat(state.getPendingAction().getType()).isEqualTo("STEAL");
        assertThat(state.getPendingAction().getTargetPlayerId()).isEqualTo(challengerId);
        assertThat(response.getPendingAction().getType()).isEqualTo("STEAL");
    }

    @Test
    @DisplayName("Steal truthful challenge transfers up to 2 coins when resolved")
    void steal_truthfulChallenge_thenResolves() {
        GameState state = seed(MatchStatus.IN_PROGRESS, actorId,
                actor(2, PlayerStatus.ACTIVE, CharacterType.DALAL, CharacterType.GOYENDA),
                challenger(4, PlayerStatus.ACTIVE, CharacterType.AMLA, CharacterType.AMLA));
        stubAdvance();

        gameEngine.performSteal(matchId, actorId, challengerId);
        challengeManager.challenge(matchId, challengerId, null);
        GameStateResponse response = gameEngine.resolveSteal(matchId, actorId, true);

        assertThat(state.getPlayers().get(0).getCoins()).isEqualTo(4);
        assertThat(state.getPlayers().get(1).getCoins()).isEqualTo(2);
        assertThat(state.getPendingAction()).isNull();
        assertThat(state.getLastChallenge()).isNotNull();
        assertThat(findPlayer(response, actorId).getCoins()).isEqualTo(4);
        assertThat(findPlayer(response, challengerId).getCoins()).isEqualTo(2);
    }

    @Test
    @DisplayName("Exchange can be challenged — truthful Amla reveals and the private pool is refreshed")
    void exchange_canBeChallenged_truthful() {
        GameState state = seed(MatchStatus.IN_PROGRESS, actorId,
                actor(2, PlayerStatus.ACTIVE, CharacterType.AMLA, CharacterType.GOYENDA),
                challenger(2, PlayerStatus.ACTIVE, CharacterType.DALAL, CharacterType.DALAL));

        gameEngine.performExchange(matchId, actorId);
        GamePlayerState actorState = state.getPlayers().get(0);
        int handBefore = actorState.getCards().size();
        int deckBefore = state.getDeck().size();

        GameStateResponse response = challengeManager.challenge(matchId, challengerId, null);

        assertThat(state.getLastChallenge().isClaimTrue()).isTrue();
        assertThat(state.getLastChallenge().getInfluenceLostById()).isEqualTo(challengerId);
        assertThat(state.getLastChallenge().isActionContinues()).isTrue();

        // Actor still holds exactly the refreshed 4-card pool (2 drawn + 2 after reveal).
        assertThat(actorState.getCards()).hasSize(handBefore);
        assertThat(state.getPendingAction().getOriginalHandCardIds()).hasSize(2);
        Set<UUID> poolIds = state.getPendingAction().getExchangePool().stream()
                .map(GameCard::getId).collect(Collectors.toSet());
        Set<UUID> handIds = actorState.getCards().stream()
                .map(GameCard::getId).collect(Collectors.toSet());
        assertThat(poolIds).isEqualTo(handIds);
        assertThat(state.getDeck()).hasSize(deckBefore);
        assertThat(response.getLastChallenge().getRevealedCharacterId()).isEqualTo("amla");
    }

    @Test
    @DisplayName("Assassinate can be challenged — truthful Ghatok keeps the reserved cost untouched")
    void assassinate_canBeChallenged_truthful() {
        GameState state = seed(MatchStatus.IN_PROGRESS, actorId,
                actor(6, PlayerStatus.ACTIVE, CharacterType.GHATOK, CharacterType.GOYENDA),
                challenger(2, PlayerStatus.ACTIVE, CharacterType.AMLA, CharacterType.DALAL));

        gameEngine.performAssassinate(matchId, actorId, challengerId);
        GameStateResponse response = challengeManager.challenge(matchId, challengerId, null);

        assertThat(state.getLastChallenge().isClaimTrue()).isTrue();
        assertThat(state.getLastChallenge().getInfluenceLostById()).isEqualTo(challengerId);
        assertThat(state.getLastChallenge().isActionContinues()).isTrue();

        // The reservation survives; nothing was deducted during the challenge.
        assertThat(state.getPendingAction().getReservedCoins()).isEqualTo(3);
        assertThat(state.getPlayers().get(0).getCoins()).isEqualTo(6);
        assertThat(response.getLastChallenge().getRevealedCharacterId()).isEqualTo("ghatok");
    }

    /* ------------------------------------------------------------------ */
    /*  Tests — actions with no character claim are not challengeable     */
    /* ------------------------------------------------------------------ */

    @Test
    @DisplayName("Income cannot be challenged")
    void income_cannotBeChallenged() {
        seed(MatchStatus.IN_PROGRESS, actorId,
                actor(2, PlayerStatus.ACTIVE, CharacterType.MINISTER, CharacterType.GOYENDA),
                challenger(2, PlayerStatus.ACTIVE, CharacterType.AMLA, CharacterType.DALAL));
        gameStore.get(matchId).setPendingAction(pending(actorId, "INCOME", null, null));

        assertChallengeRejectsWith(challengerId, "ACTION_NOT_CHALLENGEABLE");
    }

    @Test
    @DisplayName("Foreign Aid cannot be challenged")
    void foreignAid_cannotBeChallenged() {
        seed(MatchStatus.IN_PROGRESS, actorId,
                actor(2, PlayerStatus.ACTIVE, CharacterType.MINISTER, CharacterType.GOYENDA),
                challenger(2, PlayerStatus.ACTIVE, CharacterType.AMLA, CharacterType.DALAL));
        gameStore.get(matchId).setPendingAction(pending(actorId, "FOREIGN_AID", null, null));

        assertChallengeRejectsWith(challengerId, "ACTION_NOT_CHALLENGEABLE");
    }

    @Test
    @DisplayName("Coup cannot be challenged")
    void coup_cannotBeChallenged() {
        seed(MatchStatus.IN_PROGRESS, actorId,
                actor(2, PlayerStatus.ACTIVE, CharacterType.MINISTER, CharacterType.GOYENDA),
                challenger(2, PlayerStatus.ACTIVE, CharacterType.AMLA, CharacterType.DALAL));
        gameStore.get(matchId).setPendingAction(pending(actorId, "COUP", null, challengerId));

        assertChallengeRejectsWith(challengerId, "ACTION_NOT_CHALLENGEABLE");
    }

    /* ------------------------------------------------------------------ */
    /*  Tests — truthful claim outcomes                                   */
    /* ------------------------------------------------------------------ */

    @Test
    @DisplayName("Truthful claim: the challenger loses exactly one influence card")
    void truthful_challengerLosesOneInfluence() {
        GameState state = seed(MatchStatus.IN_PROGRESS, actorId,
                actor(5, PlayerStatus.ACTIVE, CharacterType.MINISTER, CharacterType.GOYENDA),
                challenger(2, PlayerStatus.ACTIVE, CharacterType.AMLA, CharacterType.DALAL));

        gameEngine.performTax(matchId, actorId);
        challengeManager.challenge(matchId, challengerId, null);

        GamePlayerState challengerState = state.getPlayers().get(1);
        assertThat(challengerState.getCards()).hasSize(1);
        assertThat(challengerState.getStatus()).isEqualTo(PlayerStatus.ACTIVE);
    }

    @Test
    @DisplayName("Truthful claim: the claimant keeps their hand size via a replacement draw")
    void truthful_claimantKeepsHandSize() {
        GameState state = seed(MatchStatus.IN_PROGRESS, actorId,
                actor(5, PlayerStatus.ACTIVE, CharacterType.MINISTER, CharacterType.AMLA),
                challenger(2, PlayerStatus.ACTIVE, CharacterType.DALAL, CharacterType.DALAL));

        UUID claimedCardId = state.getPlayers().get(0).getCards().get(0).getId();
        List<UUID> deckBefore = state.getDeck().stream().map(GameCard::getId).toList();
        gameEngine.performTax(matchId, actorId);
        challengeManager.challenge(matchId, challengerId, null);

        GamePlayerState actorState = state.getPlayers().get(0);
        // One card revealed, one replacement drawn: the hand size is preserved.
        assertThat(actorState.getCards()).hasSize(2);
        // The revealed card returned to the deck...
        assertThat(state.getDeck().stream().map(GameCard::getId))
                .contains(claimedCardId);
        // ...and the replacement came out of that deck (not out of thin air).
        assertThat(actorState.getCards().stream().map(GameCard::getId))
                .containsAnyElementsOf(deckBefore);
        assertThat(state.getDeck()).hasSize(11);
    }

    @Test
    @DisplayName("Truthful claim: the action stays pending and a second challenge is rejected")
    void truthful_duplicateChallenge_rejected() {
        seed(MatchStatus.IN_PROGRESS, actorId,
                actor(5, PlayerStatus.ACTIVE, CharacterType.MINISTER, CharacterType.GOYENDA),
                challenger(2, PlayerStatus.ACTIVE, CharacterType.AMLA, CharacterType.DALAL));

        gameEngine.performTax(matchId, actorId);
        GameStateResponse first = challengeManager.challenge(matchId, challengerId, null);
        assertThat(first.getLastChallenge().getResult()).isEqualTo("CLAIM_TRUE");

        assertChallengeRejectsWith(challengerId, "DUPLICATE_CHALLENGE");
    }

    @Test
    @DisplayName("Truthful claim: hidden cards stay private (challenger sees influence count only)")
    void truthful_hiddenCardsStayPrivate() {
        GameState state = seed(MatchStatus.IN_PROGRESS, actorId,
                actor(5, PlayerStatus.ACTIVE, CharacterType.MINISTER, CharacterType.GOYENDA),
                challenger(2, PlayerStatus.ACTIVE, CharacterType.AMLA, CharacterType.DALAL));

        gameEngine.performTax(matchId, actorId);
        GameStateResponse response = challengeManager.challenge(matchId, challengerId, null);

        GamePlayerDto claimantView = findPlayer(response, actorId);
        assertThat(claimantView.getCards()).isNull();
        assertThat(claimantView.getInfluenceCount()).isEqualTo(2);
        assertThat(claimantView.getCoins()).isEqualTo(5);

        // The challenger still sees their own cards (own cards are safe to expose).
        GamePlayerDto challengerView = findPlayer(response, challengerId);
        assertThat(challengerView.getCards()).hasSize(1);
        assertThat(challengerView.getInfluenceCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("Tax accepted with an explicit staked card loses exactly that card")
    void truthful_loserCardIdRespected() {
        GameState state = seed(MatchStatus.IN_PROGRESS, actorId,
                actor(5, PlayerStatus.ACTIVE, CharacterType.MINISTER, CharacterType.GOYENDA),
                challenger(2, PlayerStatus.ACTIVE, CharacterType.AMLA, CharacterType.DALAL));

        gameEngine.performTax(matchId, actorId);
        GamePlayerState challengerState = state.getPlayers().get(1);
        UUID staked = challengerState.getCards().get(1).getId();

        challengeManager.challenge(matchId, challengerId, staked);

        assertThat(challengerState.getCards().stream().map(GameCard::getId))
                .doesNotContain(staked);
    }

    /* ------------------------------------------------------------------ */
    /*  Tests — bluff claim outcomes                                      */
    /* ------------------------------------------------------------------ */

    @Test
    @DisplayName("Bluff Tax: the actor loses one influence, the action is cancelled, no coins awarded")
    void bluff_tax_cancelled() {
        GameState state = seed(MatchStatus.IN_PROGRESS, actorId,
                actor(2, PlayerStatus.ACTIVE, CharacterType.GOYENDA, CharacterType.AMLA),
                challenger(2, PlayerStatus.ACTIVE, CharacterType.MINISTER, CharacterType.DALAL));
        stubAdvance();

        gameEngine.performTax(matchId, actorId);
        GameStateResponse response = challengeManager.challenge(matchId, challengerId, null);

        GamePlayerState actorState = state.getPlayers().get(0);
        assertThat(state.getLastChallenge().isClaimTrue()).isFalse();
        assertThat(state.getLastChallenge().getInfluenceLostById()).isEqualTo(actorId);
        assertThat(state.getLastChallenge().isActionContinues()).isFalse();
        assertThat(actorState.getCards()).hasSize(1);
        assertThat(actorState.getCoins()).isEqualTo(2);
        assertThat(state.getPendingAction()).isNull();
        assertThat(state.getCurrentTurnPlayerId()).isEqualTo(challengerId);
        assertThat(response.getLastChallenge().getResult()).isEqualTo("CLAIM_FALSE");
        assertThat(response.getPendingAction()).isNull();
    }

    @Test
    @DisplayName("Bluff Steal: no coins are transferred")
    void bluff_steal_noCoinsTransferred() {
        GameState state = seed(MatchStatus.IN_PROGRESS, actorId,
                actor(2, PlayerStatus.ACTIVE, CharacterType.GOYENDA, CharacterType.AMLA),
                challenger(4, PlayerStatus.ACTIVE, CharacterType.DALAL, CharacterType.DALAL));
        stubAdvance();

        gameEngine.performSteal(matchId, actorId, challengerId);
        challengeManager.challenge(matchId, challengerId, null);

        assertThat(state.getLastChallenge().isClaimTrue()).isFalse();
        assertThat(state.getPlayers().get(0).getCoins()).isEqualTo(2);
        assertThat(state.getPlayers().get(1).getCoins()).isEqualTo(4);
        assertThat(state.getPendingAction()).isNull();
    }

    @Test
    @DisplayName("Bluff Exchange: the actor's original hand is restored and drawn cards return to the deck")
    void bluff_exchange_restoresOriginalHand() {
        GameState state = seed(MatchStatus.IN_PROGRESS, actorId,
                actor(2, PlayerStatus.ACTIVE, CharacterType.GOYENDA, CharacterType.DALAL),
                challenger(2, PlayerStatus.ACTIVE, CharacterType.AMLA, CharacterType.AMLA));
        stubAdvance();

        gameEngine.performExchange(matchId, actorId);
        GamePlayerState actorState = state.getPlayers().get(0);
        List<UUID> originalHand = new ArrayList<>(
                state.getPendingAction().getOriginalHandCardIds());
        int deckAfterPerform = state.getDeck().size();

        challengeManager.challenge(matchId, challengerId, null);

        // The original hand is restored... then one of those cards is taken as
        // the bluff penalty, and the two drawn cards return to the deck.
        assertThat(actorState.getCards()).hasSize(1);
        assertThat(actorState.getCards().stream().map(GameCard::getId).toList())
                .isSubsetOf(originalHand);
        assertThat(state.getDeck()).hasSize(deckAfterPerform + 2);
        assertThat(state.getPendingAction()).isNull();
        assertThat(state.getLastChallenge().isClaimTrue()).isFalse();
    }

    @Test
    @DisplayName("Bluff Assassinate: nothing is deducted and no influence is lost by the target")
    void bluff_assassinate_noPaymentNoLoss() {
        GameState state = seed(MatchStatus.IN_PROGRESS, actorId,
                actor(6, PlayerStatus.ACTIVE, CharacterType.GOYENDA, CharacterType.AMLA),
                challenger(2, PlayerStatus.ACTIVE, CharacterType.GHATOK, CharacterType.DALAL));
        stubAdvance();

        gameEngine.performAssassinate(matchId, actorId, challengerId);
        GameStateResponse response = challengeManager.challenge(matchId, challengerId, null);

        assertThat(state.getLastChallenge().isClaimTrue()).isFalse();
        assertThat(state.getLastChallenge().getInfluenceLostById()).isEqualTo(actorId);
        assertThat(state.getPlayers().get(0).getCoins()).isEqualTo(6);
        assertThat(state.getPlayers().get(1).getCards()).hasSize(2);
        assertThat(state.getPendingAction()).isNull();
        assertThat(state.getCurrentTurnPlayerId()).isEqualTo(challengerId);
        assertThat(response.getLastChallenge().getResult()).isEqualTo("CLAIM_FALSE");
    }

    @Test
    @DisplayName("Bluff claim: the actor loses their last card and is eliminated")
    void bluff_lastCardEliminatesActor() {
        GameState state = seed(MatchStatus.IN_PROGRESS, actorId,
                actor(2, PlayerStatus.ACTIVE, CharacterType.GOYENDA),
                challenger(2, PlayerStatus.ACTIVE, CharacterType.DALAL, CharacterType.MINISTER));
        stubAdvance();

        gameEngine.performTax(matchId, actorId);
        challengeManager.challenge(matchId, challengerId, null);

        GamePlayerState actorState = state.getPlayers().get(0);
        assertThat(actorState.getCards()).isEmpty();
        assertThat(actorState.getStatus()).isEqualTo(PlayerStatus.ELIMINATED);
        assertThat(state.getPendingAction()).isNull();
    }

    @Test
    @DisplayName("Truthful claim: the challenger loses their last card and is eliminated")
    void truthful_lastCardEliminatesChallenger() {
        GameState state = seed(MatchStatus.IN_PROGRESS, actorId,
                actor(5, PlayerStatus.ACTIVE, CharacterType.MINISTER, CharacterType.GOYENDA),
                challenger(2, PlayerStatus.ACTIVE, CharacterType.AMLA));
        // No turn advance on a truthful claim.
        verifyNoInteractions(turnManager);

        gameEngine.performTax(matchId, actorId);
        challengeManager.challenge(matchId, challengerId, null);

        GamePlayerState challengerState = state.getPlayers().get(1);
        assertThat(challengerState.getCards()).isEmpty();
        assertThat(challengerState.getStatus()).isEqualTo(PlayerStatus.ELIMINATED);
        // The pending action survives for the actor to resolve.
        assertThat(state.getPendingAction()).isNotNull();
    }

    /* ------------------------------------------------------------------ */
    /*  Tests — challenge validity rules                                  */
    /* ------------------------------------------------------------------ */

    @Test
    @DisplayName("Eliminated players cannot challenge")
    void eliminatedPlayer_cannotChallenge() {
        seed(MatchStatus.IN_PROGRESS, actorId,
                actor(5, PlayerStatus.ACTIVE, CharacterType.MINISTER, CharacterType.GOYENDA),
                challenger(2, PlayerStatus.ELIMINATED, CharacterType.AMLA, CharacterType.DALAL));
        gameStore.get(matchId).setPendingAction(pending(actorId, "TAX", "minister", null));

        assertChallengeRejectsWith(challengerId, "PLAYER_ELIMINATED");
    }

    @Test
    @DisplayName("A player cannot challenge their own action")
    void cannotChallengeSelf() {
        seed(MatchStatus.IN_PROGRESS, actorId,
                actor(5, PlayerStatus.ACTIVE, CharacterType.MINISTER, CharacterType.GOYENDA),
                challenger(2, PlayerStatus.ACTIVE, CharacterType.AMLA, CharacterType.DALAL));
        gameStore.get(matchId).setPendingAction(pending(actorId, "TAX", "minister", null));

        assertChallengeRejectsWith(actorId, "CANNOT_CHALLENGE_SELF");
    }

    @Test
    @DisplayName("A player outside the match cannot challenge")
    void playerOutsideMatch_cannotChallenge() {
        seed(MatchStatus.IN_PROGRESS, actorId,
                actor(5, PlayerStatus.ACTIVE, CharacterType.MINISTER, CharacterType.GOYENDA),
                challenger(2, PlayerStatus.ACTIVE, CharacterType.AMLA, CharacterType.DALAL));
        gameStore.get(matchId).setPendingAction(pending(actorId, "TAX", "minister", null));

        assertChallengeRejectsWith(UUID.randomUUID(), "PLAYER_NOT_IN_MATCH");
    }

    @Test
    @DisplayName("A challenger with no influence cards cannot challenge")
    void challengerWithoutInfluence_cannotChallenge() {
        seed(MatchStatus.IN_PROGRESS, actorId,
                actor(5, PlayerStatus.ACTIVE, CharacterType.MINISTER, CharacterType.GOYENDA),
                challenger(2, PlayerStatus.ACTIVE));
        gameStore.get(matchId).setPendingAction(pending(actorId, "TAX", "minister", null));

        assertChallengeRejectsWith(challengerId, "NO_INFLUENCE");
    }

    @Test
    @DisplayName("Challenging without a pending action is rejected")
    void noPendingAction_rejected() {
        seed(MatchStatus.IN_PROGRESS, actorId,
                actor(5, PlayerStatus.ACTIVE, CharacterType.MINISTER, CharacterType.GOYENDA),
                challenger(2, PlayerStatus.ACTIVE, CharacterType.AMLA, CharacterType.DALAL));

        assertChallengeRejectsWith(challengerId, "NO_PENDING_ACTION");
    }

    @Test
    @DisplayName("Challenging in a finished match is rejected")
    void finishedMatch_cannotChallenge() {
        seed(MatchStatus.FINISHED, actorId,
                actor(5, PlayerStatus.ACTIVE, CharacterType.MINISTER, CharacterType.GOYENDA),
                challenger(2, PlayerStatus.ACTIVE, CharacterType.AMLA, CharacterType.DALAL));
        gameStore.get(matchId).setPendingAction(pending(actorId, "TAX", "minister", null));

        assertChallengeRejectsWith(challengerId, "MATCH_NOT_ACTIVE");
    }

    @Test
    @DisplayName("A pending action carrying an unknown claimed character is rejected")
    void invalidClaimedCharacter_rejected() {
        seed(MatchStatus.IN_PROGRESS, actorId,
                actor(5, PlayerStatus.ACTIVE, CharacterType.MINISTER, CharacterType.GOYENDA),
                challenger(2, PlayerStatus.ACTIVE, CharacterType.AMLA, CharacterType.DALAL));
        gameStore.get(matchId).setPendingAction(pending(actorId, "TAX", "president", null));

        assertChallengeRejectsWith(challengerId, "INVALID_CLAIM");
    }

    /* ------------------------------------------------------------------ */
    /*  Tests — stale verdict lifecycle                                   */
    /* ------------------------------------------------------------------ */

    @Test
    @DisplayName("A new action clears the previous challenge verdict")
    void newAction_clearsLastChallenge() {
        // Bluff Tax: no minister in the actor's hand, so the claim is exposed and
        // the turn advances to the challenger, who then starts a fresh action.
        GameState state = seed(MatchStatus.IN_PROGRESS, actorId,
                actor(2, PlayerStatus.ACTIVE, CharacterType.GOYENDA, CharacterType.AMLA),
                challenger(2, PlayerStatus.ACTIVE, CharacterType.MINISTER, CharacterType.DALAL));
        stubAdvance();

        gameEngine.performTax(matchId, actorId);
        challengeManager.challenge(matchId, challengerId, null);
        assertThat(state.getLastChallenge()).isNotNull();

        // A brand-new action clears the stale verdict.
        gameEngine.performTax(matchId, challengerId);

        assertThat(state.getLastChallenge()).isNull();
    }

    @Test
    @DisplayName("Deck is internally consistent after truthful and bluff challenge flows")
    void deckConsistencyHolds() {
        stubAdvance();

        // Bluff flow: the actor's Tax claim (no minister) is exposed and cancelled.
        GameState state = seed(MatchStatus.IN_PROGRESS, actorId,
                actor(2, PlayerStatus.ACTIVE, CharacterType.GOYENDA, CharacterType.AMLA),
                challenger(2, PlayerStatus.ACTIVE, CharacterType.MINISTER, CharacterType.DALAL));
        gameEngine.performTax(matchId, actorId);
        challengeManager.challenge(matchId, challengerId, null);

        // A challenge permanently removes one influence card; the rest remain unique.
        assertNoDuplicateIds(state.getDeck(), state.getPlayers());

        // Truthful exchange flow: the challenger holds amla and wins the claim.
        state = seed(MatchStatus.IN_PROGRESS, challengerId,
                challenger(2, PlayerStatus.ACTIVE, CharacterType.AMLA, CharacterType.DALAL),
                actor(2, PlayerStatus.ACTIVE, CharacterType.GOYENDA, CharacterType.GOYENDA));
        gameEngine.performExchange(matchId, challengerId);
        challengeManager.challenge(matchId, actorId, null);

        assertNoDuplicateIds(state.getDeck(), state.getPlayers());
    }

    private void assertNoDuplicateIds(List<GameCard> deck, List<GamePlayerState> players) {
        java.util.Set<java.util.UUID> seen = new java.util.HashSet<>();
        for (GameCard card : deck) {
            assertThat(seen.add(card.getId()))
                    .as("No duplicate in deck: %s", card.getId())
                    .isTrue();
        }
        for (GamePlayerState player : players) {
            for (GameCard card : player.getCards()) {
                assertThat(seen.add(card.getId()))
                        .as("No duplicate across players: %s", card.getId())
                        .isTrue();
            }
        }
    }
}