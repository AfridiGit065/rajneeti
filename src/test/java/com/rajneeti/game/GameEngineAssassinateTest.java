package com.rajneeti.game;

import com.rajneeti.dto.game.GamePlayerDto;
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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Module 16 — Assassination tests.
 *
 * <p>Seeds the in-memory {@link GameStore} with a real 15-card deck (so deck
 * integrity assertions hold) and drives {@link GameEngine#performAssassinate}
 * and {@link GameEngine#resolveAssassinate}. The TurnManager is mocked.
 *
 * <p>The deterministic (unshuffled) deck yields GOYENDA cards first, so the
 * actor's hand never contains a GHATOK — exactly what the
 * "ownership not required" tests need.
 */
@ExtendWith(MockitoExtension.class)
class GameEngineAssassinateTest {

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

    private UUID matchId;
    private UUID actorId;
    private UUID otherId;

    @BeforeEach
    void setUp() {
        matchId = UUID.randomUUID();
        actorId = UUID.randomUUID();
        otherId = UUID.randomUUID();

        gameEngine = new GameEngine(
                matchRepository, matchPlayerRepository, gameStore,
                cardManager, turnManager, gameStateMapper);
        gameStore.remove(matchId);
    }

    /* ------------------------------------------------------------------ */
    /*  Helpers                                                           */
    /* ------------------------------------------------------------------ */

    /**
     * Seeds a game with a full 15-card deck. All card IDs come from one deck,
     * so deck integrity always holds.
     */
    private GameState seed(int actorCoins, int otherCoins, int targetInfluence,
                           UUID currentTurnPlayerId, PlayerStatus targetStatus) {
        List<GameCard> full = cardManager.createDeck();
        List<GameCard> actorCards = cardManager.drawMany(full, 2);
        List<GameCard> otherCards = targetInfluence > 0
                ? cardManager.drawMany(full, targetInfluence)
                : new ArrayList<>();

        GamePlayerState actor = player(actorId, "actor", actorCoins, PlayerStatus.ACTIVE, actorCards);
        GamePlayerState other = player(otherId, "other", otherCoins, targetStatus, otherCards);
        List<GamePlayerState> players = List.of(actor, other);

        GameState state = GameState.builder()
                .matchId(matchId)
                .roomId(UUID.randomUUID())
                .roomCode("RAJASN")
                .status(MatchStatus.IN_PROGRESS)
                .phase(GameEngine.PHASE_IN_PROGRESS)
                .players(players)
                .turnOrder(players.stream().map(GamePlayerState::getUserId).toList())
                .currentTurnPlayerId(currentTurnPlayerId)
                .turnNumber(1)
                .deck(full)
                .log(new ArrayList<>())
                .build();
        gameStore.put(matchId, state);
        return state;
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

    private GamePlayerDto findPlayer(GameStateResponse response, UUID userId) {
        return response.getPlayers().stream()
                .filter(p -> p.getUserId().equals(userId))
                .findFirst()
                .orElseThrow();
    }

    private void stubAdvance() {
        when(turnManager.advanceTurn(matchId)).thenReturn(advancedMatch(otherId, 2));
    }

    private void assertPerformsRejectWith(int actorCoins, int targetInfluence,
                                          PlayerStatus targetStatus, UUID targetId,
                                          String errorCode) {
        seed(actorCoins, 2, targetInfluence, actorId, targetStatus);
        assertThatThrownBy(() -> gameEngine.performAssassinate(matchId, actorId, targetId))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(errorCode);
    }

    /* ------------------------------------------------------------------ */
    /*  Tests — performAssassinate (initiation + pending state)           */
    /* ------------------------------------------------------------------ */

    @Test
    @DisplayName("Assassinate - initiates a block/challenge-pending ASSASSINATE claiming GHATOK")
    void assassinate_initiatesPendingAction() {
        GameState state = seed(3, 2, 2, actorId, PlayerStatus.ACTIVE);

        GameStateResponse response = gameEngine.performAssassinate(matchId, actorId, otherId);

        assertThat(state.getPendingAction()).isNotNull();
        assertThat(state.getPendingAction().getType()).isEqualTo("ASSASSINATE");
        assertThat(state.getPendingAction().getActorUserId()).isEqualTo(actorId);
        assertThat(state.getPendingAction().getClaimedCharacter()).isEqualTo("ghatok");
        assertThat(state.getPendingAction().getTargetPlayerId()).isEqualTo(otherId);
        assertThat(state.getPendingAction().getReservedCoins()).isEqualTo(3);
        assertThat(state.getPendingAction().getStartedAt()).isBeforeOrEqualTo(LocalDateTime.now());
        assertThat(state.isActionExecuted()).isTrue();
        assertThat(response.getPendingAction().getType()).isEqualTo("ASSASSINATE");
        assertThat(response.getPendingAction().getTargetPlayerId()).isEqualTo(otherId);
        assertThat(response.getPendingAction().getClaimedCharacter()).isEqualTo("ghatok");
        verifyNoInteractions(turnManager);
    }

    @Test
    @DisplayName("Assassinate - does not require ownership of a GHATOK card (bluff allowed)")
    void assassinate_doesNotRequireGhatok() {
        // Deterministic deck deals GOYENDA cards first; the actor holds no GHATOK.
        seed(3, 2, 2, actorId, PlayerStatus.ACTIVE);

        assertThat(gameEngine.performAssassinate(matchId, actorId, otherId)).isNotNull();
    }

    @Test
    @DisplayName("Assassinate - accepts a balance of exactly 3 coins")
    void assassinate_acceptsExactlyThreeCoins() {
        GameState state = seed(3, 2, 2, actorId, PlayerStatus.ACTIVE);

        gameEngine.performAssassinate(matchId, actorId, otherId);

        assertThat(state.getPendingAction()).isNotNull();
    }

    @Test
    @DisplayName("Assassinate - reserves 3 coins without deducting them up front")
    void assassinate_doesNotDeductCoinsUpFront() {
        GameState state = seed(5, 2, 2, actorId, PlayerStatus.ACTIVE);
        int coinsBefore = state.getPlayers().get(0).getCoins();

        gameEngine.performAssassinate(matchId, actorId, otherId);

        assertThat(state.getPlayers().get(0).getCoins()).isEqualTo(coinsBefore);
        assertThat(state.getPendingAction().getReservedCoins()).isEqualTo(3);
    }

    @Test
    @DisplayName("Assassinate - no influence is lost before resolution")
    void assassinate_noImmediateInfluenceLoss() {
        GameState state = seed(3, 2, 2, actorId, PlayerStatus.ACTIVE);
        int targetInfluenceBefore = state.getPlayers().get(1).getCards().size();

        gameEngine.performAssassinate(matchId, actorId, otherId);

        assertThat(state.getPlayers().get(1).getCards().size()).isEqualTo(targetInfluenceBefore);
    }

    @Test
    @DisplayName("Assassinate - rejects when the actor has fewer than 3 coins")
    void assassinate_rejectsInsufficientCoins() {
        assertPerformsRejectWith(2, 2, PlayerStatus.ACTIVE, otherId, "INSUFFICIENT_COINS");
    }

    @Test
    @DisplayName("Assassinate - rejects targeting yourself")
    void assassinate_rejectsSelfTarget() {
        assertPerformsRejectWith(3, 2, PlayerStatus.ACTIVE, actorId, "TARGET_SELF");
    }

    @Test
    @DisplayName("Assassinate - rejects targeting an eliminated player")
    void assassinate_rejectsEliminatedTarget() {
        assertPerformsRejectWith(3, 2, PlayerStatus.ELIMINATED, otherId, "TARGET_ELIMINATED");
    }

    @Test
    @DisplayName("Assassinate - rejects a target with no influence cards")
    void assassinate_rejectsTargetWithoutInfluence() {
        assertPerformsRejectWith(3, 0, PlayerStatus.ACTIVE, otherId, "TARGET_NO_INFLUENCE");
    }

    @Test
    @DisplayName("Assassinate - rejects a target that is not in the match")
    void assassinate_rejectsUnknownTarget() {
        seed(3, 2, 2, actorId, PlayerStatus.ACTIVE);
        assertThatThrownBy(() -> gameEngine.performAssassinate(
                matchId, actorId, UUID.randomUUID()))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo("TARGET_NOT_IN_MATCH");
    }

    /* ------------------------------------------------------------------ */
    /*  Tests — resolveAssassinate (cost is paid only on success)         */
    /* ------------------------------------------------------------------ */

    @Test
    @DisplayName("Assassinate - successful resolution deducts 3 coins and removes one card")
    void assassinate_successDeductsCostAndOneCard() {
        GameState state = seed(3, 5, 2, actorId, PlayerStatus.ACTIVE);
        stubAdvance();

        gameEngine.performAssassinate(matchId, actorId, otherId);
        assertThat(state.getPlayers().get(0).getCoins()).isEqualTo(3);

        GameStateResponse response = gameEngine.resolveAssassinate(matchId, actorId, true);

        GamePlayerState actor = state.getPlayers().get(0);
        GamePlayerState target = state.getPlayers().get(1);
        assertThat(actor.getCoins()).isZero();
        assertThat(target.getCards()).hasSize(1);
        assertThat(state.getPendingAction()).isNull();
        assertThat(state.getRevealedCardsCount()).isEqualTo(1);
        assertThat(state.isActionExecuted()).isFalse();
        assertThat(findPlayer(response, actorId).getCoins()).isZero();
    }

    @Test
    @DisplayName("Assassinate - cancelled resolution deducts nothing and keeps the target intact")
    void assassinate_cancelledDeductsNothing() {
        GameState state = seed(3, 5, 2, actorId, PlayerStatus.ACTIVE);
        stubAdvance();

        gameEngine.performAssassinate(matchId, actorId, otherId);
        GameStateResponse response = gameEngine.resolveAssassinate(matchId, actorId, false);

        GamePlayerState actor = state.getPlayers().get(0);
        GamePlayerState target = state.getPlayers().get(1);
        assertThat(actor.getCoins()).isEqualTo(3);
        assertThat(target.getCards()).hasSize(2);
        assertThat(target.getStatus()).isEqualTo(PlayerStatus.ACTIVE);
        assertThat(state.getPendingAction()).isNull();
        assertThat(state.getRevealedCardsCount()).isZero();
        assertThat(state.isActionExecuted()).isFalse();
        assertThat(findPlayer(response, actorId).getCoins()).isEqualTo(3);
    }

    @Test
    @DisplayName("Assassinate - removing the target's last card eliminates the target")
    void assassinate_lastCardEliminatesTarget() {
        GameState state = seed(3, 5, 1, actorId, PlayerStatus.ACTIVE);
        stubAdvance();

        gameEngine.performAssassinate(matchId, actorId, otherId);
        gameEngine.resolveAssassinate(matchId, actorId, true);

        GamePlayerState target = state.getPlayers().get(1);
        assertThat(target.getCards()).isEmpty();
        assertThat(target.getStatus()).isEqualTo(PlayerStatus.ELIMINATED);
        assertThat(state.getRevealedCardsCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("Assassinate - the balance can never go negative on payout")
    void assassinate_neverGoesNegative() {
        GameState state = seed(3, 5, 1, actorId, PlayerStatus.ACTIVE);
        stubAdvance();

        gameEngine.performAssassinate(matchId, actorId, otherId);
        gameEngine.resolveAssassinate(matchId, actorId, true);

        assertThat(state.getPlayers().get(0).getCoins()).isZero();
    }

    /* ------------------------------------------------------------------ */
    /*  Tests — earlier modules still work                                 */
    /* ------------------------------------------------------------------ */

    @Test
    @DisplayName("Assassinate - previous actions (income, foreign aid) still work alongside it")
    void assassinate_previousModulesStillWork() {
        GameState state = seed(2, 5, 1, actorId, PlayerStatus.ACTIVE);
        stubAdvance();

        GameStateResponse income = gameEngine.performIncome(matchId, actorId);
        assertThat(findPlayer(income, actorId).getCoins()).isEqualTo(3);

        // Income advanced the turn to the opponent; bring it back so the actor
        // can demonstrate a block-window action in the same test.
        state.setCurrentTurnPlayerId(actorId);
        gameEngine.performForeignAid(matchId, actorId);
        assertThat(state.getPendingAction().getType()).isEqualTo("FOREIGN_AID");

        gameEngine.resolveForeignAid(matchId, actorId, false);
        assertThat(state.getPlayers().get(0).getCoins()).isEqualTo(5);
        assertThat(state.getPendingAction()).isNull();
    }
}