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
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Module 15 — Exchange tests.
 *
 * <p>Seeds the in-memory {@link GameStore} with a real 15-card deck (so deck
 * integrity assertions hold) and drives {@link GameEngine#performExchange} and
 * {@link GameEngine#confirmExchange}. The TurnManager is mocked.
 */
@ExtendWith(MockitoExtension.class)
class GameEngineExchangeTest {

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
     * Creates a full 15-card deck and deals {@code actorHandSize} cards to the
     * actor and {@code otherHandSize} to the opponent; the remainder becomes the
     * deck. All card ids come from one deck, so deck integrity always holds.
     */
    private GameState seed(int actorHandSize, int otherHandSize,
                           UUID currentTurnPlayerId, MatchStatus status,
                           boolean actionExecuted, PendingAction pending) {
        List<GameCard> full = cardManager.createDeck();
        List<GameCard> actorCards = cardManager.drawMany(full, actorHandSize);
        List<GameCard> otherCards = cardManager.drawMany(full, otherHandSize);

        GamePlayerState actor = player(actorId, "actor", 2, PlayerStatus.ACTIVE, actorCards);
        GamePlayerState other = player(otherId, "other", 2, PlayerStatus.ACTIVE, otherCards);
        List<GamePlayerState> players = List.of(actor, other);

        GameState state = GameState.builder()
                .matchId(matchId)
                .roomId(UUID.randomUUID())
                .roomCode("RAJEX")
                .status(status)
                .phase(GameEngine.PHASE_IN_PROGRESS)
                .players(players)
                .turnOrder(players.stream().map(GamePlayerState::getUserId).toList())
                .currentTurnPlayerId(currentTurnPlayerId)
                .turnNumber(1)
                .deck(full)
                .actionExecuted(actionExecuted)
                .pendingAction(pending)
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

    private PendingAction exchangePending(UUID actorUserId, List<GameCard> pool) {
        return PendingAction.builder()
                .type(GameEngine.ACTION_EXCHANGE)
                .actorUserId(actorUserId)
                .startedAt(LocalDateTime.now())
                .claimedCharacter(GameEngine.CHARACTER_AMLA)
                .exchangePool(new ArrayList<>(pool))
                .originalHandCardIds(pool.stream().limit(2).map(GameCard::getId).toList())
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

    private void assertPerformsRejectsWith(UUID actingUserId, String errorCode) {
        assertThatThrownBy(() -> gameEngine.performExchange(matchId, actingUserId))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(errorCode);
    }

    /* ------------------------------------------------------------------ */
    /*  Tests — performExchange (initiation + pending state)              */
    /* ------------------------------------------------------------------ */

    @Test
    @DisplayName("Exchange - initiates a challenge-pending EXCHANGE action claiming Amla")
    void exchange_initiatesPendingAction() {
        GameState state = seed(2, 2, actorId, MatchStatus.IN_PROGRESS, false, null);

        GameStateResponse response = gameEngine.performExchange(matchId, actorId);

        assertThat(state.getPendingAction()).isNotNull();
        assertThat(state.getPendingAction().getType()).isEqualTo("EXCHANGE");
        assertThat(state.getPendingAction().getActorUserId()).isEqualTo(actorId);
        assertThat(state.getPendingAction().getClaimedCharacter()).isEqualTo("amla");
        assertThat(state.getPendingAction().getStartedAt()).isBeforeOrEqualTo(LocalDateTime.now());
        assertThat(state.isActionExecuted()).isTrue();
        assertThat(findPlayer(response, actorId).getInfluenceCount()).isEqualTo(4);
        verifyNoInteractions(turnManager);
    }

    @Test
    @DisplayName("Exchange - does not require ownership of Amla")
    void exchange_doesNotRequireAmla() {
        // Draws from the deterministic (unshuffled) deck yield GOYENDA cards;
        // the actor's hand never contains Amla.
        seed(2, 2, actorId, MatchStatus.IN_PROGRESS, false, null);

        assertThat(gameEngine.performExchange(matchId, actorId)).isNotNull();
    }

    @Test
    @DisplayName("Exchange - draws exactly 2 cards into the private pool, keeping 15 in play")
    void exchange_drawsTwoCards() {
        GameState state = seed(2, 2, actorId, MatchStatus.IN_PROGRESS, false, null);
        int deckBefore = state.getDeck().size();

        gameEngine.performExchange(matchId, actorId);

        assertThat(state.getDeck().size()).isEqualTo(deckBefore - 2);
        GamePlayerState actor = state.getPlayers().get(0);
        assertThat(actor.getCards()).hasSize(4);
        assertThat(state.getPendingAction().getExchangePool()).hasSize(4);
        int total = state.getDeck().size()
                + state.getPlayers().stream().mapToInt(p -> p.getCards().size()).sum();
        assertThat(total).isEqualTo(CardManager.DECK_SIZE);
    }

    @Test
    @DisplayName("Exchange - only the actor sees the pool; opponents see no private cards")
    void exchange_actorSeesPool_opponentsDoNot() {
        seed(2, 2, actorId, MatchStatus.IN_PROGRESS, false, null);
        gameEngine.performExchange(matchId, actorId);

        GameStateResponse actorView = gameEngine.getSafeGameState(matchId, actorId);
        assertThat(actorView.getPendingAction().getExchangePool()).hasSize(4);
        assertThat(findPlayer(actorView, actorId).getCards()).hasSize(4);

        GameStateResponse otherView = gameEngine.getSafeGameState(matchId, otherId);
        assertThat(otherView.getPendingAction().getType()).isEqualTo("EXCHANGE");
        assertThat(otherView.getPendingAction().getActorUserId()).isEqualTo(actorId);
        assertThat(otherView.getPendingAction().getClaimedCharacter()).isEqualTo("amla");
        assertThat(otherView.getPendingAction().getExchangePool()).isNull();
        assertThat(findPlayer(otherView, actorId).getCards()).isNull();
        assertThat(findPlayer(otherView, otherId).getCards()).hasSize(2);
    }

    @Test
    @DisplayName("Exchange - not your turn is rejected")
    void exchange_notYourTurnRejected() {
        seed(2, 2, otherId, MatchStatus.IN_PROGRESS, false, null);

        assertPerformsRejectsWith(actorId, "NOT_YOUR_TURN");
        verifyNoInteractions(turnManager);
    }

    @Test
    @DisplayName("Exchange - eliminated player is rejected")
    void exchange_eliminatedRejected() {
        GameState state = seed(2, 2, actorId, MatchStatus.IN_PROGRESS, false, null);
        state.getPlayers().get(0).setStatus(PlayerStatus.ELIMINATED);

        assertPerformsRejectsWith(actorId, "PLAYER_ELIMINATED");
        verifyNoInteractions(turnManager);
    }

    @Test
    @DisplayName("Exchange - player not in the match is rejected")
    void exchange_notInMatchRejected() {
        seed(2, 2, otherId, MatchStatus.IN_PROGRESS, false, null);

        UUID strangerId = UUID.randomUUID();
        assertPerformsRejectsWith(strangerId, "PLAYER_NOT_IN_MATCH");
        verifyNoInteractions(turnManager);
    }

    @Test
    @DisplayName("Exchange - inactive match is rejected")
    void exchange_inactiveMatchRejected() {
        seed(2, 2, actorId, MatchStatus.FINISHED, false, null);

        assertPerformsRejectsWith(actorId, "MATCH_NOT_ACTIVE");
        verifyNoInteractions(turnManager);
    }

    @Test
    @DisplayName("Exchange - duplicate action in the same turn is rejected")
    void exchange_duplicateRejected() {
        seed(2, 2, actorId, MatchStatus.IN_PROGRESS, true, null);

        assertPerformsRejectsWith(actorId, "ACTION_ALREADY_PERFORMED");
        verifyNoInteractions(turnManager);
    }

    @Test
    @DisplayName("Exchange - insufficient deck cards is handled safely")
    void exchange_insufficientDeckRejected() {
        // Nearly empty deck: only 1 card left. The draw must be refused without
        // corrupting the game state.
        List<GameCard> oneCard = new ArrayList<>(cardManager.createDeck().subList(0, 1));
        GamePlayerState actor = player(actorId, "actor", 2, PlayerStatus.ACTIVE,
                List.of(GameCard.builder().id(UUID.randomUUID()).character(CharacterType.MINISTER).build(),
                        GameCard.builder().id(UUID.randomUUID()).character(CharacterType.MINISTER).build()));
        GamePlayerState other = player(otherId, "other", 2, PlayerStatus.ACTIVE,
                List.of(GameCard.builder().id(UUID.randomUUID()).character(CharacterType.MINISTER).build(),
                        GameCard.builder().id(UUID.randomUUID()).character(CharacterType.MINISTER).build()));

        GameState state = GameState.builder()
                .matchId(matchId)
                .status(MatchStatus.IN_PROGRESS)
                .phase(GameEngine.PHASE_IN_PROGRESS)
                .players(List.of(actor, other))
                .turnOrder(List.of(actorId, otherId))
                .currentTurnPlayerId(actorId)
                .turnNumber(1)
                .deck(oneCard)
                .actionExecuted(false)
                .log(new ArrayList<>())
                .build();
        gameStore.put(matchId, state);

        assertPerformsRejectsWith(actorId, "INSUFFICIENT_DECK");
        assertThat(state.getDeck()).hasSize(1);
        assertThat(actor.getCards()).hasSize(2);
        verifyNoInteractions(turnManager);
    }

    /* ------------------------------------------------------------------ */
    /*  Tests — confirmExchange (resolution)                              */
    /* ------------------------------------------------------------------ */

    @Test
    @DisplayName("Confirm Exchange - player ends with exactly 2 cards")
    void confirmExchange_endsWithTwoCards() {
        GameState state = seed(4, 2, actorId, MatchStatus.IN_PROGRESS, true, null);
        GamePlayerState actor = state.getPlayers().get(0);
        state.setPendingAction(exchangePending(actorId, actor.getCards()));
        when(turnManager.advanceTurn(eq(matchId))).thenReturn(advancedMatch(otherId, 2));

        List<UUID> keep = actor.getCards().subList(0, 2).stream().map(GameCard::getId).toList();
        gameEngine.confirmExchange(matchId, actorId, keep);

        assertThat(actor.getCards()).hasSize(2);
        assertThat(actor.getCards().stream().map(GameCard::getId).toList()).isEqualTo(keep);
        verify(turnManager).advanceTurn(eq(matchId));
    }

    @Test
    @DisplayName("Confirm Exchange - returned cards go back to the deck")
    void confirmExchange_returnedCardsToDeck() {
        GameState state = seed(4, 2, actorId, MatchStatus.IN_PROGRESS, true, null);
        GamePlayerState actor = state.getPlayers().get(0);
        state.setPendingAction(exchangePending(actorId, actor.getCards()));
        when(turnManager.advanceTurn(eq(matchId))).thenReturn(advancedMatch(otherId, 2));
        int deckBefore = state.getDeck().size();

        List<UUID> keep = actor.getCards().subList(0, 2).stream().map(GameCard::getId).toList();
        List<UUID> returned = actor.getCards().subList(2, 4).stream().map(GameCard::getId).toList();
        gameEngine.confirmExchange(matchId, actorId, keep);

        assertThat(state.getDeck()).hasSize(deckBefore + 2);
        List<UUID> deckIds = state.getDeck().stream().map(GameCard::getId).toList();
        assertThat(deckIds).containsAll(returned);
        assertThat(deckIds).doesNotContainAnyElementsOf(keep);
    }

    @Test
    @DisplayName("Confirm Exchange - the 15-card deck integrity is preserved")
    void confirmExchange_deckIntegrityPreserved() {
        GameState state = seed(4, 2, actorId, MatchStatus.IN_PROGRESS, true, null);
        GamePlayerState actor = state.getPlayers().get(0);
        state.setPendingAction(exchangePending(actorId, actor.getCards()));
        when(turnManager.advanceTurn(eq(matchId))).thenReturn(advancedMatch(otherId, 2));

        List<UUID> keep = actor.getCards().subList(0, 2).stream().map(GameCard::getId).toList();
        gameEngine.confirmExchange(matchId, actorId, keep);

        assertThatNoException().isThrownBy(() ->
                cardManager.assertDeckIntegrity(state.getDeck(), state.getPlayers()));
    }

    @Test
    @DisplayName("Confirm Exchange - a selection not in the pool is rejected")
    void confirmExchange_invalidSelectionRejected() {
        GameState state = seed(4, 2, actorId, MatchStatus.IN_PROGRESS, true, null);
        GamePlayerState actor = state.getPlayers().get(0);
        GamePlayerState other = state.getPlayers().get(1);
        state.setPendingAction(exchangePending(actorId, actor.getCards()));

        UUID actorCard = actor.getCards().get(0).getId();
        UUID foreignCard = other.getCards().get(0).getId();

        assertThatThrownBy(() -> gameEngine.confirmExchange(matchId, actorId,
                List.of(actorCard, foreignCard)))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo("INVALID_CARD_SELECTION");
        verifyNoInteractions(turnManager);
    }

    @Test
    @DisplayName("Confirm Exchange - wrong selection size is rejected")
    void confirmExchange_wrongSelectionSizeRejected() {
        GameState state = seed(4, 2, actorId, MatchStatus.IN_PROGRESS, true, null);
        GamePlayerState actor = state.getPlayers().get(0);
        state.setPendingAction(exchangePending(actorId, actor.getCards()));

        UUID oneCard = actor.getCards().get(0).getId();

        assertThatThrownBy(() -> gameEngine.confirmExchange(matchId, actorId, List.of(oneCard)))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo("INVALID_CARD_SELECTION");
        verifyNoInteractions(turnManager);
    }

    @Test
    @DisplayName("Confirm Exchange - duplicate selection is rejected")
    void confirmExchange_duplicateSelectionRejected() {
        GameState state = seed(4, 2, actorId, MatchStatus.IN_PROGRESS, true, null);
        GamePlayerState actor = state.getPlayers().get(0);
        state.setPendingAction(exchangePending(actorId, actor.getCards()));

        UUID actorCard = actor.getCards().get(0).getId();

        assertThatThrownBy(() -> gameEngine.confirmExchange(matchId, actorId,
                List.of(actorCard, actorCard)))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo("DUPLICATE_CARD_SELECTION");
        verifyNoInteractions(turnManager);
    }

    @Test
    @DisplayName("Confirm Exchange - only the actor may keep cards")
    void confirmExchange_notActorRejected() {
        GameState state = seed(4, 2, actorId, MatchStatus.IN_PROGRESS, true, null);
        GamePlayerState actor = state.getPlayers().get(0);
        state.setPendingAction(exchangePending(actorId, actor.getCards()));

        List<UUID> keep = actor.getCards().subList(0, 2).stream().map(GameCard::getId).toList();

        assertThatThrownBy(() -> gameEngine.confirmExchange(matchId, otherId, keep))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo("NOT_ACTOR");
        verifyNoInteractions(turnManager);
    }

    @Test
    @DisplayName("Confirm Exchange - no pending action is rejected")
    void confirmExchange_noPendingRejected() {
        seed(4, 2, actorId, MatchStatus.IN_PROGRESS, true, null);

        List<UUID> keep = List.of(UUID.randomUUID(), UUID.randomUUID());

        assertThatThrownBy(() -> gameEngine.confirmExchange(matchId, actorId, keep))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo("NO_PENDING_ACTION");
    }

    @Test
    @DisplayName("Confirm Exchange - a different pending action type is rejected")
    void confirmExchange_wrongPendingTypeRejected() {
        GameState state = seed(4, 2, actorId, MatchStatus.IN_PROGRESS, true,
                PendingAction.builder()
                        .type("FOREIGN_AID").actorUserId(actorId)
                        .startedAt(LocalDateTime.now()).build());

        List<UUID> keep = List.of(UUID.randomUUID(), UUID.randomUUID());

        assertThatThrownBy(() -> gameEngine.confirmExchange(matchId, actorId, keep))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo("INVALID_PENDING_ACTION");
        assertThat(state.getPendingAction()).isNotNull();
    }

    @Test
    @DisplayName("Confirm Exchange - pending action cleared and turn state synced")
    void confirmExchange_syncsTurnState() {
        GameState state = seed(4, 2, actorId, MatchStatus.IN_PROGRESS, true, null);
        GamePlayerState actor = state.getPlayers().get(0);
        state.setPendingAction(exchangePending(actorId, actor.getCards()));
        when(turnManager.advanceTurn(eq(matchId))).thenReturn(advancedMatch(otherId, 3));

        List<UUID> keep = actor.getCards().subList(0, 2).stream().map(GameCard::getId).toList();
        gameEngine.confirmExchange(matchId, actorId, keep);

        assertThat(state.getPendingAction()).isNull();
        assertThat(state.getCurrentTurnPlayerId()).isEqualTo(otherId);
        assertThat(state.getTurnNumber()).isEqualTo(3);
        assertThat(state.isActionExecuted()).isFalse();
        assertThat(state.getPhase()).isEqualTo(GameEngine.PHASE_IN_PROGRESS);
    }

    /* ------------------------------------------------------------------ */
    /*  Tests — previous modules still functional                         */
    /* ------------------------------------------------------------------ */

    @Test
    @DisplayName("Income still works after Exchange changes")
    void existingIncome_stillWorks() {
        seed(2, 2, actorId, MatchStatus.IN_PROGRESS, false, null);
        when(turnManager.advanceTurn(eq(matchId))).thenReturn(advancedMatch(otherId, 2));

        GameStateResponse response = gameEngine.performIncome(matchId, actorId);

        assertThat(findPlayer(response, actorId).getCoins()).isEqualTo(3);
        verify(turnManager).advanceTurn(eq(matchId));
    }

    @Test
    @DisplayName("Foreign Aid still works after Exchange changes")
    void existingForeignAid_stillWorks() {
        GameState state = seed(2, 2, actorId, MatchStatus.IN_PROGRESS, false, null);

        gameEngine.performForeignAid(matchId, actorId);

        assertThat(state.getPendingAction()).isNotNull();
        assertThat(state.getPendingAction().getType()).isEqualTo("FOREIGN_AID");
        assertThat(state.isActionExecuted()).isTrue();
    }
}