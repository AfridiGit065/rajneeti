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
import com.rajneeti.websocket.WebSocketEventPublisher;
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
 * Unit tests for Steal action (+2 actor, -2 target) and edge cases.
 */
@ExtendWith(MockitoExtension.class)
class GameEngineStealTest {

    @Mock
    private MatchRepository matchRepository;

    @Mock
    private MatchPlayerRepository matchPlayerRepository;

    @Mock
    private TurnManager turnManager;

    @Mock
    private WebSocketEventPublisher webSocketEventPublisher;

    private final GameStore gameStore = new GameStore();
    private final CardManager cardManager = new CardManager();
    private final GameStateMapper gameStateMapper = new GameStateMapper();

    private GameEngine gameEngine;
    private UUID matchId;
    private UUID actorId;
    private UUID targetId;

    @BeforeEach
    void setUp() {
        matchId = UUID.randomUUID();
        actorId = UUID.randomUUID();
        targetId = UUID.randomUUID();
        gameEngine = new GameEngine(
                matchRepository, matchPlayerRepository, gameStore,
                cardManager, turnManager, gameStateMapper,
                new WinnerManager(matchRepository, matchPlayerRepository, webSocketEventPublisher),
                webSocketEventPublisher,
                new GameStateSyncService(gameStateMapper, webSocketEventPublisher));
        gameStore.remove(matchId);
    }

    private GameState seed(int actorCoins, int targetCoins, PlayerStatus targetStatus) {
        GamePlayerState actor = GamePlayerState.builder()
                .userId(actorId)
                .username("actor")
                .seatNumber(1)
                .status(PlayerStatus.ACTIVE)
                .coins(actorCoins)
                .cards(List.of(GameCard.builder().id(UUID.randomUUID()).character(CharacterType.DALAL).build()))
                .build();

        GamePlayerState target = GamePlayerState.builder()
                .userId(targetId)
                .username("target")
                .seatNumber(2)
                .status(targetStatus)
                .coins(targetCoins)
                .cards(List.of(GameCard.builder().id(UUID.randomUUID()).character(CharacterType.GHATOK).build()))
                .build();

        GameState state = GameState.builder()
                .matchId(matchId)
                .roomId(UUID.randomUUID())
                .roomCode("RJNSTL")
                .status(MatchStatus.IN_PROGRESS)
                .phase(GameEngine.PHASE_IN_PROGRESS)
                .players(new ArrayList<>(List.of(actor, target)))
                .turnOrder(List.of(actorId, targetId))
                .currentTurnPlayerId(actorId)
                .turnNumber(1)
                .deck(new ArrayList<>())
                .actionExecuted(false)
                .log(new ArrayList<>())
                .build();

        gameStore.put(matchId, state);
        return state;
    }

    @Test
    @DisplayName("Steal - declaring Steal creates pending action claiming Dalal with target")
    void performSteal_createsPendingActionClaimingDalal() {
        seed(2, 4, PlayerStatus.ACTIVE);

        GameStateResponse response = gameEngine.performSteal(matchId, actorId, targetId);

        assertThat(response.getPendingAction()).isNotNull();
        assertThat(response.getPendingAction().getType()).isEqualTo("STEAL");
        assertThat(response.getPendingAction().getClaimedCharacter()).isEqualTo("dalal");
        assertThat(response.getPendingAction().getActorUserId()).isEqualTo(actorId);
        assertThat(response.getPendingAction().getTargetPlayerId()).isEqualTo(targetId);
    }

    @Test
    @DisplayName("Steal - resolveSteal transfers 2 coins when target has >= 2 coins")
    void resolveSteal_transfersTwoCoins() {
        seed(2, 4, PlayerStatus.ACTIVE);
        gameEngine.performSteal(matchId, actorId, targetId);

        when(turnManager.advanceTurn(matchId)).thenReturn(Match.builder()
                .id(matchId)
                .currentTurnPlayerId(targetId)
                .turnNumber(2)
                .build());

        GameStateResponse response = gameEngine.resolveSteal(matchId, actorId, true);

        GamePlayerDto actorDto = response.getPlayers().stream()
                .filter(p -> p.getUserId().equals(actorId)).findFirst().orElseThrow();
        GamePlayerDto targetDto = response.getPlayers().stream()
                .filter(p -> p.getUserId().equals(targetId)).findFirst().orElseThrow();

        assertThat(actorDto.getCoins()).isEqualTo(4); // 2 + 2 = 4
        assertThat(targetDto.getCoins()).isEqualTo(2); // 4 - 2 = 2
    }

    @Test
    @DisplayName("Steal - resolveSteal caps at 1 coin if target has only 1 coin")
    void resolveSteal_capsAtAvailableCoins() {
        seed(2, 1, PlayerStatus.ACTIVE);
        gameEngine.performSteal(matchId, actorId, targetId);

        when(turnManager.advanceTurn(matchId)).thenReturn(Match.builder()
                .id(matchId)
                .currentTurnPlayerId(targetId)
                .turnNumber(2)
                .build());

        GameStateResponse response = gameEngine.resolveSteal(matchId, actorId, true);

        GamePlayerDto actorDto = response.getPlayers().stream()
                .filter(p -> p.getUserId().equals(actorId)).findFirst().orElseThrow();
        GamePlayerDto targetDto = response.getPlayers().stream()
                .filter(p -> p.getUserId().equals(targetId)).findFirst().orElseThrow();

        assertThat(actorDto.getCoins()).isEqualTo(3); // 2 + 1 = 3
        assertThat(targetDto.getCoins()).isEqualTo(0); // 1 - 1 = 0
    }

    @Test
    @DisplayName("Steal - cannot steal from oneself")
    void performSteal_cannotStealFromSelf() {
        seed(2, 2, PlayerStatus.ACTIVE);

        assertThatThrownBy(() -> gameEngine.performSteal(matchId, actorId, actorId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("cannot steal from yourself");
    }

    @Test
    @DisplayName("Steal - cannot steal from eliminated player")
    void performSteal_cannotTargetEliminatedPlayer() {
        seed(2, 2, PlayerStatus.ELIMINATED);

        assertThatThrownBy(() -> gameEngine.performSteal(matchId, actorId, targetId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("eliminated player");
    }

    @Test
    @DisplayName("Steal - cannot steal from target with zero coins")
    void performSteal_cannotTargetPlayerWithNoCoins() {
        seed(2, 0, PlayerStatus.ACTIVE);

        assertThatThrownBy(() -> gameEngine.performSteal(matchId, actorId, targetId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("has no coins to steal");
    }

    @Test
    @DisplayName("Steal - mandatory Coup when holding 10+ coins")
    void performSteal_mandatoryCoupWhenTenCoins() {
        seed(10, 2, PlayerStatus.ACTIVE);

        assertThatThrownBy(() -> gameEngine.performSteal(matchId, actorId, targetId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("10 or more coins");
    }
}
