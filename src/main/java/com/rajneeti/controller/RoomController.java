package com.rajneeti.controller;

import com.rajneeti.dto.ApiResponse;
import com.rajneeti.dto.room.CreateRoomRequest;
import com.rajneeti.dto.room.JoinRoomRequest;
import com.rajneeti.dto.room.RoomResponse;
import com.rajneeti.security.UserPrincipal;
import com.rajneeti.service.RoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * REST Controller exposing game lobby room management endpoints.
 */
@Slf4j
@RestController
@RequestMapping({"/api/rooms", "/api/v1/rooms"})
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;

    /**
     * POST /api/rooms
     * Create a new room lobby with the authenticated player as host.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<RoomResponse>> createRoom(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestBody(required = false) CreateRoomRequest request) {

        log.info("Player '{}' requested to create a room", userPrincipal.getUsername());
        CreateRoomRequest safeRequest = (request != null) ? request : new CreateRoomRequest();
        RoomResponse response = roomService.createRoom(userPrincipal.getId(), safeRequest);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Room created successfully", response));
    }

    /**
     * POST /api/rooms/join
     * Join an existing waiting room via room code.
     */
    @PostMapping("/join")
    public ResponseEntity<ApiResponse<RoomResponse>> joinRoom(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody JoinRoomRequest request) {

        log.info("Player '{}' joining room with code '{}'", userPrincipal.getUsername(), request.getRoomCode());
        RoomResponse response = roomService.joinRoom(userPrincipal.getId(), request);

        return ResponseEntity.ok(ApiResponse.success("Joined room successfully", response));
    }

    /**
     * POST /api/rooms/{roomId}/leave
     * Leave a room before the game starts.
     */
    @PostMapping("/{roomId}/leave")
    public ResponseEntity<ApiResponse<Void>> leaveRoom(
            @PathVariable UUID roomId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        log.info("Player '{}' leaving room ID: {}", userPrincipal.getUsername(), roomId);
        roomService.leaveRoom(roomId, userPrincipal.getId());

        return ResponseEntity.ok(ApiResponse.success("Left room successfully"));
    }

    /**
     * DELETE /api/rooms/{roomId}
     * Cancel/delete the room lobby (host only).
     */
    @DeleteMapping("/{roomId}")
    public ResponseEntity<ApiResponse<Void>> deleteRoom(
            @PathVariable UUID roomId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        log.info("Host '{}' deleting room ID: {}", userPrincipal.getUsername(), roomId);
        roomService.deleteRoom(roomId, userPrincipal.getId());

        return ResponseEntity.ok(ApiResponse.success("Room deleted successfully"));
    }

    /**
     * GET /api/rooms/{roomId}
     * Get room details, seated players, and readiness status.
     */
    @GetMapping("/{roomId}")
    public ResponseEntity<ApiResponse<RoomResponse>> getRoom(
            @PathVariable UUID roomId) {

        RoomResponse response = roomService.getRoom(roomId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * GET /api/rooms
     * List all joinable rooms in WAITING status.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<RoomResponse>>> listAvailableRooms() {
        List<RoomResponse> rooms = roomService.listAvailableRooms();
        return ResponseEntity.ok(ApiResponse.success(rooms));
    }

    /**
     * POST /api/rooms/{roomId}/ready
     * Toggle player ready status to true.
     */
    @PostMapping("/{roomId}/ready")
    public ResponseEntity<ApiResponse<RoomResponse>> setReady(
            @PathVariable UUID roomId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        log.info("Player '{}' set ready in room ID: {}", userPrincipal.getUsername(), roomId);
        RoomResponse response = roomService.setReadyStatus(roomId, userPrincipal.getId(), true);

        return ResponseEntity.ok(ApiResponse.success("Ready status updated", response));
    }

    /**
     * POST /api/rooms/{roomId}/unready
     * Toggle player ready status to false.
     */
    @PostMapping("/{roomId}/unready")
    public ResponseEntity<ApiResponse<RoomResponse>> setUnready(
            @PathVariable UUID roomId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        log.info("Player '{}' set unready in room ID: {}", userPrincipal.getUsername(), roomId);
        RoomResponse response = roomService.setReadyStatus(roomId, userPrincipal.getId(), false);

        return ResponseEntity.ok(ApiResponse.success("Unready status updated", response));
    }
}