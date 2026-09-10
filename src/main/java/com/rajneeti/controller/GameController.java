package com.rajneeti.controller;

import com.rajneeti.dto.ApiResponse;
import com.rajneeti.dto.game.GameStateResponse;
import com.rajneeti.game.GameEngine;
import com.rajneeti.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * REST controller exposing the live, player-safe game state and instant
 * gameplay actions.
 *
 * <p>Instant actions that resolve without a block/challenge window (income)
 * live here. Actions that open a response window (foreign aid, steal, ...)
 * arrive in a later module.
 */
@Slf4j
@RestController
@RequestMapping({"/api/matches", "/api/v1/matches"})
@RequiredArgsConstructor
public class GameController {

    private final GameEngine gameEngine;

    /**
     * GET /api/matches/{matchId}/game
     * Returns the player-safe game state from the perspective of the caller:
     * own cards visible, opponents' cards hidden (influence count only).
     */
    @GetMapping("/{matchId}/game")
    public ResponseEntity<ApiResponse<GameStateResponse>> getGame(
            @PathVariable UUID matchId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        log.info("Player '{}' requesting game state for match: {}",
                userPrincipal.getUsername(), matchId);

        GameStateResponse response = gameEngine.getSafeGameState(matchId, userPrincipal.getId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * POST /api/matches/{matchId}/income
     * Performs the unblockable, unchallengeable Income action for the caller.
     * Resolves instantly: +1 coin and the turn advances.
     */
    @PostMapping("/{matchId}/income")
    public ResponseEntity<ApiResponse<GameStateResponse>> performIncome(
            @PathVariable UUID matchId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        log.info("Player '{}' performing Income in match: {}",
                userPrincipal.getUsername(), matchId);

        GameStateResponse response = gameEngine.performIncome(matchId, userPrincipal.getId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}