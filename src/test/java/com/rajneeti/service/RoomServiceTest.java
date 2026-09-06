package com.rajneeti.service;

import com.rajneeti.dto.room.CreateRoomRequest;
import com.rajneeti.dto.room.JoinRoomRequest;
import com.rajneeti.dto.room.RoomResponse;
import com.rajneeti.entity.Room;
import com.rajneeti.entity.RoomPlayer;
import com.rajneeti.entity.User;
import com.rajneeti.entity.enums.RoomStatus;
import com.rajneeti.exception.AlreadyInRoomException;
import com.rajneeti.exception.NotRoomHostException;
import com.rajneeti.exception.RoomFullException;
import com.rajneeti.exception.RoomNotJoinableException;
import com.rajneeti.mapper.RoomMapper;
import com.rajneeti.repository.RoomPlayerRepository;
import com.rajneeti.repository.RoomRepository;
import com.rajneeti.repository.UserRepository;
import com.rajneeti.service.impl.RoomServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

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
class RoomServiceTest {

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private RoomPlayerRepository roomPlayerRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Spy
    private RoomMapper roomMapper = new RoomMapper();

    @InjectMocks
    private RoomServiceImpl roomService;

    private User hostUser;
    private User playerUser;
    private UUID hostId;
    private UUID playerId;

    @BeforeEach
    void setUp() {
        hostId = UUID.randomUUID();
        playerId = UUID.randomUUID();

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
    }

    @Test
    @DisplayName("Create Room - Success")
    void createRoom_Success() {
        CreateRoomRequest request = CreateRoomRequest.builder()
                .maxPlayers(6)
                .build();

        when(userRepository.findById(hostId)).thenReturn(Optional.of(hostUser));
        when(roomRepository.existsByRoomCode(any())).thenReturn(false);
        when(roomRepository.save(any(Room.class))).thenAnswer(inv -> {
            Room r = inv.getArgument(0);
            r.setId(UUID.randomUUID());
            return r;
        });
        when(roomPlayerRepository.save(any(RoomPlayer.class))).thenAnswer(inv -> inv.getArgument(0));

        RoomResponse response = roomService.createRoom(hostId, request);

        assertThat(response).isNotNull();
        assertThat(response.getHostUsername()).isEqualTo("hostPlayer");
        assertThat(response.getStatus()).isEqualTo(RoomStatus.WAITING);
        assertThat(response.getCurrentPlayers()).isEqualTo(1);
        assertThat(response.getPlayers().get(0).getSeatNumber()).isEqualTo(1);
        assertThat(response.getPlayers().get(0).getIsHost()).isTrue();
    }

    @Test
    @DisplayName("Join Room - Success")
    void joinRoom_Success() {
        UUID roomId = UUID.randomUUID();
        Room room = Room.builder()
                .id(roomId)
                .roomCode("RAJ100")
                .host(hostUser)
                .status(RoomStatus.WAITING)
                .maxPlayers(6)
                .build();

        RoomPlayer hostPlayer = RoomPlayer.builder()
                .id(UUID.randomUUID())
                .room(room)
                .user(hostUser)
                .seatNumber(1)
                .ready(false)
                .build();

        List<RoomPlayer> players = new ArrayList<>(List.of(hostPlayer));

        when(userRepository.findById(playerId)).thenReturn(Optional.of(playerUser));
        when(roomRepository.findByRoomCode("RAJ100")).thenReturn(Optional.of(room));
        when(roomPlayerRepository.existsByRoomIdAndUserId(roomId, playerId)).thenReturn(false);
        when(roomPlayerRepository.findByRoomIdOrderBySeatNumberAsc(roomId)).thenReturn(players);
        when(roomPlayerRepository.save(any(RoomPlayer.class))).thenAnswer(inv -> inv.getArgument(0));

        RoomResponse response = roomService.joinRoom(playerId, new JoinRoomRequest("RAJ100"));

        assertThat(response).isNotNull();
        assertThat(response.getCurrentPlayers()).isEqualTo(2);
        assertThat(response.getPlayers().get(1).getUsername()).isEqualTo("guestPlayer");
        assertThat(response.getPlayers().get(1).getSeatNumber()).isEqualTo(2);
    }

    @Test
    @DisplayName("Join Room - Fails if room is full")
    void joinRoom_RoomFull() {
        UUID roomId = UUID.randomUUID();
        Room room = Room.builder()
                .id(roomId)
                .roomCode("RAJ100")
                .host(hostUser)
                .status(RoomStatus.WAITING)
                .maxPlayers(2)
                .build();

        List<RoomPlayer> fullPlayers = List.of(
                RoomPlayer.builder().seatNumber(1).user(hostUser).build(),
                RoomPlayer.builder().seatNumber(2).user(User.builder().id(UUID.randomUUID()).build()).build()
        );

        when(userRepository.findById(playerId)).thenReturn(Optional.of(playerUser));
        when(roomRepository.findByRoomCode("RAJ100")).thenReturn(Optional.of(room));
        when(roomPlayerRepository.existsByRoomIdAndUserId(roomId, playerId)).thenReturn(false);
        when(roomPlayerRepository.findByRoomIdOrderBySeatNumberAsc(roomId)).thenReturn(fullPlayers);

        assertThatThrownBy(() -> roomService.joinRoom(playerId, new JoinRoomRequest("RAJ100")))
                .isInstanceOf(RoomFullException.class)
                .hasMessageContaining("capacity");
    }

    @Test
    @DisplayName("Join Room - Fails if user already in room")
    void joinRoom_AlreadyInRoom() {
        UUID roomId = UUID.randomUUID();
        Room room = Room.builder()
                .id(roomId)
                .roomCode("RAJ100")
                .host(hostUser)
                .status(RoomStatus.WAITING)
                .maxPlayers(6)
                .build();

        when(userRepository.findById(hostId)).thenReturn(Optional.of(hostUser));
        when(roomRepository.findByRoomCode("RAJ100")).thenReturn(Optional.of(room));
        when(roomPlayerRepository.existsByRoomIdAndUserId(roomId, hostId)).thenReturn(true);

        assertThatThrownBy(() -> roomService.joinRoom(hostId, new JoinRoomRequest("RAJ100")))
                .isInstanceOf(AlreadyInRoomException.class);
    }

    @Test
    @DisplayName("Delete Room - Fails if requester is not host")
    void deleteRoom_NotHost() {
        UUID roomId = UUID.randomUUID();
        Room room = Room.builder()
                .id(roomId)
                .host(hostUser)
                .status(RoomStatus.WAITING)
                .build();

        when(roomRepository.findById(roomId)).thenReturn(Optional.of(room));

        assertThatThrownBy(() -> roomService.deleteRoom(roomId, playerId))
                .isInstanceOf(NotRoomHostException.class);
    }

    @Test
    @DisplayName("Set Ready Status - Success")
    void setReadyStatus_Success() {
        UUID roomId = UUID.randomUUID();
        Room room = Room.builder()
                .id(roomId)
                .host(hostUser)
                .status(RoomStatus.WAITING)
                .build();

        RoomPlayer player = RoomPlayer.builder()
                .id(UUID.randomUUID())
                .room(room)
                .user(hostUser)
                .seatNumber(1)
                .ready(false)
                .build();

        when(roomRepository.findById(roomId)).thenReturn(Optional.of(room));
        when(roomPlayerRepository.findByRoomIdAndUserId(roomId, hostId)).thenReturn(Optional.of(player));
        when(roomPlayerRepository.findByRoomIdOrderBySeatNumberAsc(roomId)).thenReturn(List.of(player));

        RoomResponse response = roomService.setReadyStatus(roomId, hostId, true);

        assertThat(response).isNotNull();
        assertThat(player.getReady()).isTrue();
        verify(roomPlayerRepository).save(player);
    }
}