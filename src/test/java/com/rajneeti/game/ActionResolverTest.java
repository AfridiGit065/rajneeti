package com.rajneeti.game;

import com.rajneeti.dto.game.GameStateResponse;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Module 20 — Action Resolver tests.
 *
 * <p>Seeds the in-memory {@link GameStore} exactly like the Assassination tests
 * and drives the <b>real</b> {@link GameEngine}, {@link ChallengeManager} and
 * {@link BlockManager} behind {@link ActionResolver}, so the "backend derives
 * the outcome" contract is verified end-to-end: the resolver always learns
 * whether the action was blocked from the pending action's server-side flags,
 * never from a client boolean.
 *
 * <p>The deterministic (unshuffled) deck deals GOYENDA cards first, so bluff
 * claims and bluff blocks are easy to arrange; the {@code deckDealingHeld}
 * helper swaps the deal to a specific character for truthful-claim tests.
 */
@ExtendWith(MockitoExtension.class)
class ActionResolverTest {

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
    private BlockManager blockManager;
    private ActionResolver actionResolver;

    private UUID matchId;
    private UUID actorId;
    private UUID otherId;
    private UUID thirdId;

    @BeforeEach
    void setUp() {
        matchId = UUID.randomUUID();
        actorId = UUID.randomUUID();
        otherId = UUID.randomUUID();
        thirdId = UUID.randomUUID();

        WinnerManager winnerManager = new WinnerManager(matchRepository, matchPlayerRepository);
        gameEngine = new GameEngine(
                matchRepository, matchPlayerRepository, gameStore,
                cardManager, turnManager, gameStateMapper, winnerManager);
        challengeManager = new ChallengeManager(gameEngine, cardManager, turnManager, winnerManager);
        blockManager = new BlockManager(gameEngine);
        actionResolver = new ActionResolver(gameEngine, gameStateMapper, winnerManager);
        gameStore.remove(matchId);
    }

    /* ------------------------------------------------------------------ */
    /*  Helpers                                                           */
    /* ------------------------------------------------------------------ */

    private GameState seed(List<GameCard> deck, List<GamePlayerState> players, UUID currentTurn) {
        GameState state = GameState.builder()
                .matchId(matchId)
                .roomId(UUID.randomUUID())
                .roomCode("RAJSN")
                .status(MatchStatus.IN_PROGRESS)
                .phase(GameEngine.PHASE_IN_PROGRESS)
                .players(players)
                .turnOrder(players.stream().map(GamePlayerState::getUserId).toList())
                .currentTurnPlayerId(currentTurn)
                .turnNumber(1)
                .deck(deck)
                .log(new ArrayList<>())
                .build();
        gameStore.put(matchId, state);
        return state;
    }

    /** Two-player world: actor + target/other, both alive, standard deck. */
    private GameState seedTwo(int actorCoins, int otherCoins, int otherInfluence) {
        List<GameCard> deck = cardManager.createDeck();
        List<GameCard> actorCards = cardManager.drawMany(deck, 2);
        List<GameCard> otherCards = otherInfluence > 0
                ? cardManager.drawMany(deck, otherInfluence)
                : new ArrayList<>();
        return seed(deck, List.of(
                player(actorId, "actor", actorCoins, PlayerStatus.ACTIVE, actorCards),
                player(otherId, "other", otherCoins, PlayerStatus.ACTIVE, otherCards)),
                actorId);
    }

    /** Three-player world: actor, blocker, target — for real block challenges. */
    private GameState seedThree(int actorCoins, int blockerCoins, int targetCoins) {
        List<GameCard> deck = cardManager.createDeck();
        List<GameCard> actorCards = cardManager.drawMany(deck, 2);
        List<GameCard> blockerCards = cardManager.drawMany(deck, 2);
        List<GameCard> targetCards = cardManager.drawMany(deck, 2);
        return seed(deck, List.of(
                player(actorId, "actor", actorCoins, PlayerStatus.ACTIVE, actorCards),
                player(otherId, "blocker", blockerCoins, PlayerStatus.ACTIVE, blockerCards),
                player(thirdId, "target", targetCoins, PlayerStatus.ACTIVE, targetCards)),
                actorId);
    }

    /**
     * Standard deck mutated so the actor draws two copies of {@code held}: the
     * two tail GOYENDAs are replaced by two fresh copies of the desired
     * character (deck integrity preserved, one shared pool).
     */
    private List<GameCard> deckDealingHeld(CharacterType held) {
        List<GameCard> deck = cardManager.createDeck();
        deck.remove(deck.size() - 1);
        deck.remove(deck.size() - 1);
        deck.add(GameCard.builder().id(UUID.randomUUID()).character(held).build());
        deck.add(GameCard.builder().id(UUID.randomUUID()).character(held).build());
        return deck;
    }

    private GamePlayerState player(UUID userId, String username, int coins,
                                   PlayerStatus status, List<GameCard> cards) {
        return GamePlayerState.builder()
                .userId(userId)
                .username(username)
                .seatNumber(1)
                .status(status)
                .coins(coins)
                .cards(cards)
                .host(false)
                .build();
    }

    private Match advancedMatch(UUID nextPlayerId, int turnNumber) {
        return Match.builder()
                .id(matchId)
                .status(MatchStatus.IN_PROGRESS)
                .currentTurnPlayerId(nextPlayerId)
                .turnNumber(turnNumber)
                .build();
    }

    /** The turn advances to {@code otherId} (turn 2) whenever a seam resolves. */
    private void stubAdvance() {
        when(turnManager.advanceTurn(matchId)).thenReturn(advancedMatch(otherId, 2));
    }

    private GamePlayerState byId(GameState state, UUID userId) {
        return state.getPlayers().stream()
                .filter(p -> p.getUserId().equals(userId))
                .findFirst()
                .orElseThrow();
    }

    /* ------------------------------------------------------------------ */
    /*  Validation — no pending action, wrong caller, duplicates           */
    /* ------------------------------------------------------------------ */

    @Test
    @DisplayName("resolve - rejects when there is no pending action")
    void resolve_noPendingAction() {
        seedTwo(2, 2, 2);

        assertThatThrownBy(() -> actionResolver.resolve(matchId, actorId))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo("NO_PENDING_ACTION");
    }

    @Test
    @DisplayName("resolve - rejects when called by someone who is not the actor")
    void resolve_notActor() {
        GameState state = seedTwo(2, 2, 2);
        gameEngine.performForeignAid(matchId, actorId);
        assertThat(state.getPendingAction()).isNotNull();

        assertThatThrownBy(() -> actionResolver.resolve(matchId, otherId))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo("NOT_ACTOR");
    }

    @Test
    @DisplayName("resolve - Income resolves instantly and can never be pending")
    void resolve_incomeNeverPending() {
        seedTwo(2, 2, 2);
        stubAdvance();

        gameEngine.performIncome(matchId, actorId);
        assertThat(gameStore.get(matchId).getPendingAction()).isNull();

        assertThatThrownBy(() -> actionResolver.resolve(matchId, actorId))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo("NO_PENDING_ACTION");
    }

    @Test
    @DisplayName("resolve - Coup resolves instantly and can never be pending")
    void resolve_coupNeverPending() {
        GameState state = seedTwo(10, 3, 2);
        stubAdvance();

        gameEngine.performCoup(matchId, actorId, otherId);
        assertThat(state.getPendingAction()).isNull();

        assertThatThrownBy(() -> actionResolver.resolve(matchId, actorId))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo("NO_PENDING_ACTION");
    }

    @Test
    @DisplayName("resolve - a second resolution of the same action is rejected")
    void resolve_duplicateResolutionRejected() {
        GameState state = seedTwo(2, 2, 2);
        stubAdvance();

        gameEngine.performForeignAid(matchId, actorId);
        actionResolver.resolve(matchId, actorId);
        assertThat(state.getLastActionResult()).isNotNull();

        assertThatThrownBy(() -> actionResolver.resolve(matchId, actorId))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo("NO_PENDING_ACTION");
    }

    /* ------------------------------------------------------------------ */
    /*  Foreign Aid                                                        */
    /* ------------------------------------------------------------------ */

    @Test
    @DisplayName("Foreign Aid - unblocked resolution awards 2 coins and records RESOLVED")
    void foreignAid_unblockedResolves() {
        GameState state = seedTwo(2, 2, 2);
        stubAdvance();

        gameEngine.performForeignAid(matchId, actorId);
        assertThat(state.getLastActionResult()).isNull();

        GameStateResponse response = actionResolver.resolve(matchId, actorId);

        assertThat(byId(state, actorId).getCoins()).isEqualTo(4);
        assertThat(state.getPendingAction()).isNull();
        assertThat(state.isActionExecuted()).isFalse();

        GameActionResult result = state.getLastActionResult();
        assertThat(result).isNotNull();
        assertThat(result.getActionType()).isEqualTo(GameEngine.ACTION_FOREIGN_AID);
        assertThat(result.getResult()).isEqualTo(GameActionResult.RESULT_RESOLVED);
        assertThat(result.getActorUserId()).isEqualTo(actorId);
        assertThat(result.getCoinsGained()).isEqualTo(2);
        assertThat(result.getCoinsLost()).isZero();
        assertThat(result.getBlockedByUserId()).isNull();
        assertThat(result.getBlockedCharacter()).isNull();
        assertThat(result.isClaimChallenged()).isFalse();
        assertThat(result.getInfluenceLostById()).isNull();
        assertThat(result.isEliminated()).isFalse();
        assertThat(result.getNextTurnPlayerId()).isEqualTo(otherId);
        assertThat(result.getNextTurnNumber()).isEqualTo(2);

        assertThat(response.getLastActionResult()).isNotNull();
        assertThat(response.getLastActionResult().getCoinsGained()).isEqualTo(2);
    }

    @Test
    @DisplayName("Foreign Aid - a real Minister block cancels it and records CANCELLED")
    void foreignAid_blockedCancels() {
        GameState state = seedTwo(2, 2, 2);
        stubAdvance();

        gameEngine.performForeignAid(matchId, actorId);
        UUID pendingId = state.getPendingAction().getId();
        assertThat(pendingId).isNotNull();

        blockManager.block(matchId, otherId, GameEngine.CHARACTER_MINISTER);
        // The block rebuild keeps the same lifecycle id.
        assertThat(state.getPendingAction().getId()).isEqualTo(pendingId);
        assertThat(state.getPendingAction().getBlockerUserId()).isEqualTo(otherId);

        actionResolver.resolve(matchId, actorId);

        assertThat(byId(state, actorId).getCoins()).isEqualTo(2);
        GameActionResult result = state.getLastActionResult();
        assertThat(result.getResult()).isEqualTo(GameActionResult.RESULT_CANCELLED);
        assertThat(result.getCoinsGained()).isZero();
        assertThat(result.getBlockedByUserId()).isEqualTo(otherId);
        assertThat(result.getBlockedCharacter()).isEqualTo(GameEngine.CHARACTER_MINISTER);
    }

    /* ------------------------------------------------------------------ */
    /*  Tax                                                                */
    /* ------------------------------------------------------------------ */

    @Test
    @DisplayName("Tax - unchallenged resolution awards 3 coins")
    void tax_noChallengeGrantsThree() {
        GameState state = seedTwo(2, 2, 2);
        stubAdvance();

        gameEngine.performTax(matchId, actorId);
        actionResolver.resolve(matchId, actorId);

        assertThat(byId(state, actorId).getCoins()).isEqualTo(5);
        GameActionResult result = state.getLastActionResult();
        assertThat(result.getActionType()).isEqualTo(GameEngine.ACTION_TAX);
        assertThat(result.getResult()).isEqualTo(GameActionResult.RESULT_RESOLVED);
        assertThat(result.getCoinsGained()).isEqualTo(3);
    }

    @Test
    @DisplayName("Tax - a successfully-challenged truthful claim still awards the coins")
    void tax_successfullyChallengedTruthfulContinues() {
        List<GameCard> deck = deckDealingHeld(CharacterType.MINISTER);
        List<GameCard> actorCards = cardManager.drawMany(deck, 2);
        List<GameCard> challengerCards = cardManager.drawMany(deck, 2);
        GameState state = seed(deck, List.of(
                player(actorId, "actor", 2, PlayerStatus.ACTIVE, actorCards),
                player(otherId, "challenger", 2, PlayerStatus.ACTIVE, challengerCards)),
                actorId);
        stubAdvance();

        gameEngine.performTax(matchId, actorId);

        challengeManager.challenge(matchId, otherId, null);
        // The truthful claim keeps the pending action open, marked as challenged.
        assertThat(state.getPendingAction()).isNotNull();
        assertThat(state.getPendingAction().getChallengerUserId()).isEqualTo(otherId);
        assertThat(state.getLastChallenge().isClaimTrue()).isTrue();

        actionResolver.resolve(matchId, actorId);

        assertThat(byId(state, actorId).getCoins()).isEqualTo(5);
        GameActionResult result = state.getLastActionResult();
        assertThat(result.getResult()).isEqualTo(GameActionResult.RESULT_RESOLVED);
        assertThat(result.getCoinsGained()).isEqualTo(3);
        assertThat(result.isClaimChallenged()).isTrue();
    }

    /* ------------------------------------------------------------------ */
    /*  Steal                                                              */
    /* ------------------------------------------------------------------ */

    @Test
    @DisplayName("Steal - unblocked resolution transfers up to 2 from target to actor")
    void steal_unblockedTransfers() {
        GameState state = seedTwo(5, 3, 2);
        stubAdvance();

        gameEngine.performSteal(matchId, actorId, otherId);
        actionResolver.resolve(matchId, actorId);

        assertThat(byId(state, actorId).getCoins()).isEqualTo(7);
        assertThat(byId(state, otherId).getCoins()).isEqualTo(1);
        GameActionResult result = state.getLastActionResult();
        assertThat(result.getResult()).isEqualTo(GameActionResult.RESULT_RESOLVED);
        assertThat(result.getCoinsGained()).isEqualTo(2);
        assertThat(result.getCoinsLost()).isEqualTo(2);
    }

    @Test
    @DisplayName("Steal - the transfer is capped by the target's balance")
    void steal_cappedByTarget() {
        GameState state = seedTwo(5, 1, 2);
        stubAdvance();

        gameEngine.performSteal(matchId, actorId, otherId);
        actionResolver.resolve(matchId, actorId);

        assertThat(byId(state, actorId).getCoins()).isEqualTo(6);
        assertThat(byId(state, otherId).getCoins()).isZero();
        GameActionResult result = state.getLastActionResult();
        assertThat(result.getCoinsGained()).isEqualTo(1);
        assertThat(result.getCoinsLost()).isEqualTo(1);
    }

    @Test
    @DisplayName("Steal - a real Dalal block cancels it with no transfer")
    void steal_blockedCancels() {
        GameState state = seedTwo(5, 3, 2);
        stubAdvance();

        gameEngine.performSteal(matchId, actorId, otherId);
        blockManager.block(matchId, otherId, GameEngine.CHARACTER_DALAL);
        actionResolver.resolve(matchId, actorId);

        assertThat(byId(state, actorId).getCoins()).isEqualTo(5);
        assertThat(byId(state, otherId).getCoins()).isEqualTo(3);
        GameActionResult result = state.getLastActionResult();
        assertThat(result.getResult()).isEqualTo(GameActionResult.RESULT_CANCELLED);
        assertThat(result.getCoinsGained()).isZero();
        assertThat(result.getCoinsLost()).isZero();
        assertThat(result.getBlockedByUserId()).isEqualTo(otherId);
    }

    @Test
    @DisplayName("Steal - a bluffed block that is challenged is removed, then the steal resolves")
    void steal_bluffBlockExposedStillResolves() {
        GameState state = seedThree(5, 3, 3);
        stubAdvance();

        gameEngine.performSteal(matchId, actorId, thirdId);
        UUID pendingId = state.getPendingAction().getId();

        // Block by a player who does NOT own a Dalal card (bluff), challenged
        // successfully by the actor: the block is removed, the action continues.
        blockManager.block(matchId, otherId, GameEngine.CHARACTER_DALAL);
        assertThat(state.getPendingAction().getId()).isEqualTo(pendingId);

        challengeManager.challenge(matchId, actorId, null);
        assertThat(state.getPendingAction().getBlockerUserId()).isNull();
        assertThat(state.getPendingAction().getBlockChallengerUserId()).isEqualTo(actorId);
        assertThat(byId(state, otherId).getCards()).hasSize(1); // blocker lost a card

        actionResolver.resolve(matchId, actorId);

        assertThat(byId(state, actorId).getCoins()).isEqualTo(7);
        assertThat(byId(state, thirdId).getCoins()).isEqualTo(1);
        GameActionResult result = state.getLastActionResult();
        assertThat(result.getResult()).isEqualTo(GameActionResult.RESULT_RESOLVED);
        assertThat(result.getCoinsGained()).isEqualTo(2);
        assertThat(result.getBlockedByUserId()).isNull();
    }

    /* ------------------------------------------------------------------ */
    /*  Assassinate                                                        */
    /* ------------------------------------------------------------------ */

    @Test
    @DisplayName("Assassinate - unblocked resolution pays 3 coins and removes one card")
    void assassinate_unblockedResolves() {
        GameState state = seedTwo(3, 5, 2);
        stubAdvance();

        gameEngine.performAssassinate(matchId, actorId, otherId);
        actionResolver.resolve(matchId, actorId);

        assertThat(byId(state, actorId).getCoins()).isZero();
        assertThat(byId(state, otherId).getCards()).hasSize(1);
        assertThat(state.getRevealedCardsCount()).isEqualTo(1);
        GameActionResult result = state.getLastActionResult();
        assertThat(result.getResult()).isEqualTo(GameActionResult.RESULT_RESOLVED);
        assertThat(result.getCoinsGained()).isEqualTo(-3);
        assertThat(result.getInfluenceLostById()).isEqualTo(otherId);
        assertThat(result.isEliminated()).isFalse();
    }

    @Test
    @DisplayName("Assassinate - a real Goyenda block cancels it: no cost, no card lost")
    void assassinate_blockedCancels() {
        GameState state = seedTwo(3, 5, 2);
        stubAdvance();

        gameEngine.performAssassinate(matchId, actorId, otherId);
        blockManager.block(matchId, otherId, GameEngine.CHARACTER_GOYENDA);
        actionResolver.resolve(matchId, actorId);

        assertThat(byId(state, actorId).getCoins()).isEqualTo(3);
        assertThat(byId(state, otherId).getCards()).hasSize(2);
        assertThat(state.getRevealedCardsCount()).isZero();
        GameActionResult result = state.getLastActionResult();
        assertThat(result.getResult()).isEqualTo(GameActionResult.RESULT_CANCELLED);
        assertThat(result.getCoinsGained()).isZero();
        assertThat(result.getInfluenceLostById()).isNull();
        assertThat(result.isEliminated()).isFalse();
        assertThat(result.getBlockedByUserId()).isEqualTo(otherId);
    }

    @Test
    @DisplayName("Assassinate - removing the target's last card eliminates them")
    void assassinate_eliminatesOnLastCard() {
        GameState state = seedTwo(3, 5, 1);
        stubAdvance();

        gameEngine.performAssassinate(matchId, actorId, otherId);
        actionResolver.resolve(matchId, actorId);

        assertThat(byId(state, otherId).getCards()).isEmpty();
        assertThat(byId(state, otherId).getStatus()).isEqualTo(PlayerStatus.ELIMINATED);
        GameActionResult result = state.getLastActionResult();
        assertThat(result.getInfluenceLostById()).isEqualTo(otherId);
        assertThat(result.isEliminated()).isTrue();
    }

    /* ------------------------------------------------------------------ */
    /*  Exchange                                                           */
    /* ------------------------------------------------------------------ */

    @Test
    @DisplayName("Exchange - the generic resolver refuses: card choice must come via /exchange/confirm")
    void exchange_rejectedByGenericResolver() {
        GameState state = seedTwo(3, 2, 2);

        gameEngine.performExchange(matchId, actorId);
        assertThat(state.getPendingAction()).isNotNull();

        assertThatThrownBy(() -> actionResolver.resolve(matchId, actorId))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo("EXCHANGE_CARD_CHOICE_REQUIRED");
    }
}