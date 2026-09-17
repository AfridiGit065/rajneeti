package com.rajneeti.service;

import com.rajneeti.dto.meta.LeaderboardEntryDto;
import com.rajneeti.dto.meta.MatchHistoryEntryDto;

import java.util.List;
import java.util.UUID;

/**
 * Read-only service exposing aggregated meta data (leaderboard + match history).
 */
public interface MetaService {

    List<LeaderboardEntryDto> getLeaderboard();

    List<MatchHistoryEntryDto> getMatchHistory(UUID userId);
}