package com.rajneeti.controller;

import com.rajneeti.dto.room.ChatMessageRequest;
import com.rajneeti.dto.room.RoomResponse;
import com.rajneeti.dto.websocket.ChatMessagePayload;
import com.rajneeti.dto.websocket.WebSocketEventType;
import com.rajneeti.exception.BusinessException;
import com.rajneeti.repository.RoomPlayerRepository;
import com.rajneeti.service.RoomService;
import com.rajneeti.websocket.WebSocketEventPublisher;
import com.rajneeti.websocket.WebSocketPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.UUID;

/**
 * STOMP WebSocket controller for bi-directional in-room communication.
 *
 * <p>Clients subscribe to {@code /topic/rooms/{roomId}} (or their private
 * {@code /user/queue/events}) and send messages to {@code /app/rooms/{roomId}/*}.
 * Identity is never read from the payload — it comes from the JWT-derived
 * {@link Principal}. Chat and sync are the only inbound room commands in
 * Module 22; gameplay actions continue to go through the REST APIs and the
 * resulting events are broadcast server-side by the game layer.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class RoomWebSocketController {

    private static final int CHAT_MAX_LENGTH = 500;

    private final RoomService roomService;
    private final RoomPlayerRepository roomPlayerRepository;
    private final WebSocketEventPublisher webSocketEventPublisher;

    /**
     * Handles validated in-room chat.
     * Destination: /app/rooms/{roomId}/chat
     * Broadcasts to: /topic/rooms/{roomId}
     */
    @MessageMapping("/rooms/{roomId}/chat")
    public void handleRoomChat(
            @DestinationVariable UUID roomId,
            @Payload ChatMessageRequest payload,
            Principal principal) {

        UUID senderId = resolveUserId(principal);
        String username = resolveUsername(principal);

        String message = payload == null ? null : payload.getMessage();
        validateChat(roomId, senderId, message);

        log.debug("Room chat in [{}]: {}: {}", roomId, username, message);

        webSocketEventPublisher.publishToRoom(roomId, WebSocketEventType.CHAT_MESSAGE, senderId,
                ChatMessagePayload.builder()
                        .senderId(senderId)
                        .senderUsername(username)
                        .message(message)
                        .build());
    }

    /**
     * Clients can request a fresh state synchronization.
     * Destination: /app/rooms/{roomId}/sync
     * Broadcasts to: /topic/rooms/{roomId}
     */
    @MessageMapping("/rooms/{roomId}/sync")
    public void syncRoomState(@DestinationVariable UUID roomId) {
        RoomResponse roomResponse = roomService.getRoom(roomId);
        webSocketEventPublisher.publishToRoom(roomId, WebSocketEventType.ROOM_UPDATED, null,
                roomResponse);
    }

    private void validateChat(UUID roomId, UUID senderId, String message) {
        if (senderId == null) {
            throw new BusinessException("NOT_AUTHENTICATED",
                    "An authenticated connection is required to send chat messages.");
        }
        if (message == null || message.trim().isEmpty()) {
            throw new BusinessException("CHAT_EMPTY_MESSAGE", "Chat message cannot be empty.");
        }
        if (message.trim().length() > CHAT_MAX_LENGTH) {
            throw new BusinessException("CHAT_MESSAGE_TOO_LONG",
                    "Chat message cannot exceed " + CHAT_MAX_LENGTH + " characters.");
        }
        if (!roomPlayerRepository.existsByRoomIdAndUserId(roomId, senderId)) {
            throw new BusinessException("NOT_IN_ROOM",
                    "You must be a member of the room to send chat messages.");
        }
    }

    private UUID resolveUserId(Principal principal) {
        if (principal instanceof WebSocketPrincipal wsPrincipal) {
            return wsPrincipal.getUserId();
        }
        if (principal != null) {
            try {
                return UUID.fromString(principal.getName());
            } catch (IllegalArgumentException ignored) {
                return null;
            }
        }
        return null;
    }

    private String resolveUsername(Principal principal) {
        if (principal instanceof WebSocketPrincipal wsPrincipal) {
            return wsPrincipal.getUsername();
        }
        return principal != null ? principal.getName() : "Unknown";
    }
}