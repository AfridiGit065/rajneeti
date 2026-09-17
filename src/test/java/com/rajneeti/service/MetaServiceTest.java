package com.rajneeti.service;

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
import com.rajneeti.service.impl.MetaServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MetaServiceTest {

    @Mock
    private LeaderboardRepository leaderboardRepository;

    @Mock
    private MatchHistoryRepository matchHistoryRepository;

    @Mock
    private MatchPlayerRepository matchPlayerRepository;

    @InjectMocks
    private MetaServiceImpl metaService;

    private UUID userId;
    private User user;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        user = User.builder()
                .id(userId)
                .username("testplayer")
                .email("test@rajneeti.com")
                .avatarUrl("https://example.com/avatar.png")
                .build();
    }

    @Test
    @DisplayName("Get Leaderboard - Success")
    void getLeaderboard_Success() {
        Leaderboard row = Leaderboard.builder()
                .user(user)
                .rating(1450)
                .wins(30)
                .losses(10)
                .totalMatches(40)
                .rank(null)
                .build();
        when(leaderboardRepository.findTop100ByOrderByRatingDesc()).thenReturn(List.of(row));

        List<LeaderboardEntryDto> entries = metaService.getLeaderboard();

        assertThat(entries).hasSize(1);
        LeaderboardEntryDto entry = entries.get(0);
        assertThat(entry.getRank()).isEqualTo(1);
        assertThat(entry.getUserId()).isEqualTo(userId);
        assertThat(entry.getUsername()).isEqualTo("testplayer");
        assertThat(entry.getRating()).isEqualTo(1450);
        assertThat(entry.getWins()).isEqualTo(30);
        assertThat(entry.getTotalMatches()).isEqualTo(40);
        assertThat(entry.getWinRate()).isEqualTo(0.75);
    }

    @Test
    @DisplayName("Get Leaderboard - Win rate zero when no matches played")
    void getLeaderboard_NoMatches() {
        Leaderboard row = Leaderboard.builder()
                .user(user)
                .rating(1000)
                .wins(0)
                .losses(0)
                .totalMatches(0)
                .rank(3)
                .build();
        when(leaderboardRepository.findTop100ByOrderByRatingDesc()).thenReturn(List.of(row));

        List<LeaderboardEntryDto> entries = metaService.getLeaderboard();

        assertThat(entries).hasSize(1);
        assertThat(entries.get(0).getWinRate()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("Get Match History - Success, finished match is a WIN")
    void getMatchHistory_Win() {
        Match match = Match.builder()
                .id(UUID.randomUUID())
                .startedAt(LocalDateTime.now().minusMinutes(15))
                .endedAt(LocalDateTime.now())
                .build();

        User otherUser = User.builder()
                .id(UUID.randomUUID())
                .username("opponent")
                .email("opponent@test.local")
                .build();

        MatchPlayer rowSelf = MatchPlayer.builder()
                .match(match)
                .user(user)
                .seatNumber(1)
                .finalRank(1)
                .coinsAtEnd(12)
                .eliminated(false)
                .build();
        MatchPlayer rowOther = MatchPlayer.builder()
                .match(match)
                .user(otherUser)
                .seatNumber(2)
                .finalRank(2)
                .coinsAtEnd(4)
                .eliminated(true)
                .build();

        MatchHistory history = MatchHistory.builder()
                .match(match)
                .user(user)
                .finalRank(1)
                .coinsAtEnd(12)
                .eliminated(false)
                .createdAt(LocalDateTime.now())
                .build();

        when(matchHistoryRepository.findByUserIdOrderByCreatedAtDesc(userId)).thenReturn(List.of(history));
        when(matchPlayerRepository.findByMatchId(match.getId())).thenReturn(new ArrayList<>(List.of(rowSelf, rowOther)));

        List<MatchHistoryEntryDto> entries = metaService.getMatchHistory(userId);

        assertThat(entries).hasSize(1);
        MatchHistoryEntryDto entry = entries.get(0);
        assertThat(entry.getMatchId()).isEqualTo(match.getId());
        assertThat(entry.getResult()).isEqualTo("WIN");
        assertThat(entry.getDurationMinutes()).isGreaterThanOrEqualTo(1);
        assertThat(entry.getPlayerCount()).isEqualTo(2);
        assertThat(entry.getParticipants()).hasSize(2);
        assertThat(entry.getParticipants().get(0).getUsername()).isEqualTo("testplayer");
        assertThat(entry.getParticipants().get(0).getIsCurrentUser()).isTrue();
        assertThat(entry.getParticipants().get(1).getUsername()).isEqualTo("opponent");
        assertThat(entry.getParticipants().get(1).getIsCurrentUser()).isFalse();
    }

    @Test
    @DisplayName("Get Match History - Unfinished match is reported as DRAW")
    void getMatchHistory_InProgress() {
        Match match = Match.builder()
                .id(UUID.randomUUID())
                .startedAt(LocalDateTime.now().minusMinutes(3))
                .endedAt(null)
                .build();

        MatchHistory history = MatchHistory.builder()
                .match(match)
                .user(user)
                .finalRank(null)
                .coinsAtEnd(6)
                .eliminated(false)
                .createdAt(LocalDateTime.now())
                .build();

        when(matchHistoryRepository.findByUserIdOrderByCreatedAtDesc(userId)).thenReturn(List.of(history));
        when(matchPlayerRepository.findByMatchId(match.getId())).thenReturn(new ArrayList<>());

        List<MatchHistoryEntryDto> entries = metaService.getMatchHistory(userId);

        assertThat(entries).hasSize(1);
        assertThat(entries.get(0).getResult()).isEqualTo("DRAW");
        assertThat(entries.get(0).getPlayerCount()).isEqualTo(1);
    }
}