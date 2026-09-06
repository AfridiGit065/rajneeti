package com.rajneeti.service;

import com.rajneeti.dto.room.CreateRoomRequest;
import com.rajneeti.dto.room.JoinRoomRequest;
import com.rajneeti.dto.room.RoomResponse;
import com.rajneeti.entity.Room;

import java.util.List;
import java.util.UUID;

/**
 * Service managing room lobby lifecycle, player memberships, seat allocation, and readiness state.
 */
public interface RoomService {

    /**
     * Creates a new game room lobby with the authenticated user as host.
     */
    RoomResponse createRoom(UUID userId, CreateRoomRequest request);

    /**
     * Allows an authenticated player to join a room by its alphanumeric room code.
     */
    RoomResponse joinRoom(UUID userId, JoinRoomRequest request);

    /**
     * Allows a player to leave the room. Handles host succession or room cancellation.
     */
    void leaveRoom(UUID roomId, UUID userId);

    /**
     * Cancels/deletes the room. Strictly restricted to the room host.
     */
    void deleteRoom(UUID roomId, UUID userId);

    /**
     * Retrieves the current room state, seated players, and readiness.
     */
    RoomResponse getRoom(UUID roomId);

    /**
     * Lists all rooms currently in WAITING status with available seats.
     */
    List<RoomResponse> listAvailableRooms();

    /**
     * Toggles or updates the ready status of a player in a room.
     */
    RoomResponse setReadyStatus(UUID roomId, UUID userId, boolean ready);

    /**
     * Evaluates if the room satisfies all criteria to launch a match.
     */
    boolean canStartMatch(UUID roomId);
}