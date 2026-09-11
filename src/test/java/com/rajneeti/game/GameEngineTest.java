package com.rajneeti.game;

import com.rajneeti.entity.Match;
import com.rajneeti.entity.MatchPlayer;
import com.rajneeti.entity.Room;
import com.rajneeti.entity.User;
import com.rajneeti.entity.enums.MatchStatus;
import com.rajneeti.entity.enums.PlayerStatus;
import com.rajneeti.exception.BusinessException;
import com.rajneeti.exception.MatchNotFoundException;
import com.rajneeti.repository.MatchPlayerRepository;
import com.rajneeti.repository.MatchRepository;
import com.rajneeti.service.TurnManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GameEngineTest {

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
    private User hostUser;
    private User guestUser;
    private Room room;

    @BeforeEach
    void setUp() {
        matchId = UUID.randomUUID();
        hostUser = User.builder()
                .id(UUID.randomUUID())
                .username("hostPlayer")
                .email("host@rajneeti.com")
                .rating(1000)
                .build();
        guestUser = User.builder()
                .id(UUID.randomUUID())
                .username("guestPlayer")
                .email("guest@rajneeti.com")
                .rating(900)
                .build();
        room = Room.builder()
                .id(UUID.randomUUID())
                .roomCode("RJNG01")
                .host(hostUser)
                .maxPlayers(6)
                .build();

        gameEngine = new GameEngine(
                matchRepository, matchPlayerRepository, gameStore, cardManager, turnManager, gameStateMapper);
    }

    private Match buildMatch() {
        return Match.builder()
                .id(matchId)
                .room(room)
                .status(MatchStatus.CREATED)
                .currentTurnPlayerId(hostUser.getId())
                .turnNumber(1)
                .build();
    }

    private List<MatchPlayer> buildPlayers() {
        MatchPlayer host = MatchPlayer.builder()
                .id(UUID.randomUUID())
                .user(hostUser)
                .seatNumber(1)
                .playerStatus(PlayerStatus.ACTIVE)
                .build();
        MatchPlayer guest = MatchPlayer.builder()
                .id(UUID.randomUUID())
                .user(guestUser)
                .seatNumber(2)
                .playerStatus(PlayerStatus.ACTIVE)
                .build();
        return List.of(host, guest);
    }

    @Test
    @DisplayName("Initialize - every player gets 2 coins, 2 cards, ACTIVE status")
    void initializeMatch_grantsStartingResources() {
        Match match = buildMatch();
        List<MatchPlayer> players = buildPlayers();

        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));
        when(matchPlayerRepository.findByMatchId(matchId)).thenReturn(players);

        GameState state = gameEngine.initializeMatch(matchId);

        assertThat(state.getPlayers()).hasSize(2);
        for (GamePlayerState player : state.getPlayers()) {
            assertThat(player.getCoins()).isEqualTo(GameEngine.STARTING_COINS);
            assertThat(player.getCards()).hasSize(GameEngine.STARTING_INFLUENCE);
            assertThat(player.getStatus()).isEqualTo(PlayerStatus.ACTIVE);
        }
    }

    @Test
    @DisplayName("Initialize - draws from a 15-card deck, leaving 15 - 2*players cards")
    void initializeMatch_dealsFromFullDeck() {
        Match match = buildMatch();
        List<MatchPlayer> players = buildPlayers();

        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));
        when(matchPlayerRepository.findByMatchId(matchId)).thenReturn(players);

        GameState state = gameEngine.initializeMatch(matchId);

        int playerCards = state.getPlayers().stream()
                .mapToInt(p -> p.getCards().size())
                .sum();
        assertThat(state.getDeck()).hasSize(CardManager.DECK_SIZE - playerCards);
        assertThat(playerCards).isEqualTo(players.size() * GameEngine.STARTING_INFLUENCE);
    }

    @Test
    @DisplayName("Initialize - dealt card ids are unique across the whole game")
    void initializeMatch_cardIdsAreUniqueAcrossGame() {
        Match match = buildMatch();
        List<MatchPlayer> players = buildPlayers();

        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));
        when(matchPlayerRepository.findByMatchId(matchId)).thenReturn(players);

        GameState state = gameEngine.initializeMatch(matchId);

        Set<UUID> allIds = new HashSet<>();
        for (GamePlayerState player : state.getPlayers()) {
            player.getCards().forEach(card -> assertThat(allIds.add(card.getId())).isTrue());
        }
        state.getDeck().forEach(card -> assertThat(allIds.add(card.getId())).isTrue());
        assertThat(allIds).hasSize(CardManager.DECK_SIZE);
    }

    @Test
    @DisplayName("Initialize - persists turn reference from the match")
    void initializeMatch_keepsTurnReference() {
        Match match = buildMatch();
        List<MatchPlayer> players = buildPlayers();

        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));
        when(matchPlayerRepository.findByMatchId(matchId)).thenReturn(players);

        GameState state = gameEngine.initializeMatch(matchId);

        assertThat(state.getCurrentTurnPlayerId()).isEqualTo(hostUser.getId());
        assertThat(state.getTurnNumber()).isEqualTo(1);
        assertThat(state.getTurnOrder()).containsExactly(hostUser.getId(), guestUser.getId());
    }

    @Test
    @DisplayName("Initialize - invalid (nonexistent) match is rejected")
    void initializeMatch_invalidMatchThrows() {
        when(matchRepository.findById(matchId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> gameEngine.initializeMatch(matchId))
                .isInstanceOf(MatchNotFoundException.class);
    }

    @Test
    @DisplayName("Initialize - duplicate initialization is prevented")
    void initializeMatch_duplicateInitThrows() {
        Match match = buildMatch();
        List<MatchPlayer> players = buildPlayers();

        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));
        when(matchPlayerRepository.findByMatchId(matchId)).thenReturn(players);

        gameEngine.initializeMatch(matchId);

        assertThatThrownBy(() -> gameEngine.initializeMatch(matchId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("already initialized");
    }

    @Test
    @DisplayName("getGameState - returns stored state once initialized")
    void getGameState_returnsStoredState() {
        Match match = buildMatch();
        List<MatchPlayer> players = buildPlayers();

        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));
        when(matchPlayerRepository.findByMatchId(matchId)).thenReturn(players);

        gameEngine.initializeMatch(matchId);

        GameState state = gameEngine.getGameState(matchId);
        assertThat(state.getMatchId()).isEqualTo(matchId);
    }

    @Test
    @DisplayName("getGameState - rejects a match with no live state")
    void getGameState_notInitializedThrows() {
        assertThatThrownBy(() -> gameEngine.getGameState(matchId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("not been initialized");
    }

    @Test
    @DisplayName("getOrInitialize - initializes once and reuses on subsequent calls")
    void getOrInitialize_isIdempotent() {
        Match match = buildMatch();
        List<MatchPlayer> players = buildPlayers();

        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));
        when(matchPlayerRepository.findByMatchId(matchId)).thenReturn(players);

        GameState first = gameEngine.getOrInitialize(matchId);
        GameState second = gameEngine.getOrInitialize(matchId);

        assertThat(first).isSameAs(second);
        assertThat(gameStore.size()).isEqualTo(1);
    }
}