package com.rajneeti.controller;

import com.rajneeti.dto.ApiResponse;
import com.rajneeti.dto.game.ExchangeConfirmRequest;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * REST controller exposing the live, player-safe game state and instant
 * gameplay actions.
 *
 * <p>Instant actions that resolve without a block/challenge window (income)
 * and block-window actions with a minimal resolve seam (foreign aid) live
 * here. The full Block Manager and Action Resolver arrive in later modules.
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

    /**
     * POST /api/matches/{matchId}/foreign-aid
     * Declares Foreign Aid. Opens a block window (Minister can block).
     * Does NOT award coins or advance the turn until resolved.
     */
    @PostMapping("/{matchId}/foreign-aid")
    public ResponseEntity<ApiResponse<GameStateResponse>> performForeignAid(
            @PathVariable UUID matchId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        log.info("Player '{}' performing Foreign Aid in match: {}",
                userPrincipal.getUsername(), matchId);

        GameStateResponse response = gameEngine.performForeignAid(matchId, userPrincipal.getId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * POST /api/matches/{matchId}/foreign-aid/resolve
     * Resolves the pending Foreign Aid block window.
     *
     * <p>Minimal seam for Module 19 (Block Manager): once a full block
     * manager exists it will call this after processing the real
     * block/challenge flow. Today the frontend calls it when the demo
     * block dialog is dismissed.
     *
     * @param blocked {@code true} if the Minister block succeeded,
     *                {@code false} if no block or block failed
     */
    @PostMapping("/{matchId}/foreign-aid/resolve")
    public ResponseEntity<ApiResponse<GameStateResponse>> resolveForeignAid(
            @PathVariable UUID matchId,
            @RequestParam boolean blocked,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        log.info("Player '{}' resolving Foreign Aid (blocked={}) in match: {}",
                userPrincipal.getUsername(), blocked, matchId);

        GameStateResponse response = gameEngine.resolveForeignAid(
                matchId, userPrincipal.getId(), blocked);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * POST /api/matches/{matchId}/exchange
     * Declares Exchange, claiming Amla. Draws 2 cards into the actor's hand
     * (private pool) and opens a challenge window. No card swap and no turn
     * advance until the actor confirms via {@code /exchange/confirm}.
     */
    @PostMapping("/{matchId}/exchange")
    public ResponseEntity<ApiResponse<GameStateResponse>> performExchange(
            @PathVariable UUID matchId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        log.info("Player '{}' performing Exchange in match: {}",
                userPrincipal.getUsername(), matchId);

        GameStateResponse response = gameEngine.performExchange(matchId, userPrincipal.getId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * POST /api/matches/{matchId}/exchange/confirm
     * Resolves a pending Exchange: the actor keeps exactly 2 cards from the
     * server-side private pool and the rest return to the deck.
     *
     * @param request body containing the {@code keepCardIds} to keep
     */
    @PostMapping("/{matchId}/exchange/confirm")
    public ResponseEntity<ApiResponse<GameStateResponse>> confirmExchange(
            @PathVariable UUID matchId,
            @RequestBody ExchangeConfirmRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        log.info("Player '{}' resolving Exchange in match: {} (keeping {} cards)",
                userPrincipal.getUsername(), matchId,
                request != null && request.getKeepCardIds() != null
                        ? request.getKeepCardIds().size() : 0);

        GameStateResponse response = gameEngine.confirmExchange(
                matchId, userPrincipal.getId(),
                request != null ? request.getKeepCardIds() : null);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}