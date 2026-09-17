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
 * Unit tests for Tax action (+3 coins) and mandatory Coup rule.
 */
@ExtendWith(MockitoExtension.class)
class GameEngineTaxTest {

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
    private UUID opponentId;

    @BeforeEach
    void setUp() {
        matchId = UUID.randomUUID();
        actorId = UUID.randomUUID();
        opponentId = UUID.randomUUID();
        gameEngine = new GameEngine(
                matchRepository, matchPlayerRepository, gameStore,
                cardManager, turnManager, gameStateMapper,
                new WinnerManager(matchRepository, matchPlayerRepository, webSocketEventPublisher),
                webSocketEventPublisher,
                new GameStateSyncService(gameStateMapper, webSocketEventPublisher));
        gameStore.remove(matchId);
    }

    private GameState seed(int actorCoins, int opponentCoins) {
        GamePlayerState actor = GamePlayerState.builder()
                .userId(actorId)
                .username("actor")
                .seatNumber(1)
                .status(PlayerStatus.ACTIVE)
                .coins(actorCoins)
                .cards(List.of(GameCard.builder().id(UUID.randomUUID()).character(CharacterType.MINISTER).build()))
                .build();

        GamePlayerState opponent = GamePlayerState.builder()
                .userId(opponentId)
                .username("opponent")
                .seatNumber(2)
                .status(PlayerStatus.ACTIVE)
                .coins(opponentCoins)
                .cards(List.of(GameCard.builder().id(UUID.randomUUID()).character(CharacterType.GHATOK).build()))
                .build();

        GameState state = GameState.builder()
                .matchId(matchId)
                .roomId(UUID.randomUUID())
                .roomCode("RJNTAX")
                .status(MatchStatus.IN_PROGRESS)
                .phase(GameEngine.PHASE_IN_PROGRESS)
                .players(new ArrayList<>(List.of(actor, opponent)))
                .turnOrder(List.of(actorId, opponentId))
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
    @DisplayName("Tax - declaring Tax creates pending action claiming Minister")
    void performTax_createsPendingActionClaimingMinister() {
        seed(2, 2);

        GameStateResponse response = gameEngine.performTax(matchId, actorId);

        assertThat(response.getPendingAction()).isNotNull();
        assertThat(response.getPendingAction().getType()).isEqualTo("TAX");
        assertThat(response.getPendingAction().getClaimedCharacter()).isEqualTo("minister");
        assertThat(response.getPendingAction().getActorUserId()).isEqualTo(actorId);
    }

    @Test
    @DisplayName("Tax - resolveTax adds +3 coins and advances turn")
    void resolveTax_addsThreeCoinsAndAdvancesTurn() {
        seed(2, 2);
        gameEngine.performTax(matchId, actorId);

        when(turnManager.advanceTurn(matchId)).thenReturn(Match.builder()
                .id(matchId)
                .currentTurnPlayerId(opponentId)
                .turnNumber(2)
                .build());

        GameStateResponse response = gameEngine.resolveTax(matchId, actorId, true);

        GamePlayerDto actorDto = response.getPlayers().stream()
                .filter(p -> p.getUserId().equals(actorId))
                .findFirst().orElseThrow();

        assertThat(actorDto.getCoins()).isEqualTo(5); // 2 + 3 = 5
        assertThat(response.getPendingAction()).isNull();
        assertThat(response.getCurrentTurnPlayerId()).isEqualTo(opponentId);
        assertThat(response.getTurnNumber()).isEqualTo(2);
    }

    @Test
    @DisplayName("Tax - cannot perform when holding 10+ coins (Mandatory Coup)")
    void performTax_mandatoryCoupWhenTenCoins() {
        seed(10, 2);

        assertThatThrownBy(() -> gameEngine.performTax(matchId, actorId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("10 or more coins");
    }

    @Test
    @DisplayName("Tax - non-turn player cannot perform Tax")
    void performTax_notYourTurn() {
        seed(2, 2);

        assertThatThrownBy(() -> gameEngine.performTax(matchId, opponentId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("not your turn");
    }
}
