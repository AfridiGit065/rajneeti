package com.rajneeti.service;

import com.rajneeti.dto.match.MatchResponse;
import com.rajneeti.entity.Match;
import com.rajneeti.entity.MatchPlayer;
import com.rajneeti.entity.Room;
import com.rajneeti.entity.RoomPlayer;
import com.rajneeti.entity.User;
import com.rajneeti.entity.enums.MatchStatus;
import com.rajneeti.entity.enums.RoomStatus;
import com.rajneeti.exception.BusinessException;
import com.rajneeti.exception.MatchAlreadyExistsException;
import com.rajneeti.exception.MatchNotFoundException;
import com.rajneeti.exception.NotRoomHostException;
import com.rajneeti.exception.RoomNotFoundException;
import com.rajneeti.game.GameEngine;
import com.rajneeti.mapper.MatchMapper;
import com.rajneeti.repository.MatchPlayerRepository;
import com.rajneeti.repository.MatchRepository;
import com.rajneeti.repository.RoomPlayerRepository;
import com.rajneeti.repository.RoomRepository;
import com.rajneeti.repository.UserRepository;
import com.rajneeti.service.impl.MatchServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MatchServiceTest {

    @Mock
    private MatchRepository matchRepository;

    @Mock
    private MatchPlayerRepository matchPlayerRepository;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private RoomPlayerRepository roomPlayerRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoomService roomService;

    @Mock
    private TurnManager turnManager;

    @Mock
    private GameEngine gameEngine;

    @Spy
    private MatchMapper matchMapper = new MatchMapper();

    @InjectMocks
    private MatchServiceImpl matchService;

    private User hostUser;
    private User playerUser;
    private UUID hostId;
    private UUID playerId;
    private UUID roomId;
    private Room room;

    @BeforeEach
    void setUp() {
        hostId = UUID.randomUUID();
        playerId = UUID.randomUUID();
        roomId = UUID.randomUUID();

        hostUser = User.builder()
                .id(hostId)
                .username("hostPlayer")
                .email("host@rajneeti.com")
                .rating(1000)
                .build();

        playerUser = User.builder()
                .id(playerId)
                .username("guestPlayer")
                .email("guest@rajneeti.com")
                .rating(1000)
                .build();

        room = Room.builder()
                .id(roomId)
                .roomCode("RAJ100")
                .host(hostUser)
                .status(RoomStatus.WAITING)
                .maxPlayers(6)
                .players(new ArrayList<>())
                .build();
    }

    private List<RoomPlayer> buildReadyPlayers() {
        RoomPlayer hostPlayer = RoomPlayer.builder()
                .id(UUID.randomUUID())
                .room(room)
                .user(hostUser)
                .seatNumber(1)
                .ready(true)
                .build();

        RoomPlayer guestPlayer = RoomPlayer.builder()
                .id(UUID.randomUUID())
                .room(room)
                .user(playerUser)
                .seatNumber(2)
                .ready(true)
                .build();

        return new ArrayList<>(List.of(hostPlayer, guestPlayer));
    }

    @Test
    @DisplayName("Start Match - Success")
    void startMatch_Success() {
        List<RoomPlayer> players = buildReadyPlayers();

        when(roomRepository.findById(roomId)).thenReturn(Optional.of(room));
        when(roomPlayerRepository.findByRoomIdOrderBySeatNumberAsc(roomId)).thenReturn(players);
        when(roomService.canStartMatch(roomId)).thenReturn(true);
        when(matchRepository.findByRoomId(roomId)).thenReturn(List.of());
        when(matchRepository.save(any(Match.class))).thenAnswer(inv -> {
            Match m = inv.getArgument(0);
            m.setId(UUID.randomUUID());
            return m;
        });
        when(matchPlayerRepository.save(any(MatchPlayer.class))).thenAnswer(inv -> {
            MatchPlayer mp = inv.getArgument(0);
            mp.setId(UUID.randomUUID());
            return mp;
        });

        MatchResponse response = matchService.startMatch(roomId, hostId);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(MatchStatus.CREATED);
        assertThat(response.getPlayerCount()).isEqualTo(2);
        assertThat(response.getRoomId()).isEqualTo(roomId);
        assertThat(response.getRoomCode()).isEqualTo("RAJ100");
        assertThat(room.getStatus()).isEqualTo(RoomStatus.IN_GAME);
        verify(matchRepository).save(any(Match.class));
        verify(matchPlayerRepository, org.mockito.Mockito.times(2)).save(any(MatchPlayer.class));
    }

    @Test
    @DisplayName("Start Match - Fails if requester is not host")
    void startMatch_NotHost() {
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(room));

        assertThatThrownBy(() -> matchService.startMatch(roomId, playerId))
                .isInstanceOf(NotRoomHostException.class);
    }

    @Test
    @DisplayName("Start Match - Fails if room is not WAITING")
    void startMatch_RoomNotWaiting() {
        room.setStatus(RoomStatus.IN_GAME);

        when(roomRepository.findById(roomId)).thenReturn(Optional.of(room));

        assertThatThrownBy(() -> matchService.startMatch(roomId, hostId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Cannot start match");
    }

    @Test
    @DisplayName("Start Match - Fails if match start conditions not met")
    void startMatch_ConditionsNotMet() {
        List<RoomPlayer> players = buildReadyPlayers();

        when(roomRepository.findById(roomId)).thenReturn(Optional.of(room));
        when(roomPlayerRepository.findByRoomIdOrderBySeatNumberAsc(roomId)).thenReturn(players);
        when(roomService.canStartMatch(roomId)).thenReturn(false);

        assertThatThrownBy(() -> matchService.startMatch(roomId, hostId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("at least 2 players");
    }

    @Test
    @DisplayName("Start Match - Fails if active match already exists")
    void startMatch_DuplicateMatch() {
        List<RoomPlayer> players = buildReadyPlayers();
        Match existingMatch = Match.builder()
                .id(UUID.randomUUID())
                .room(room)
                .status(MatchStatus.CREATED)
                .build();

        when(roomRepository.findById(roomId)).thenReturn(Optional.of(room));
        when(roomPlayerRepository.findByRoomIdOrderBySeatNumberAsc(roomId)).thenReturn(players);
        when(roomService.canStartMatch(roomId)).thenReturn(true);
        when(matchRepository.findByRoomId(roomId)).thenReturn(List.of(existingMatch));

        assertThatThrownBy(() -> matchService.startMatch(roomId, hostId))
                .isInstanceOf(MatchAlreadyExistsException.class);
    }

    @Test
    @DisplayName("Start Match - Fails if room not found")
    void startMatch_RoomNotFound() {
        when(roomRepository.findById(roomId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> matchService.startMatch(roomId, hostId))
                .isInstanceOf(RoomNotFoundException.class);
    }

    @Test
    @DisplayName("Get Match - Success")
    void getMatch_Success() {
        UUID matchId = UUID.randomUUID();
        Match match = Match.builder()
                .id(matchId)
                .room(room)
                .status(MatchStatus.CREATED)
                .build();

        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));
        when(matchPlayerRepository.findByMatchId(matchId)).thenReturn(List.of());

        MatchResponse response = matchService.getMatch(matchId);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(matchId);
        assertThat(response.getStatus()).isEqualTo(MatchStatus.CREATED);
    }

    @Test
    @DisplayName("Get Match - Fails if not found")
    void getMatch_NotFound() {
        UUID matchId = UUID.randomUUID();

        when(matchRepository.findById(matchId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> matchService.getMatch(matchId))
                .isInstanceOf(MatchNotFoundException.class);
    }

    @Test
    @DisplayName("Get Active Match By Room - Success")
    void getActiveMatchByRoom_Success() {
        Match match = Match.builder()
                .id(UUID.randomUUID())
                .room(room)
                .status(MatchStatus.IN_PROGRESS)
                .build();

        when(roomRepository.findById(roomId)).thenReturn(Optional.of(room));
        when(matchRepository.findByRoomId(roomId)).thenReturn(List.of(match));
        when(matchPlayerRepository.findByMatchId(match.getId())).thenReturn(List.of());

        MatchResponse response = matchService.getActiveMatchByRoom(roomId);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(MatchStatus.IN_PROGRESS);
    }

    @Test
    @DisplayName("Get Active Match By Room - Fails if no active match")
    void getActiveMatchByRoom_NoMatch() {
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(room));
        when(matchRepository.findByRoomId(roomId)).thenReturn(List.of());

        assertThatThrownBy(() -> matchService.getActiveMatchByRoom(roomId))
                .isInstanceOf(MatchNotFoundException.class)
                .hasMessageContaining("No active match");
    }

    @Test
    @DisplayName("Start Match - Creates correct MatchPlayer records")
    void startMatch_CorrectMatchPlayers() {
        List<RoomPlayer> players = buildReadyPlayers();

        when(roomRepository.findById(roomId)).thenReturn(Optional.of(room));
        when(roomPlayerRepository.findByRoomIdOrderBySeatNumberAsc(roomId)).thenReturn(players);
        when(roomService.canStartMatch(roomId)).thenReturn(true);
        when(matchRepository.findByRoomId(roomId)).thenReturn(List.of());
        when(matchRepository.save(any(Match.class))).thenAnswer(inv -> {
            Match m = inv.getArgument(0);
            m.setId(UUID.randomUUID());
            return m;
        });
        when(matchPlayerRepository.save(any(MatchPlayer.class))).thenAnswer(inv -> {
            MatchPlayer mp = inv.getArgument(0);
            mp.setId(UUID.randomUUID());
            return mp;
        });

        MatchResponse response = matchService.startMatch(roomId, hostId);

        assertThat(response.getPlayers()).hasSize(2);
        assertThat(response.getPlayers().get(0).getUsername()).isEqualTo("hostPlayer");
        assertThat(response.getPlayers().get(0).getSeatNumber()).isEqualTo(1);
        assertThat(response.getPlayers().get(1).getUsername()).isEqualTo("guestPlayer");
        assertThat(response.getPlayers().get(1).getSeatNumber()).isEqualTo(2);
    }
}
