package com.rajneeti.controller;

import com.rajneeti.dto.room.RoomEventResponse;
import com.rajneeti.dto.room.RoomResponse;
import com.rajneeti.service.RoomService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.Map;
import java.util.UUID;

/**
 * STOMP WebSocket controller for bi-directional in-room communication.
 *
 * <p>Clients subscribe to {@code /topic/rooms/{roomId}} and send messages to
 * {@code /app/rooms/{roomId}/*}.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class RoomWebSocketController {

    private final RoomService           roomService;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Handles in-room chat or ping broadcasts.
     * Destination: /app/rooms/{roomId}/chat
     * Broadcasts to: /topic/rooms/{roomId}
     */
    @MessageMapping("/rooms/{roomId}/chat")
    public void handleRoomChat(
            @DestinationVariable UUID roomId,
            @Payload Map<String, String> payload,
            Principal principal) {

        String message = payload.getOrDefault("message", "");
        String username = (principal != null) ? principal.getName() : "Anonymous";

        log.debug("Room chat in [{}]: {}: {}", roomId, username, message);

        RoomEventResponse event = RoomEventResponse.builder()
                .event("CHAT_MESSAGE")
                .roomId(roomId)
                .username(username)
                .message(message)
                .build();

        messagingTemplate.convertAndSend("/topic/rooms/" + roomId, event);
    }

    /**
     * Clients can request a fresh state synchronization.
     * Destination: /app/rooms/{roomId}/sync
     * Broadcasts to: /topic/rooms/{roomId}
     */
    @MessageMapping("/rooms/{roomId}/sync")
    public void syncRoomState(@DestinationVariable UUID roomId) {
        RoomResponse roomResponse = roomService.getRoom(roomId);

        messagingTemplate.convertAndSend("/topic/rooms/" + roomId, RoomEventResponse.builder()
                .event("ROOM_UPDATED")
                .roomId(roomId)
                .roomCode(roomResponse.getRoomCode())
                .message("Room state synchronized.")
                .build());
    }
}