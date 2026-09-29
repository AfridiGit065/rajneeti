package com.rajneeti.service.impl;

import com.rajneeti.dto.meta.LeaderboardEntryDto;
import com.rajneeti.dto.meta.MatchHistoryEntryDto;
import com.rajneeti.entity.Leaderboard;
import com.rajneeti.entity.Match;
import com.rajneeti.entity.MatchHistory;
import com.rajneeti.entity.MatchPlayer;
import com.rajneeti.entity.User;
import com.rajneeti.repository.LeaderboardRepository;
import com.rajneeti.repository.MatchHistoryRepository;
import com.rajneeti.repository.MatchPlayerRepository;
import com.rajneeti.service.MetaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Implementation of {@link MetaService}. Both methods are read-only projections
 * over records already persisted by earlier modules; no game rules are touched.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MetaServiceImpl implements MetaService {

    private final LeaderboardRepository leaderboardRepository;
    private final MatchHistoryRepository matchHistoryRepository;
    private final MatchPlayerRepository matchPlayerRepository;

    @Override
    public List<LeaderboardEntryDto> getLeaderboard() {
        List<Leaderboard> rows = leaderboardRepository.findAll();
        List<Leaderboard> ranked = new ArrayList<>(rows.size());
        for (Leaderboard row : rows) {
            if (row.getUser() != null) {
                ranked.add(row);
            }
        }

        // The persisted rating column is static (never advanced when a match
        // finishes), so it cannot order the board. Rank by a rating derived
        // from real results: leaders rise, those behind sink.
        ranked.sort(Comparator
                .comparingInt((Leaderboard row) -> derivedRating(row.getWins(), row.getLosses()))
                .reversed()
                .thenComparing(Comparator.comparingInt(
                        (Leaderboard row) -> row.getWins() != null ? row.getWins() : 0).reversed())
                .thenComparing(row -> row.getId().toString()));

        List<LeaderboardEntryDto> entries = new ArrayList<>(ranked.size());
        int sequentialRank = 1;
        for (Leaderboard row : ranked) {
            User user = row.getUser();
            Integer totalMatches = row.getTotalMatches() != null ? row.getTotalMatches() : 0;
            Integer wins = row.getWins() != null ? row.getWins() : 0;
            Integer losses = row.getLosses() != null ? row.getLosses() : 0;
            double winRate = totalMatches > 0
                    ? Math.round(((double) wins / totalMatches) * 100.0 * 100.0) / 100.0
                    : 0.0;
            entries.add(LeaderboardEntryDto.builder()
                    .rank(sequentialRank)
                    .userId(user.getId())
                    .username(user.getUsername())
                    .avatarUrl(user.getAvatarUrl())
                    .rating(derivedRating(wins, losses))
                    .wins(wins)
                    .losses(losses)
                    .totalMatches(totalMatches)
                    .winRate(winRate)
                    .build());
            sequentialRank++;
            if (sequentialRank > 100) {
                break;
            }
        }
        return entries;
    }

    /**
     * Elo-style standing derived from results: a win is worth more than a loss
     * costs, so the last player standing always outranks those eliminated
     * earlier. Pure function of wins/losses so the board is always consistent
     * with the matches that actually finished.
     */
    public static int derivedRating(Integer wins, Integer losses) {
        int winCount = wins != null ? wins : 0;
        int lossCount = losses != null ? losses : 0;
        return 1000 + (winCount * 25) - (lossCount * 15);
    }

    @Override
    public List<MatchHistoryEntryDto> getMatchHistory(UUID userId) {
        List<MatchHistory> rows = matchHistoryRepository.findByUserIdOrderByCreatedAtDesc(userId);
        List<MatchHistoryEntryDto> entries = new ArrayList<>(rows.size());
        for (MatchHistory row : rows) {
            Match match = row.getMatch();
            if (match == null) {
                continue;
            }
            List<MatchPlayer> players = matchPlayerRepository.findByMatchId(match.getId());
            if (players == null) {
                players = List.of();
            }

            Integer finalRank = row.getFinalRank();
            List<MatchHistoryEntryDto.ParticipantDto> participants = players.stream()
                    .sorted(Comparator.comparing(MatchPlayer::getSeatNumber))
                    .map(player -> {
                        User participant = player.getUser();
                        if (participant == null) {
                            return null;
                        }
                        return MatchHistoryEntryDto.ParticipantDto.builder()
                                .userId(participant.getId())
                                .username(participant.getUsername())
                                .avatarUrl(participant.getAvatarUrl())
                                .seatNumber(player.getSeatNumber())
                                .finalRank(player.getFinalRank())
                                .isCurrentUser(participant.getId().equals(userId))
                                .build();
                    })
                    .filter(Objects::nonNull)
                    .toList();

            entries.add(MatchHistoryEntryDto.builder()
                    .matchId(match.getId())
                    .playedAt(row.getCreatedAt() != null
                            ? row.getCreatedAt().toString()
                            : LocalDateTime.now().toString())
                    .durationMinutes(computeDuration(match))
                    .position(finalRank != null ? finalRank : 0)
                    .playerCount(participants.isEmpty() ? 1 : participants.size())
                    .result(computeResult(match.getEndedAt() != null, finalRank, row.getEliminated()))
                    .eliminated(Boolean.TRUE.equals(row.getEliminated()))
                    .participants(participants)
                    .build());
        }
        return entries;
    }

    private int computeDuration(Match match) {
        if (match.getStartedAt() != null && match.getEndedAt() != null) {
            long minutes = Duration.between(match.getStartedAt(), match.getEndedAt()).toMinutes();
            return (int) Math.max(1L, minutes);
        }
        return 1;
    }

    private String computeResult(boolean finished, Integer finalRank, Boolean eliminated) {
        if (finished) {
            if (finalRank != null && finalRank == 1) {
                return "WIN";
            }
            return "LOSS";
        }
        return "DRAW";
    }
}