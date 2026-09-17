package com.rajneeti.service.impl;

import com.rajneeti.dto.room.CreateRoomRequest;
import com.rajneeti.dto.room.JoinRoomRequest;
import com.rajneeti.dto.room.RoomResponse;
import com.rajneeti.dto.websocket.RoomPayload;
import com.rajneeti.dto.websocket.WebSocketEventType;
import com.rajneeti.entity.Room;
import com.rajneeti.entity.RoomPlayer;
import com.rajneeti.entity.User;
import com.rajneeti.entity.enums.RoomStatus;
import com.rajneeti.exception.AlreadyInRoomException;
import com.rajneeti.exception.InvalidRoomStateException;
import com.rajneeti.exception.NotInRoomException;
import com.rajneeti.exception.NotRoomHostException;
import com.rajneeti.exception.ResourceNotFoundException;
import com.rajneeti.exception.RoomFullException;
import com.rajneeti.exception.RoomNotFoundException;
import com.rajneeti.exception.RoomNotJoinableException;
import com.rajneeti.mapper.RoomMapper;
import com.rajneeti.bot.service.BotUserService;
import com.rajneeti.repository.RoomPlayerRepository;
import com.rajneeti.repository.RoomRepository;
import com.rajneeti.repository.UserRepository;
import com.rajneeti.service.RoomService;
import com.rajneeti.websocket.WebSocketEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementation of {@link RoomService}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoomServiceImpl implements RoomService {

    private final RoomRepository            roomRepository;
    private final RoomPlayerRepository      roomPlayerRepository;
    private final UserRepository            userRepository;
    private final RoomMapper                roomMapper;
    private final WebSocketEventPublisher   webSocketEventPublisher;
    private final BotUserService            botUserService;

    private static final SecureRandom RANDOM = new SecureRandom();

    @Override
    @Transactional
    public RoomResponse createRoom(UUID userId, CreateRoomRequest request) {
        User host = findUserById(userId);

        int maxPlayers = (request != null && request.getMaxPlayers() != null)
                ? Math.max(2, Math.min(6, request.getMaxPlayers()))
                : 6;

        String roomCode = generateUniqueRoomCode();

        String roomName = (request != null && request.getName() != null)
                ? request.getName().trim()
                : null;

        Room room = Room.builder()
                .roomCode(roomCode)
                .name((roomName != null && !roomName.isEmpty()) ? roomName : null)
                .host(host)
                .status(RoomStatus.WAITING)
                .maxPlayers(maxPlayers)
                .players(new ArrayList<>())
                .build();

        Room savedRoom = roomRepository.save(room);

        // Host automatically joins seat 1
        RoomPlayer hostPlayer = RoomPlayer.builder()
                .room(savedRoom)
                .user(host)
                .seatNumber(1)
                .ready(false)
                .build();

        RoomPlayer savedPlayer = roomPlayerRepository.save(hostPlayer);
        List<RoomPlayer> players = List.of(savedPlayer);

        // Module 25 — seed AI bots (auto-ready) right after the host joins. The
        // host always keeps seat 1 and stays human; bots take the next seats.
        int requestedBots = (request != null && request.getBotCount() != null)
                ? Math.max(0, Math.min(maxPlayers - 1, request.getBotCount()))
                : 0;
        if (requestedBots > 0) {
            String difficulty = request.getBotDifficulty() != null
                    ? request.getBotDifficulty().toUpperCase() : "MEDIUM";
            String personality = request.getBotPersonality() != null
                    ? request.getBotPersonality().toUpperCase() : "BALANCED";
            botUserService.seedBots(savedRoom, requestedBots, difficulty, personality);
            players = roomPlayerRepository.findByRoomIdOrderBySeatNumberAsc(savedRoom.getId());
        }

        log.info("Room '{}' ({}) created by host '{}' with {} bot(s)",
                roomCode, savedRoom.getId(), host.getUsername(), requestedBots);

        RoomResponse response = roomMapper.toRoomResponse(savedRoom, players);

        webSocketEventPublisher.publishToRoom(savedRoom.getId(), WebSocketEventType.JOIN_ROOM, host.getId(),
                RoomPayload.builder()
                        .playerId(host.getId())
                        .username(host.getUsername())
                        .seatNumber(savedPlayer.getSeatNumber())
                        .ready(false)
                        .host(true)
                        .playerCount(players.size())
                        .build());

        return response;
    }

    @Override
    @Transactional
    public RoomResponse joinRoom(UUID userId, JoinRoomRequest request) {
        User user = findUserById(userId);
        String roomCode = request.getRoomCode().trim().toUpperCase();

        Room room = roomRepository.findByRoomCode(roomCode)
                .orElseThrow(() -> new RoomNotFoundException("Room with code '" + roomCode + "' not found."));

        if (room.getStatus() != RoomStatus.WAITING) {
            throw new RoomNotJoinableException(
                    "Cannot join room '" + roomCode + "'. Room status is " + room.getStatus() + ".");
        }

        if (roomPlayerRepository.existsByRoomIdAndUserId(room.getId(), userId)) {
            throw new AlreadyInRoomException("You are already inside this room.");
        }

        List<RoomPlayer> currentPlayers = roomPlayerRepository.findByRoomIdOrderBySeatNumberAsc(room.getId());
        if (currentPlayers.size() >= room.getMaxPlayers()) {
            throw new RoomFullException("Room '" + roomCode + "' has reached maximum capacity of " +
                    room.getMaxPlayers() + " players.");
        }

        // Allocate lowest available seat number (1..maxPlayers)
        Set<Integer> occupiedSeats = currentPlayers.stream()
                .map(RoomPlayer::getSeatNumber)
                .collect(Collectors.toSet());

        int assignedSeat = 1;
        for (int seat = 1; seat <= room.getMaxPlayers(); seat++) {
            if (!occupiedSeats.contains(seat)) {
                assignedSeat = seat;
                break;
            }
        }

        RoomPlayer newPlayer = RoomPlayer.builder()
                .room(room)
                .user(user)
                .seatNumber(assignedSeat)
                .ready(false)
                .build();

        RoomPlayer savedPlayer = roomPlayerRepository.save(newPlayer);
        currentPlayers.add(savedPlayer);

        log.info("Player '{}' joined room '{}' (Seat {})", user.getUsername(), roomCode, assignedSeat);

        webSocketEventPublisher.publishToRoom(room.getId(), WebSocketEventType.JOIN_ROOM, user.getId(),
                RoomPayload.builder()
                        .playerId(user.getId())
                        .username(user.getUsername())
                        .seatNumber(savedPlayer.getSeatNumber())
                        .ready(false)
                        .host(room.getHost().getId().equals(user.getId()))
                        .playerCount(currentPlayers.size())
                        .build());

        return roomMapper.toRoomResponse(room, currentPlayers);
    }

    @Override
    @Transactional
    public void leaveRoom(UUID roomId, UUID userId) {
        Room room = findRoomById(roomId);
        User user = findUserById(userId);

        RoomPlayer player = roomPlayerRepository.findByRoomIdAndUserId(roomId, userId)
                .orElseThrow(() -> new NotInRoomException("You are not seated in this room."));

        if (room.getStatus() == RoomStatus.IN_GAME) {
            throw new InvalidRoomStateException("Cannot leave room while a match is currently in progress.");
        }

        roomPlayerRepository.delete(player);
        log.info("Player '{}' left room '{}'", user.getUsername(), room.getRoomCode());

        // Check remaining players
        List<RoomPlayer> remainingPlayers = roomPlayerRepository.findByRoomIdOrderBySeatNumberAsc(roomId)
                .stream()
                .filter(p -> !p.getUser().getId().equals(userId))
                .toList();

        boolean wasHost = room.getHost().getId().equals(userId);

        if (remainingPlayers.isEmpty()) {
            // No players left -> cancel room
            room.setStatus(RoomStatus.CANCELLED);
            roomRepository.save(room);
            log.info("Room '{}' cancelled as all players have left.", room.getRoomCode());

            webSocketEventPublisher.publishToRoom(roomId, WebSocketEventType.LEAVE_ROOM, userId,
                    RoomPayload.builder()
                            .playerId(userId)
                            .username(user.getUsername())
                            .playerCount(0)
                            .build());
            webSocketEventPublisher.publishToRoom(roomId, WebSocketEventType.ROOM_CANCELLED, userId,
                    RoomPayload.builder().playerCount(0).build());
        } else if (wasHost) {
            // Assign next earliest player as the new host
            RoomPlayer newHostPlayer = remainingPlayers.get(0);
            room.setHost(newHostPlayer.getUser());
            roomRepository.save(room);

            log.info("Host transferred to '{}' for room '{}'", newHostPlayer.getUser().getUsername(), room.getRoomCode());

            webSocketEventPublisher.publishToRoom(roomId, WebSocketEventType.LEAVE_ROOM, userId,
                    RoomPayload.builder()
                            .playerId(userId)
                            .username(user.getUsername())
                            .playerCount(remainingPlayers.size())
                            .build());

            webSocketEventPublisher.publishToRoom(roomId, WebSocketEventType.HOST_CHANGED, newHostPlayer.getUser().getId(),
                    RoomPayload.builder()
                            .playerId(newHostPlayer.getUser().getId())
                            .username(newHostPlayer.getUser().getUsername())
                            .host(true)
                            .newHostId(newHostPlayer.getUser().getId())
                            .newHostUsername(newHostPlayer.getUser().getUsername())
                            .playerCount(remainingPlayers.size())
                            .build());
        } else {
            // Regular player left
            webSocketEventPublisher.publishToRoom(roomId, WebSocketEventType.LEAVE_ROOM, userId,
                    RoomPayload.builder()
                            .playerId(userId)
                            .username(user.getUsername())
                            .playerCount(remainingPlayers.size())
                            .build());
        }
    }

    @Override
    @Transactional
    public void deleteRoom(UUID roomId, UUID userId) {
        Room room = findRoomById(roomId);

        if (!room.getHost().getId().equals(userId)) {
            throw new NotRoomHostException("Only the room host can delete or cancel the room.");
        }

        room.setStatus(RoomStatus.CANCELLED);
        roomRepository.save(room);

        log.info("Room '{}' cancelled by host ID: {}", room.getRoomCode(), userId);

        webSocketEventPublisher.publishToRoom(roomId, WebSocketEventType.ROOM_CANCELLED, userId,
                RoomPayload.builder().playerCount(0).build());
    }

    @Override
    @Transactional(readOnly = true)
    public RoomResponse getRoom(UUID roomId) {
        Room room = findRoomById(roomId);
        List<RoomPlayer> players = roomPlayerRepository.findByRoomIdOrderBySeatNumberAsc(roomId);
        return roomMapper.toRoomResponse(room, players);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoomResponse> listAvailableRooms() {
        List<Room> waitingRooms = roomRepository.findByStatus(RoomStatus.WAITING);

        return waitingRooms.stream()
                .map(room -> {
                    List<RoomPlayer> players = roomPlayerRepository.findByRoomIdOrderBySeatNumberAsc(room.getId());
                    return roomMapper.toRoomResponse(room, players);
                })
                .filter(resp -> resp.getCurrentPlayers() < resp.getMaxPlayers())
                .toList();
    }

    @Override
    @Transactional
    public RoomResponse setReadyStatus(UUID roomId, UUID userId, boolean ready) {
        Room room = findRoomById(roomId);

        if (room.getStatus() != RoomStatus.WAITING) {
            throw new InvalidRoomStateException("Cannot change readiness when room status is " + room.getStatus());
        }

        RoomPlayer player = roomPlayerRepository.findByRoomIdAndUserId(roomId, userId)
                .orElseThrow(() -> new NotInRoomException("You are not seated in this room."));

        player.setReady(ready);
        roomPlayerRepository.save(player);

        log.info("Player '{}' set ready status to {} in room '{}'",
                player.getUser().getUsername(), ready, room.getRoomCode());

        List<RoomPlayer> players = roomPlayerRepository.findByRoomIdOrderBySeatNumberAsc(roomId);

        webSocketEventPublisher.publishToRoom(roomId, ready ? WebSocketEventType.READY : WebSocketEventType.UNREADY, userId,
                RoomPayload.builder()
                        .playerId(userId)
                        .username(player.getUser().getUsername())
                        .seatNumber(player.getSeatNumber())
                        .ready(ready)
                        .host(room.getHost().getId().equals(userId))
                        .playerCount(players.size())
                        .build());

        return roomMapper.toRoomResponse(room, players);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canStartMatch(UUID roomId) {
        Room room = findRoomById(roomId);
        List<RoomPlayer> players = roomPlayerRepository.findByRoomIdOrderBySeatNumberAsc(roomId);
        return roomMapper.evaluateCanStartMatch(room, players);
    }

    private User findUserById(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
    }

    private Room findRoomById(UUID roomId) {
        return roomRepository.findById(roomId)
                .orElseThrow(() -> new RoomNotFoundException("Room with ID '" + roomId + "' not found."));
    }

    private String generateUniqueRoomCode() {
        String code;
        int attempts = 0;
        do {
            int num = 100 + RANDOM.nextInt(900);
            code = "RAJ" + num;
            attempts++;
            if (attempts > 50) {
                code = "RAJ" + (1000 + RANDOM.nextInt(9000));
            }
        } while (roomRepository.existsByRoomCode(code));
        return code;
    }
}