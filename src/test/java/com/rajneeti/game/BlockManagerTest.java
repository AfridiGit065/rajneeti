package com.rajneeti.game;

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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Module 19 — Block Manager tests.
 *
 * <p>Seeds the in-memory {@link GameStore} with a real 15-card deck whose hands
 * are carved out of that same deck (so deck-integrity assertions always hold),
 * then drives {@link GameEngine} actions and {@link BlockManager#block}. Block
 * challenges are exercised through the real {@link ChallengeManager}; the
 * TurnManager is mocked and only the resolve* seams advance the turn.
 */
@ExtendWith(MockitoExtension.class)
class BlockManagerTest {

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
    private BlockManager blockManager;
    private ChallengeManager challengeManager;

    private UUID matchId;
    private UUID actorId;
    private UUID blockerId;
    private UUID challengerId;

    @BeforeEach
    void setUp() {
        matchId = UUID.randomUUID();
        actorId = UUID.randomUUID();
        blockerId = UUID.randomUUID();
        challengerId = UUID.randomUUID();

        gameEngine = new GameEngine(
                matchRepository, matchPlayerRepository, gameStore,
                cardManager, turnManager, gameStateMapper);
        blockManager = new BlockManager(gameEngine);
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
                .roomCode("RAJNAB")
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

    private PlayerSpec blocker(int coins, PlayerStatus status, CharacterType... chars) {
        return new PlayerSpec(blockerId, "blocker", coins, status, chars);
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
        when(turnManager.advanceTurn(matchId)).thenReturn(advancedMatch(blockerId, 2));
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

    private void assertBlockRejectsWith(UUID blocker, String claimed, String errorCode) {
        assertThatThrownBy(() -> blockManager.block(matchId, blocker, claimed))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(errorCode);
    }

    /* ------------------------------------------------------------------ */
    /*  Tests — blockable actions                                         */
    /* ------------------------------------------------------------------ */

    @Test
    @DisplayName("Foreign Aid can be blocked by Minister — block is recorded")
    void foreignAid_canBeBlocked_byMinister() {
        GameState state = seed(MatchStatus.IN_PROGRESS, actorId,
                actor(2, PlayerStatus.ACTIVE, CharacterType.MINISTER, CharacterType.GOYENDA),
                blocker(2, PlayerStatus.ACTIVE, CharacterType.DALAL, CharacterType.AMLA));

        gameEngine.performForeignAid(matchId, actorId);
        GameStateResponse response = blockManager.block(matchId, blockerId, "minister");

        assertThat(state.getPendingAction().getBlockerUserId()).isEqualTo(blockerId);
        assertThat(state.getPendingAction().getBlockedCharacter()).isEqualTo("minister");
        assertThat(response.getPendingAction().getBlockedCharacter()).isEqualTo("minister");
    }

    @Test
    @DisplayName("Steal can be blocked by Dalal")
    void steal_canBeBlocked_byDalal() {
        GameState state = seed(MatchStatus.IN_PROGRESS, actorId,
                actor(2, PlayerStatus.ACTIVE, CharacterType.DALAL, CharacterType.GOYENDA),
                blocker(4, PlayerStatus.ACTIVE, CharacterType.MINISTER, CharacterType.AMLA));

        gameEngine.performSteal(matchId, actorId, blockerId);
        blockManager.block(matchId, blockerId, "dalal");

        assertThat(state.getPendingAction().getBlockerUserId()).isEqualTo(blockerId);
        assertThat(state.getPendingAction().getBlockedCharacter()).isEqualTo("dalal");
    }

    @Test
    @DisplayName("Steal can be blocked by Amla")
    void steal_canBeBlocked_byAmla() {
        GameState state = seed(MatchStatus.IN_PROGRESS, actorId,
                actor(2, PlayerStatus.ACTIVE, CharacterType.DALAL, CharacterType.GOYENDA),
                blocker(4, PlayerStatus.ACTIVE, CharacterType.MINISTER, CharacterType.AMLA));

        gameEngine.performSteal(matchId, actorId, blockerId);
        blockManager.block(matchId, blockerId, "amla");

        assertThat(state.getPendingAction().getBlockedCharacter()).isEqualTo("amla");
        assertThat(state.getPendingAction().getBlockerUserId()).isEqualTo(blockerId);
    }

    @Test
    @DisplayName("Assassinate can be blocked by Goyenda")
    void assassinate_canBeBlocked_byGoyenda() {
        GameState state = seed(MatchStatus.IN_PROGRESS, actorId,
                actor(6, PlayerStatus.ACTIVE, CharacterType.GHATOK, CharacterType.GOYENDA),
                blocker(2, PlayerStatus.ACTIVE, CharacterType.MINISTER, CharacterType.AMLA));

        gameEngine.performAssassinate(matchId, actorId, blockerId);
        blockManager.block(matchId, blockerId, "goyenda");

        assertThat(state.getPendingAction().getBlockedCharacter()).isEqualTo("goyenda");
        assertThat(state.getPendingAction().getBlockerUserId()).isEqualTo(blockerId);
        assertThat(state.getPendingAction().getReservedCoins()).isEqualTo(3);
    }

    /* ------------------------------------------------------------------ */
    /*  Tests — non-blockable actions                                     */
    /* ------------------------------------------------------------------ */

    @Test
    @DisplayName("Income cannot be blocked")
    void income_cannotBeBlocked() {
        seed(MatchStatus.IN_PROGRESS, actorId,
                actor(2, PlayerStatus.ACTIVE, CharacterType.MINISTER, CharacterType.GOYENDA),
                blocker(2, PlayerStatus.ACTIVE, CharacterType.DALAL, CharacterType.AMLA));
        gameStore.get(matchId).setPendingAction(pending(actorId, "INCOME", null, null));

        assertBlockRejectsWith(blockerId, "minister", "ACTION_NOT_BLOCKABLE");
    }

    @Test
    @DisplayName("Tax cannot be blocked")
    void tax_cannotBeBlocked() {
        seed(MatchStatus.IN_PROGRESS, actorId,
                actor(2, PlayerStatus.ACTIVE, CharacterType.MINISTER, CharacterType.GOYENDA),
                blocker(2, PlayerStatus.ACTIVE, CharacterType.DALAL, CharacterType.AMLA));
        gameStore.get(matchId).setPendingAction(pending(actorId, "TAX", "minister", null));

        assertBlockRejectsWith(blockerId, "minister", "ACTION_NOT_BLOCKABLE");
    }

    @Test
    @DisplayName("Exchange cannot be blocked")
    void exchange_cannotBeBlocked() {
        seed(MatchStatus.IN_PROGRESS, actorId,
                actor(2, PlayerStatus.ACTIVE, CharacterType.AMLA, CharacterType.GOYENDA),
                blocker(2, PlayerStatus.ACTIVE, CharacterType.DALAL, CharacterType.AMLA));
        gameStore.get(matchId).setPendingAction(pending(actorId, "EXCHANGE", "amla", null));

        assertBlockRejectsWith(blockerId, "amla", "ACTION_NOT_BLOCKABLE");
    }

    @Test
    @DisplayName("Blocking without a pending action is rejected")
    void noPendingAction_rejected() {
        seed(MatchStatus.IN_PROGRESS, actorId,
                actor(2, PlayerStatus.ACTIVE, CharacterType.MINISTER, CharacterType.GOYENDA),
                blocker(2, PlayerStatus.ACTIVE, CharacterType.DALAL, CharacterType.AMLA));

        assertBlockRejectsWith(blockerId, "minister", "NO_PENDING_ACTION");
    }

    /* ------------------------------------------------------------------ */
    /*  Tests — bluff blocks are allowed                                  */
    /* ------------------------------------------------------------------ */

    @Test
    @DisplayName("A bluff block is accepted — ownership of the claimed character is not checked")
    void bluffBlock_isAllowed() {
        GameState state = seed(MatchStatus.IN_PROGRESS, actorId,
                actor(2, PlayerStatus.ACTIVE, CharacterType.GOYENDA, CharacterType.AMLA),
                blocker(2, PlayerStatus.ACTIVE, CharacterType.DALAL, CharacterType.AMLA));

        gameEngine.performForeignAid(matchId, actorId);
        // The blocker does NOT hold Minister, yet the block claim is accepted.
        GameStateResponse response = blockManager.block(matchId, blockerId, "minister");

        assertThat(state.getPendingAction().getBlockedCharacter()).isEqualTo("minister");
        assertThat(response.getPendingAction().getBlockerUserId()).isEqualTo(blockerId);
    }

    /* ------------------------------------------------------------------ */
    /*  Tests — block challenge outcomes                                  */
    /* ------------------------------------------------------------------ */

    @Test
    @DisplayName("A truthfully blocked claim stands and the challenger loses one influence")
    void blockClaim_canBeChallenged_truthful() {
        GameState state = seed(MatchStatus.IN_PROGRESS, actorId,
                actor(2, PlayerStatus.ACTIVE, CharacterType.GOYENDA, CharacterType.AMLA),
                challenger(2, PlayerStatus.ACTIVE, CharacterType.MINISTER, CharacterType.DALAL),
                blocker(2, PlayerStatus.ACTIVE, CharacterType.MINISTER, CharacterType.MINISTER));

        gameEngine.performForeignAid(matchId, actorId);
        blockManager.block(matchId, blockerId, "minister");

        GameStateResponse response = challengeManager.challenge(matchId, challengerId, null);

        assertThat(state.getLastChallenge()).isNotNull();
        assertThat(state.getLastChallenge().isBlockClaim()).isTrue();
        assertThat(state.getLastChallenge().isClaimTrue()).isTrue();
        assertThat(state.getLastChallenge().getInfluenceLostById()).isEqualTo(challengerId);
        assertThat(state.getLastChallenge().isActionContinues()).isFalse();

        // The block stands on the pending action for the actor to resolve as blocked.
        PendingAction pendingAfter = state.getPendingAction();
        assertThat(pendingAfter).isNotNull();
        assertThat(pendingAfter.getBlockerUserId()).isEqualTo(blockerId);
        assertThat(pendingAfter.getBlockedCharacter()).isEqualTo("minister");
        assertThat(pendingAfter.getBlockChallengerUserId()).isEqualTo(challengerId);

        // The challenger lost exactly one influence; the blocker proved the card
        // and keeps a full hand via the replacement draw.
        assertThat(state.getPlayers().get(1).getCards()).hasSize(1);
        assertThat(state.getPlayers().get(2).getCards()).hasSize(2);
        assertThat(state.getRevealedCardsCount()).isEqualTo(1);

        // Player-safe verdict is exposed to the challenger.
        assertThat(response.getLastChallenge().getResult()).isEqualTo("CLAIM_TRUE");
        assertThat(response.getLastChallenge().isBlockClaim()).isTrue();
        assertThat(response.getLastChallenge().getRevealedCharacterId()).isEqualTo("minister");
        assertThat(response.getPendingAction().getBlockedCharacter()).isEqualTo("minister");
    }

    @Test
    @DisplayName("A bluff block is exposed: the blocker loses one influence and the block is removed")
    void blockClaim_canBeChallenged_bluff() {
        GameState state = seed(MatchStatus.IN_PROGRESS, actorId,
                actor(2, PlayerStatus.ACTIVE, CharacterType.GOYENDA, CharacterType.AMLA),
                challenger(2, PlayerStatus.ACTIVE, CharacterType.MINISTER, CharacterType.DALAL),
                blocker(2, PlayerStatus.ACTIVE, CharacterType.DALAL, CharacterType.AMLA));

        gameEngine.performForeignAid(matchId, actorId);
        blockManager.block(matchId, blockerId, "minister"); // bluff: no Minister held

        GameStateResponse response = challengeManager.challenge(matchId, challengerId, null);

        assertThat(state.getLastChallenge().isBlockClaim()).isTrue();
        assertThat(state.getLastChallenge().isClaimTrue()).isFalse();
        assertThat(state.getLastChallenge().getInfluenceLostById()).isEqualTo(blockerId);
        assertThat(state.getLastChallenge().isActionContinues()).isTrue();

        // The block is removed but the Foreign Aid action stays pending for the actor.
        PendingAction pendingAfter = state.getPendingAction();
        assertThat(pendingAfter).isNotNull();
        assertThat(pendingAfter.getBlockerUserId()).isNull();
        assertThat(pendingAfter.getBlockedCharacter()).isNull();
        assertThat(pendingAfter.getBlockChallengerUserId()).isEqualTo(challengerId);

        GamePlayerState blockerState = state.getPlayers().get(2);
        assertThat(blockerState.getCards()).hasSize(1);
        assertThat(blockerState.getStatus()).isEqualTo(PlayerStatus.ACTIVE);

        assertThat(response.getLastChallenge().getResult()).isEqualTo("CLAIM_FALSE");
        assertThat(response.getLastChallenge().isBlockClaim()).isTrue();
        assertThat(response.getPendingAction()).isNotNull();
        assertThat(response.getPendingAction().getBlockerUserId()).isNull();
    }

    /* ------------------------------------------------------------------ */
    /*  Tests — block validity rules                                      */
    /* ------------------------------------------------------------------ */

    @Test
    @DisplayName("A block claiming a character that cannot block the action is rejected")
    void invalidBlockingCharacter_rejected() {
        seed(MatchStatus.IN_PROGRESS, actorId,
                actor(2, PlayerStatus.ACTIVE, CharacterType.MINISTER, CharacterType.GOYENDA),
                blocker(2, PlayerStatus.ACTIVE, CharacterType.GOYENDA, CharacterType.DALAL));

        gameEngine.performForeignAid(matchId, actorId);
        assertBlockRejectsWith(blockerId, "goyenda", "INVALID_BLOCKING_CHARACTER");
    }

    @Test
    @DisplayName("Eliminated players cannot block")
    void eliminatedPlayer_cannotBlock() {
        seed(MatchStatus.IN_PROGRESS, actorId,
                actor(2, PlayerStatus.ACTIVE, CharacterType.MINISTER, CharacterType.GOYENDA),
                blocker(2, PlayerStatus.ELIMINATED, CharacterType.DALAL, CharacterType.AMLA));

        gameEngine.performForeignAid(matchId, actorId);
        assertBlockRejectsWith(blockerId, "minister", "PLAYER_ELIMINATED");
    }

    @Test
    @DisplayName("A player outside the match cannot block")
    void invalidBlocker_notInMatch() {
        seed(MatchStatus.IN_PROGRESS, actorId,
                actor(2, PlayerStatus.ACTIVE, CharacterType.MINISTER, CharacterType.GOYENDA),
                blocker(2, PlayerStatus.ACTIVE, CharacterType.DALAL, CharacterType.AMLA));

        gameEngine.performForeignAid(matchId, actorId);
        assertBlockRejectsWith(UUID.randomUUID(), "minister", "PLAYER_NOT_IN_MATCH");
    }

    @Test
    @DisplayName("A player cannot block their own action")
    void cannotBlockSelf() {
        seed(MatchStatus.IN_PROGRESS, actorId,
                actor(2, PlayerStatus.ACTIVE, CharacterType.MINISTER, CharacterType.GOYENDA),
                challenger(2, PlayerStatus.ACTIVE, CharacterType.DALAL, CharacterType.AMLA));

        gameEngine.performForeignAid(matchId, actorId);
        assertBlockRejectsWith(actorId, "minister", "CANNOT_BLOCK_SELF");
    }

    @Test
    @DisplayName("Only one block is allowed per pending action")
    void duplicateBlock_rejected() {
        GameState state = seed(MatchStatus.IN_PROGRESS, actorId,
                actor(2, PlayerStatus.ACTIVE, CharacterType.DALAL, CharacterType.GOYENDA),
                blocker(4, PlayerStatus.ACTIVE, CharacterType.AMLA, CharacterType.MINISTER),
                challenger(4, PlayerStatus.ACTIVE, CharacterType.DALAL, CharacterType.MINISTER));

        gameEngine.performSteal(matchId, actorId, blockerId);
        blockManager.block(matchId, blockerId, "amla");
        assertThat(state.getPendingAction().getBlockerUserId()).isEqualTo(blockerId);

        assertBlockRejectsWith(challengerId, "dalal", "DUPLICATE_BLOCK");
    }

    /* ------------------------------------------------------------------ */
    /*  Tests — full flow                                                 */
    /* ------------------------------------------------------------------ */

    @Test
    @DisplayName("Steal → block → actor resolves blocked: action cancelled, no coins move")
    void fullFlow_actionThenBlock() {
        GameState state = seed(MatchStatus.IN_PROGRESS, actorId,
                actor(2, PlayerStatus.ACTIVE, CharacterType.DALAL, CharacterType.GOYENDA),
                blocker(4, PlayerStatus.ACTIVE, CharacterType.AMLA, CharacterType.AMLA));
        stubAdvance();

        gameEngine.performSteal(matchId, actorId, blockerId);
        blockManager.block(matchId, blockerId, "amla");

        assertThat(state.getPendingAction().getBlockedCharacter()).isEqualTo("amla");

        // Actor resolves the blocked Steal as cancelled (granted=false).
        GameStateResponse response = gameEngine.resolveSteal(matchId, actorId, false);

        assertThat(state.getPendingAction()).isNull();
        assertThat(state.getPlayers().get(0).getCoins()).isEqualTo(2);
        assertThat(state.getPlayers().get(1).getCoins()).isEqualTo(4);
        assertThat(state.getCurrentTurnPlayerId()).isEqualTo(blockerId);
        assertThat(response.getPendingAction()).isNull();
    }

    @Test
    @DisplayName("Steal → block removed by challenge → actor resolves: coins transfer")
    void fullFlow_blockChallengedThenResolves() {
        GameState state = seed(MatchStatus.IN_PROGRESS, actorId,
                actor(2, PlayerStatus.ACTIVE, CharacterType.DALAL, CharacterType.GOYENDA),
                blocker(4, PlayerStatus.ACTIVE, CharacterType.DALAL, CharacterType.AMLA),
                challenger(0, PlayerStatus.ACTIVE, CharacterType.MINISTER, CharacterType.MINISTER));
        stubAdvance();

        gameEngine.performSteal(matchId, actorId, blockerId);
        blockManager.block(matchId, challengerId, "amla"); // challenger bluffs the block

        assertThat(state.getPendingAction().getBlockerUserId()).isEqualTo(challengerId);

        // The actor exposes the bluff block — the block is removed.
        challengeManager.challenge(matchId, actorId, null);

        PendingAction pendingAfter = state.getPendingAction();
        assertThat(pendingAfter).isNotNull();
        assertThat(pendingAfter.getBlockerUserId()).isNull();
        assertThat(state.getLastChallenge().isBlockClaim()).isTrue();
        assertThat(state.getLastChallenge().isClaimTrue()).isFalse();

        // Actor resolves the now-unblocked Steal as granted (coins transfer).
        GameStateResponse response = gameEngine.resolveSteal(matchId, actorId, true);

        assertThat(state.getPlayers().get(0).getCoins()).isEqualTo(4);
        assertThat(state.getPlayers().get(1).getCoins()).isEqualTo(2);
        assertThat(response.getPendingAction()).isNull();
    }
}