package com.rajneeti.mapper;

import com.rajneeti.dto.room.RoomPlayerResponse;
import com.rajneeti.dto.room.RoomResponse;
import com.rajneeti.entity.Room;
import com.rajneeti.entity.RoomPlayer;
import com.rajneeti.entity.enums.RoomStatus;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Mapper for transforming Room and RoomPlayer entities into presentation DTOs.
 */
@Component
public class RoomMapper {

    public RoomPlayerResponse toRoomPlayerResponse(RoomPlayer player, Room room) {
        if (player == null) {
            return null;
        }

        boolean isHost = room != null && room.getHost() != null &&
                room.getHost().getId().equals(player.getUser().getId());

        return RoomPlayerResponse.builder()
                .id(player.getUser().getId())
                .username(player.getUser().getUsername())
                .avatarUrl(player.getUser().getAvatarUrl())
                .seatNumber(player.getSeatNumber())
                .ready(player.getReady())
                .isHost(isHost)
                .joinedAt(player.getJoinedAt())
                .build();
    }

    public RoomResponse toRoomResponse(Room room, List<RoomPlayer> players) {
        if (room == null) {
            return null;
        }

        List<RoomPlayerResponse> playerResponses = (players != null)
                ? players.stream()
                .sorted(Comparator.comparing(RoomPlayer::getSeatNumber))
                .map(p -> toRoomPlayerResponse(p, room))
                .toList()
                : Collections.emptyList();

        boolean canStart = evaluateCanStartMatch(room, players);

        return RoomResponse.builder()
                .id(room.getId())
                .roomCode(room.getRoomCode())
                .hostId(room.getHost() != null ? room.getHost().getId() : null)
                .hostUsername(room.getHost() != null ? room.getHost().getUsername() : null)
                .status(room.getStatus())
                .maxPlayers(room.getMaxPlayers())
                .currentPlayers(playerResponses.size())
                .canStart(canStart)
                .players(playerResponses)
                .createdAt(room.getCreatedAt())
                .build();
    }

    public boolean evaluateCanStartMatch(Room room, List<RoomPlayer> players) {
        if (room == null || players == null) {
            return false;
        }
        if (room.getStatus() != RoomStatus.WAITING) {
            return false;
        }
        if (players.size() < 2) {
            return false;
        }
        if (room.getHost() == null) {
            return false;
        }

        // Host must be present in the room
        boolean hostPresent = players.stream()
                .anyMatch(p -> p.getUser().getId().equals(room.getHost().getId()));
        if (!hostPresent) {
            return false;
        }

        // All current players must be marked ready
        return players.stream().allMatch(p -> Boolean.TRUE.equals(p.getReady()));
    }
}