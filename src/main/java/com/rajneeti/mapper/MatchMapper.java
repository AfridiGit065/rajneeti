package com.rajneeti.mapper;

import com.rajneeti.dto.match.MatchPlayerResponse;
import com.rajneeti.dto.match.MatchResponse;
import com.rajneeti.entity.Match;
import com.rajneeti.entity.MatchPlayer;
import com.rajneeti.entity.Room;
import com.rajneeti.entity.User;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Mapper for transforming Match and MatchPlayer entities into presentation DTOs.
 */
@Component
public class MatchMapper {

    public MatchPlayerResponse toMatchPlayerResponse(MatchPlayer player) {
        if (player == null) {
            return null;
        }

        User user = player.getUser();

        return MatchPlayerResponse.builder()
                .id(player.getId())
                .userId(user != null ? user.getId() : null)
                .username(user != null ? user.getUsername() : null)
                .avatarUrl(user != null ? user.getAvatarUrl() : null)
                .seatNumber(player.getSeatNumber())
                .playerStatus(player.getPlayerStatus())
                .finalRank(player.getFinalRank())
                .coinsAtEnd(player.getCoinsAtEnd())
                .eliminated(player.getEliminated())
                .eliminatedAt(player.getEliminatedAt())
                .build();
    }

    public MatchResponse toMatchResponse(Match match, List<MatchPlayer> players) {
        if (match == null) {
            return null;
        }

        Room room = match.getRoom();
        User winner = match.getWinner();

        List<MatchPlayerResponse> playerResponses = (players != null)
                ? players.stream()
                .sorted(Comparator.comparing(MatchPlayer::getSeatNumber))
                .map(this::toMatchPlayerResponse)
                .toList()
                : Collections.emptyList();

        return MatchResponse.builder()
                .id(match.getId())
                .roomId(room != null ? room.getId() : null)
                .roomCode(room != null ? room.getRoomCode() : null)
                .status(match.getStatus())
                .playerCount(playerResponses.size())
                .players(playerResponses)
                .winnerId(winner != null ? winner.getId() : null)
                .winnerUsername(winner != null ? winner.getUsername() : null)
                .currentTurnPlayerId(match.getCurrentTurnPlayerId())
                .turnNumber(match.getTurnNumber())
                .turnOrder(playerResponses.stream()
                        .map(MatchPlayerResponse::getUserId)
                        .toList())
                .startedAt(match.getStartedAt())
                .endedAt(match.getEndedAt())
                .createdAt(match.getCreatedAt())
                .build();
    }
}
