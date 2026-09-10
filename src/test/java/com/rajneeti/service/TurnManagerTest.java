package com.rajneeti.service;

import com.rajneeti.entity.Match;
import com.rajneeti.entity.MatchPlayer;
import com.rajneeti.entity.User;
import com.rajneeti.entity.enums.MatchStatus;
import com.rajneeti.entity.enums.PlayerStatus;
import com.rajneeti.exception.BusinessException;
import com.rajneeti.exception.MatchNotFoundException;
import com.rajneeti.repository.MatchPlayerRepository;
import com.rajneeti.repository.MatchRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TurnManagerTest {

    @Mock
    private MatchRepository matchRepository;

    @Mock
    private MatchPlayerRepository matchPlayerRepository;

    @InjectMocks
    private TurnManager turnManager;

    private Match match;
    private User userA;
    private User userB;
    private User userC;
    private User userD;

    @BeforeEach
    void setUp() {
        match = Match.builder()
                .id(UUID.randomUUID())
                .status(MatchStatus.IN_PROGRESS)
                .players(new ArrayList<>())
                .build();

        userA = User.builder()
                .id(UUID.randomUUID())
                .username("playerA")
                .email("a@rajneeti.com")
                .build();

        userB = User.builder()
                .id(UUID.randomUUID())
                .username("playerB")
                .email("b@rajneeti.com")
                .build();

        userC = User.builder()
                .id(UUID.randomUUID())
                .username("playerC")
                .email("c@rajneeti.com")
                .build();

        userD = User.builder()
                .id(UUID.randomUUID())
                .username("playerD")
                .email("d@rajneeti.com")
                .build();
    }

    private MatchPlayer buildPlayer(User user, int seat, PlayerStatus status) {
        return MatchPlayer.builder()
                .id(UUID.randomUUID())
                .match(match)
                .user(user)
                .seatNumber(seat)
                .playerStatus(status)
                .eliminated(status == PlayerStatus.ELIMINATED)
                .coinsAtEnd(0)
                .build();
    }

    // ===========================
    // Test 1: First turn assigned correctly
    // ===========================
    @Test
    @DisplayName("assignFirstTurn - assigns turn to player with lowest seat number")
    void assignFirstTurn_LowestSeat() {
        MatchPlayer playerC = buildPlayer(userC, 3, PlayerStatus.ACTIVE);
        MatchPlayer playerA = buildPlayer(userA, 1, PlayerStatus.ACTIVE);
        MatchPlayer playerB = buildPlayer(userB, 2, PlayerStatus.ACTIVE);

        List<MatchPlayer> players = List.of(playerC, playerA, playerB);
        match.setPlayers(players);

        turnManager.assignFirstTurn(match, players);

        assertThat(match.getCurrentTurnPlayerId()).isEqualTo(userA.getId());
        assertThat(match.getTurnNumber()).isEqualTo(1);
        verify(matchRepository).save(match);
    }

    @Test
    @DisplayName("assignFirstTurn - assigns turn to only active player when only one is active")
    void assignFirstTurn_SingleActivePlayer() {
        MatchPlayer playerA = buildPlayer(userA, 1, PlayerStatus.ACTIVE);
        MatchPlayer playerB = buildPlayer(userB, 2, PlayerStatus.ELIMINATED);
        MatchPlayer playerC = buildPlayer(userC, 3, PlayerStatus.ELIMINATED);

        List<MatchPlayer> players = List.of(playerA, playerB, playerC);
        match.setPlayers(players);

        turnManager.assignFirstTurn(match, players);

        assertThat(match.getCurrentTurnPlayerId()).isEqualTo(userA.getId());
        assertThat(match.getTurnNumber()).isEqualTo(1);
    }

    @Test
    @DisplayName("assignFirstTurn - throws when no players provided")
    void assignFirstTurn_EmptyPlayers() {
        assertThatThrownBy(() -> turnManager.assignFirstTurn(match, List.of()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("no players");
    }

    @Test
    @DisplayName("assignFirstTurn - throws when null players provided")
    void assignFirstTurn_NullPlayers() {
        assertThatThrownBy(() -> turnManager.assignFirstTurn(match, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("no players");
    }

    @Test
    @DisplayName("assignFirstTurn - throws when no active players")
    void assignFirstTurn_NoActivePlayers() {
        MatchPlayer playerA = buildPlayer(userA, 1, PlayerStatus.ELIMINATED);
        MatchPlayer playerB = buildPlayer(userB, 2, PlayerStatus.ELIMINATED);

        assertThatThrownBy(() -> turnManager.assignFirstTurn(match, List.of(playerA, playerB)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("no active players");
    }

    // ===========================
    // Test 2: Current player identified correctly
    // ===========================
    @Test
    @DisplayName("getCurrentTurnPlayerId - returns correct player")
    void getCurrentTurnPlayerId_ReturnsCorrectPlayer() {
        match.setCurrentTurnPlayerId(userB.getId());

        when(matchRepository.findById(match.getId())).thenReturn(Optional.of(match));

        UUID currentTurn = turnManager.getCurrentTurnPlayerId(match.getId());
        assertThat(currentTurn).isEqualTo(userB.getId());
    }

    @Test
    @DisplayName("getCurrentTurnPlayerId - returns null when no turn set")
    void getCurrentTurnPlayerId_Null() {
        when(matchRepository.findById(match.getId())).thenReturn(Optional.of(match));

        UUID currentTurn = turnManager.getCurrentTurnPlayerId(match.getId());
        assertThat(currentTurn).isNull();
    }

    @Test
    @DisplayName("getCurrentTurnPlayerId - throws when match not found")
    void getCurrentTurnPlayerId_MatchNotFound() {
        UUID fakeId = UUID.randomUUID();
        when(matchRepository.findById(fakeId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> turnManager.getCurrentTurnPlayerId(fakeId))
                .isInstanceOf(MatchNotFoundException.class);
    }

    // ===========================
    // Test 3: Current player validation works
    // ===========================
    @Test
    @DisplayName("isPlayerTurn - returns true for current active player")
    void isPlayerTurn_ValidPlayer() {
        MatchPlayer playerA = buildPlayer(userA, 1, PlayerStatus.ACTIVE);
        match.setCurrentTurnPlayerId(userA.getId());

        when(matchRepository.findById(match.getId())).thenReturn(Optional.of(match));
        when(matchPlayerRepository.findByMatchIdAndUserId(match.getId(), userA.getId()))
                .thenReturn(Optional.of(playerA));

        assertThat(turnManager.isPlayerTurn(match.getId(), userA.getId())).isTrue();
    }

    @Test
    @DisplayName("isPlayerTurn - returns false for non-current player")
    void isPlayerTurn_NonCurrentPlayer() {
        match.setCurrentTurnPlayerId(userA.getId());

        when(matchRepository.findById(match.getId())).thenReturn(Optional.of(match));

        assertThat(turnManager.isPlayerTurn(match.getId(), userB.getId())).isFalse();
    }

    @Test
    @DisplayName("isPlayerTurn - returns false when match not in progress")
    void isPlayerTurn_MatchNotInProgress() {
        match.setStatus(MatchStatus.FINISHED);
        match.setCurrentTurnPlayerId(userA.getId());

        when(matchRepository.findById(match.getId())).thenReturn(Optional.of(match));

        assertThat(turnManager.isPlayerTurn(match.getId(), userA.getId())).isFalse();
    }

    @Test
    @DisplayName("isPlayerTurn - returns false when no current turn set")
    void isPlayerTurn_NoCurrentTurn() {
        when(matchRepository.findById(match.getId())).thenReturn(Optional.of(match));

        assertThat(turnManager.isPlayerTurn(match.getId(), userA.getId())).isFalse();
    }

    @Test
    @DisplayName("isPlayerTurn - returns false for eliminated player")
    void isPlayerTurn_EliminatedPlayer() {
        MatchPlayer playerA = buildPlayer(userA, 1, PlayerStatus.ELIMINATED);
        match.setCurrentTurnPlayerId(userA.getId());

        when(matchRepository.findById(match.getId())).thenReturn(Optional.of(match));
        when(matchPlayerRepository.findByMatchIdAndUserId(match.getId(), userA.getId()))
                .thenReturn(Optional.of(playerA));

        assertThat(turnManager.isPlayerTurn(match.getId(), userA.getId())).isFalse();
    }

    @Test
    @DisplayName("isPlayerTurn - returns false when player not found in match")
    void isPlayerTurn_PlayerNotFound() {
        match.setCurrentTurnPlayerId(userA.getId());

        when(matchRepository.findById(match.getId())).thenReturn(Optional.of(match));
        when(matchPlayerRepository.findByMatchIdAndUserId(match.getId(), userA.getId()))
                .thenReturn(Optional.empty());

        assertThat(turnManager.isPlayerTurn(match.getId(), userA.getId())).isFalse();
    }

    // ===========================
    // Test 4: Next player calculated correctly
    // ===========================
    @Test
    @DisplayName("advanceTurn - advances to next player in seat order")
    void advanceTurn_Normal() {
        MatchPlayer playerA = buildPlayer(userA, 1, PlayerStatus.ACTIVE);
        MatchPlayer playerB = buildPlayer(userB, 2, PlayerStatus.ACTIVE);
        MatchPlayer playerC = buildPlayer(userC, 3, PlayerStatus.ACTIVE);

        match.setCurrentTurnPlayerId(userA.getId());
        match.setTurnNumber(1);

        when(matchRepository.findById(match.getId())).thenReturn(Optional.of(match));
        when(matchPlayerRepository.findByMatchId(match.getId()))
                .thenReturn(List.of(playerA, playerB, playerC));

        Match result = turnManager.advanceTurn(match.getId());

        assertThat(result.getCurrentTurnPlayerId()).isEqualTo(userB.getId());
        assertThat(result.getTurnNumber()).isEqualTo(2);
    }

    @Test
    @DisplayName("advanceTurn - wraps around from last to first player")
    void advanceTurn_WrapAround() {
        MatchPlayer playerA = buildPlayer(userA, 1, PlayerStatus.ACTIVE);
        MatchPlayer playerB = buildPlayer(userB, 2, PlayerStatus.ACTIVE);
        MatchPlayer playerC = buildPlayer(userC, 3, PlayerStatus.ACTIVE);

        match.setCurrentTurnPlayerId(userC.getId());
        match.setTurnNumber(5);

        when(matchRepository.findById(match.getId())).thenReturn(Optional.of(match));
        when(matchPlayerRepository.findByMatchId(match.getId()))
                .thenReturn(List.of(playerA, playerB, playerC));

        Match result = turnManager.advanceTurn(match.getId());

        assertThat(result.getCurrentTurnPlayerId()).isEqualTo(userA.getId());
        assertThat(result.getTurnNumber()).isEqualTo(6);
    }

    @Test
    @DisplayName("advanceTurn - throws when match not active")
    void advanceTurn_MatchNotInProgress() {
        match.setStatus(MatchStatus.FINISHED);

        when(matchRepository.findById(match.getId())).thenReturn(Optional.of(match));

        assertThatThrownBy(() -> turnManager.advanceTurn(match.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("not active");
    }

    // ===========================
    // Test 5: Eliminated players skipped
    // ===========================
    @Test
    @DisplayName("advanceTurn - skips eliminated player")
    void advanceTurn_SkipsEliminated() {
        MatchPlayer playerA = buildPlayer(userA, 1, PlayerStatus.ACTIVE);
        MatchPlayer playerB = buildPlayer(userB, 2, PlayerStatus.ELIMINATED);
        MatchPlayer playerC = buildPlayer(userC, 3, PlayerStatus.ACTIVE);

        match.setCurrentTurnPlayerId(userA.getId());
        match.setTurnNumber(1);

        when(matchRepository.findById(match.getId())).thenReturn(Optional.of(match));
        when(matchPlayerRepository.findByMatchId(match.getId()))
                .thenReturn(List.of(playerA, playerB, playerC));

        Match result = turnManager.advanceTurn(match.getId());

        assertThat(result.getCurrentTurnPlayerId()).isEqualTo(userC.getId());
        assertThat(result.getTurnNumber()).isEqualTo(2);
    }

    // ===========================
    // Test 6: Multiple eliminated players skipped
    // ===========================
    @Test
    @DisplayName("advanceTurn - skips multiple eliminated players")
    void advanceTurn_SkipsMultipleEliminated() {
        MatchPlayer playerA = buildPlayer(userA, 1, PlayerStatus.ACTIVE);
        MatchPlayer playerB = buildPlayer(userB, 2, PlayerStatus.ELIMINATED);
        MatchPlayer playerC = buildPlayer(userC, 3, PlayerStatus.ELIMINATED);
        MatchPlayer playerD = buildPlayer(userD, 4, PlayerStatus.ACTIVE);

        match.setCurrentTurnPlayerId(userA.getId());
        match.setTurnNumber(1);

        when(matchRepository.findById(match.getId())).thenReturn(Optional.of(match));
        when(matchPlayerRepository.findByMatchId(match.getId()))
                .thenReturn(List.of(playerA, playerB, playerC, playerD));

        Match result = turnManager.advanceTurn(match.getId());

        assertThat(result.getCurrentTurnPlayerId()).isEqualTo(userD.getId());
        assertThat(result.getTurnNumber()).isEqualTo(2);
    }

    @Test
    @DisplayName("advanceTurn - wraps around skipping eliminated")
    void advanceTurn_WrapAroundSkippingEliminated() {
        MatchPlayer playerA = buildPlayer(userA, 1, PlayerStatus.ACTIVE);
        MatchPlayer playerB = buildPlayer(userB, 2, PlayerStatus.ELIMINATED);
        MatchPlayer playerC = buildPlayer(userC, 3, PlayerStatus.ACTIVE);
        MatchPlayer playerD = buildPlayer(userD, 4, PlayerStatus.ELIMINATED);

        match.setCurrentTurnPlayerId(userC.getId());
        match.setTurnNumber(3);

        when(matchRepository.findById(match.getId())).thenReturn(Optional.of(match));
        when(matchPlayerRepository.findByMatchId(match.getId()))
                .thenReturn(List.of(playerA, playerB, playerC, playerD));

        Match result = turnManager.advanceTurn(match.getId());

        assertThat(result.getCurrentTurnPlayerId()).isEqualTo(userA.getId());
        assertThat(result.getTurnNumber()).isEqualTo(4);
    }

    // ===========================
    // Test 7: Non-current player rejected (isPlayerTurn returns false)
    // ===========================
    @Test
    @DisplayName("isPlayerTurn - rejects player who is not current turn holder")
    void isPlayerTurn_RejectsNonCurrentPlayer() {
        match.setCurrentTurnPlayerId(userA.getId());

        when(matchRepository.findById(match.getId())).thenReturn(Optional.of(match));

        boolean canAct = turnManager.isPlayerTurn(match.getId(), userB.getId());
        assertThat(canAct).isFalse();
    }

    // ===========================
    // Test 8: One remaining active player handled safely
    // ===========================
    @Test
    @DisplayName("advanceTurn - does not advance when only one active player remains")
    void advanceTurn_SinglePlayerRemaining() {
        MatchPlayer playerA = buildPlayer(userA, 1, PlayerStatus.ACTIVE);
        MatchPlayer playerB = buildPlayer(userB, 2, PlayerStatus.ELIMINATED);
        MatchPlayer playerC = buildPlayer(userC, 3, PlayerStatus.ELIMINATED);

        match.setCurrentTurnPlayerId(userA.getId());
        match.setTurnNumber(5);

        when(matchRepository.findById(match.getId())).thenReturn(Optional.of(match));
        when(matchPlayerRepository.findByMatchId(match.getId()))
                .thenReturn(List.of(playerA, playerB, playerC));

        Match result = turnManager.advanceTurn(match.getId());

        assertThat(result.getCurrentTurnPlayerId()).isEqualTo(userA.getId());
        assertThat(result.getTurnNumber()).isEqualTo(5);
    }

    // ===========================
    // Test 9: Deterministic turn order
    // ===========================
    @Test
    @DisplayName("getPlayersInTurnOrder - returns players sorted by seat number")
    void getPlayersInTurnOrder_Deterministic() {
        MatchPlayer playerC = buildPlayer(userC, 3, PlayerStatus.ACTIVE);
        MatchPlayer playerA = buildPlayer(userA, 1, PlayerStatus.ACTIVE);
        MatchPlayer playerB = buildPlayer(userB, 2, PlayerStatus.ACTIVE);

        when(matchPlayerRepository.findByMatchId(match.getId()))
                .thenReturn(List.of(playerC, playerA, playerB));

        List<MatchPlayer> ordered = turnManager.getPlayersInTurnOrder(match.getId());

        assertThat(ordered).hasSize(3);
        assertThat(ordered.get(0).getUser().getId()).isEqualTo(userA.getId());
        assertThat(ordered.get(0).getSeatNumber()).isEqualTo(1);
        assertThat(ordered.get(1).getUser().getId()).isEqualTo(userB.getId());
        assertThat(ordered.get(1).getSeatNumber()).isEqualTo(2);
        assertThat(ordered.get(2).getUser().getId()).isEqualTo(userC.getId());
        assertThat(ordered.get(2).getSeatNumber()).isEqualTo(3);
    }

    @Test
    @DisplayName("getTurnOrder - returns user IDs in deterministic seat order")
    void getTurnOrder_Deterministic() {
        MatchPlayer playerD = buildPlayer(userD, 4, PlayerStatus.ACTIVE);
        MatchPlayer playerA = buildPlayer(userA, 1, PlayerStatus.ACTIVE);

        when(matchPlayerRepository.findByMatchId(match.getId()))
                .thenReturn(List.of(playerD, playerA));

        List<UUID> turnOrder = turnManager.getTurnOrder(match.getId());

        assertThat(turnOrder).containsExactly(userA.getId(), userD.getId());
    }

    // ===========================
    // Additional tests: countActivePlayers, isSinglePlayerRemaining
    // ===========================
    @Test
    @DisplayName("countActivePlayers - counts correctly")
    void countActivePlayers() {
        MatchPlayer playerA = buildPlayer(userA, 1, PlayerStatus.ACTIVE);
        MatchPlayer playerB = buildPlayer(userB, 2, PlayerStatus.ELIMINATED);
        MatchPlayer playerC = buildPlayer(userC, 3, PlayerStatus.ACTIVE);

        when(matchPlayerRepository.findByMatchId(match.getId()))
                .thenReturn(List.of(playerA, playerB, playerC));

        long count = turnManager.countActivePlayers(match.getId());
        assertThat(count).isEqualTo(2);
    }

    @Test
    @DisplayName("isSinglePlayerRemaining - returns true when one active")
    void isSinglePlayerRemaining_True() {
        MatchPlayer playerA = buildPlayer(userA, 1, PlayerStatus.ACTIVE);
        MatchPlayer playerB = buildPlayer(userB, 2, PlayerStatus.ELIMINATED);

        when(matchPlayerRepository.findByMatchId(match.getId()))
                .thenReturn(List.of(playerA, playerB));

        assertThat(turnManager.isSinglePlayerRemaining(match.getId())).isTrue();
    }

    @Test
    @DisplayName("isSinglePlayerRemaining - returns false when multiple active")
    void isSinglePlayerRemaining_False() {
        MatchPlayer playerA = buildPlayer(userA, 1, PlayerStatus.ACTIVE);
        MatchPlayer playerB = buildPlayer(userB, 2, PlayerStatus.ACTIVE);

        when(matchPlayerRepository.findByMatchId(match.getId()))
                .thenReturn(List.of(playerA, playerB));

        assertThat(turnManager.isSinglePlayerRemaining(match.getId())).isFalse();
    }

    @Test
    @DisplayName("advanceTurn - throws when match not found")
    void advanceTurn_MatchNotFound() {
        UUID fakeId = UUID.randomUUID();
        when(matchRepository.findById(fakeId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> turnManager.advanceTurn(fakeId))
                .isInstanceOf(MatchNotFoundException.class);
    }

    @Test
    @DisplayName("advanceTurn - falls back to first active player when current player not found in active list")
    void advanceTurn_CurrentPlayerNotInActiveList() {
        MatchPlayer playerA = buildPlayer(userA, 1, PlayerStatus.ELIMINATED);
        MatchPlayer playerB = buildPlayer(userB, 2, PlayerStatus.ACTIVE);
        MatchPlayer playerC = buildPlayer(userC, 3, PlayerStatus.ACTIVE);

        match.setCurrentTurnPlayerId(userA.getId());
        match.setTurnNumber(1);

        when(matchRepository.findById(match.getId())).thenReturn(Optional.of(match));
        when(matchPlayerRepository.findByMatchId(match.getId()))
                .thenReturn(List.of(playerA, playerB, playerC));

        Match result = turnManager.advanceTurn(match.getId());

        assertThat(result.getCurrentTurnPlayerId()).isEqualTo(userB.getId());
        assertThat(result.getTurnNumber()).isEqualTo(2);
    }
}
