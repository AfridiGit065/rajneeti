package com.rajneeti.controller;

import com.rajneeti.dto.ApiResponse;
import com.rajneeti.dto.meta.LeaderboardEntryDto;
import com.rajneeti.dto.meta.MatchHistoryEntryDto;
import com.rajneeti.security.UserPrincipal;
import com.rajneeti.service.MetaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controller exposing read-only meta data: the global leaderboard and the
 * authenticated player's own match history. Both endpoints are protected by
 * Spring Security (JWT), exactly like the existing profile endpoints.
 */
@Slf4j
@RestController
@RequestMapping({"/api", "/api/v1"})
@RequiredArgsConstructor
public class MetaController {

    private final MetaService metaService;

    /**
     * GET /api/leaderboard
     * Top ranked players, ordered by rating descending.
     */
    @GetMapping("/leaderboard")
    public ResponseEntity<ApiResponse<List<LeaderboardEntryDto>>> getLeaderboard() {
        log.info("Fetching leaderboard");
        return ResponseEntity.ok(ApiResponse.success(metaService.getLeaderboard()));
    }

    /**
     * GET /api/me/match-history
     * Match results for the authenticated player, newest first.
     */
    @GetMapping("/me/match-history")
    public ResponseEntity<ApiResponse<List<MatchHistoryEntryDto>>> getMatchHistory(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        log.info("Fetching match history for user ID: {}", userPrincipal.getId());
        return ResponseEntity.ok(ApiResponse.success(metaService.getMatchHistory(userPrincipal.getId())));
    }
}