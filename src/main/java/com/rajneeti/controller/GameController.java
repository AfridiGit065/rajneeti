package com.rajneeti.controller;

import com.rajneeti.dto.ApiResponse;
import com.rajneeti.dto.game.AssassinateRequest;
import com.rajneeti.dto.game.ExchangeConfirmRequest;
import com.rajneeti.dto.game.GameStateResponse;
import com.rajneeti.dto.game.StealRequest;
import com.rajneeti.game.ChallengeManager;
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
 * and block-window actions with a minimal resolve seam (foreign aid, tax,
 * steal, exchange, assassination) live here. Challenges against claim-based
 * actions are resolved by the {@link ChallengeManager} (Module 18); the full
 * Block Manager arrives in a later module.
 */
@Slf4j
@RestController
@RequestMapping({"/api/matches", "/api/v1/matches"})
@RequiredArgsConstructor
public class GameController {

    private final GameEngine gameEngine;
    private final ChallengeManager challengeManager;

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

    /**
     * POST /api/matches/{matchId}/assassinate
     * Declares an Assassination, claiming GHATOK and targeting an opponent.
     * Opens a block/challenge window. The 3-coin cost is only reserved on the
     * pending action and is deducted on successful resolution, never up front.
     *
     * @param request body containing the {@code targetPlayerId}
     */
    @PostMapping("/{matchId}/assassinate")
    public ResponseEntity<ApiResponse<GameStateResponse>> performAssassinate(
            @PathVariable UUID matchId,
            @RequestBody AssassinateRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        UUID targetPlayerId = request != null ? request.getTargetPlayerId() : null;
        log.info("Player '{}' performing Assassination on '{}' in match: {}",
                userPrincipal.getUsername(), targetPlayerId, matchId);

        GameStateResponse response = gameEngine.performAssassinate(
                matchId, userPrincipal.getId(), targetPlayerId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * POST /api/matches/{matchId}/assassinate/resolve
     * Resolves the pending Assassination block/challenge window.
     *
     * <p>Minimal seam for the future Block/Challenge Manager: once those
     * managers exist they will call this after processing the real flow.
     * Today the frontend calls it when the demo block dialog is dismissed
     * (succeeded = no block / block failed, failed = block succeeded).
     *
     * @param succeeded {@code true} if the Assassination goes through (pay 3
     *                  coins, target loses one influence card), {@code false}
     *                  if it was prevented (no payment, no card loss)
     */
    @PostMapping("/{matchId}/assassinate/resolve")
    public ResponseEntity<ApiResponse<GameStateResponse>> resolveAssassinate(
            @PathVariable UUID matchId,
            @RequestParam boolean succeeded,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        log.info("Player '{}' resolving Assassination (succeeded={}) in match: {}",
                userPrincipal.getUsername(), succeeded, matchId);

        GameStateResponse response = gameEngine.resolveAssassinate(
                matchId, userPrincipal.getId(), succeeded);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * POST /api/matches/{matchId}/tax
     * Declares Tax, claiming Minister. Opens a challenge window. Coins are
     * awarded only when the action resolves via {@code /tax/resolve}.
     */
    @PostMapping("/{matchId}/tax")
    public ResponseEntity<ApiResponse<GameStateResponse>> performTax(
            @PathVariable UUID matchId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        log.info("Player '{}' performing Tax in match: {}",
                userPrincipal.getUsername(), matchId);

        GameStateResponse response = gameEngine.performTax(matchId, userPrincipal.getId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * POST /api/matches/{matchId}/tax/resolve
     * Resolves a pending Tax challenge window.
     *
     * @param granted {@code true} awards the 3 coin gain (truthful claim),
     *                {@code false} cancels the Tax without awarding coins
     */
    @PostMapping("/{matchId}/tax/resolve")
    public ResponseEntity<ApiResponse<GameStateResponse>> resolveTax(
            @PathVariable UUID matchId,
            @RequestParam boolean granted,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        log.info("Player '{}' resolving Tax (granted={}) in match: {}",
                userPrincipal.getUsername(), granted, matchId);

        GameStateResponse response = gameEngine.resolveTax(
                matchId, userPrincipal.getId(), granted);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * POST /api/matches/{matchId}/steal
     * Declares Steal, claiming Dalal, targeting an opponent. Opens a challenge
     * window. Coins are transferred only when the action resolves via
     * {@code /steal/resolve}.
     *
     * @param request body containing the {@code targetPlayerId}
     */
    @PostMapping("/{matchId}/steal")
    public ResponseEntity<ApiResponse<GameStateResponse>> performSteal(
            @PathVariable UUID matchId,
            @RequestBody StealRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        UUID targetPlayerId = request != null ? request.getTargetPlayerId() : null;
        log.info("Player '{}' performing Steal on '{}' in match: {}",
                userPrincipal.getUsername(), targetPlayerId, matchId);

        GameStateResponse response = gameEngine.performSteal(
                matchId, userPrincipal.getId(), targetPlayerId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * POST /api/matches/{matchId}/steal/resolve
     * Resolves a pending Steal challenge window.
     *
     * @param granted {@code true} transfers up to 2 coins from the target to the
     *                actor (truthful claim), {@code false} cancels the Steal
     *                without transferring coins
     */
    @PostMapping("/{matchId}/steal/resolve")
    public ResponseEntity<ApiResponse<GameStateResponse>> resolveSteal(
            @PathVariable UUID matchId,
            @RequestParam boolean granted,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        log.info("Player '{}' resolving Steal (granted={}) in match: {}",
                userPrincipal.getUsername(), granted, matchId);

        GameStateResponse response = gameEngine.resolveSteal(
                matchId, userPrincipal.getId(), granted);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * POST /api/matches/{matchId}/challenge
     * Module 18 — resolves a challenge raised by the caller against the
     * currently pending claim. The decision is made entirely server-side: the
     * caller never supplies card ownership.
     *
     * <p>A truthful claim makes the challenger lose one influence card and lets
     * the original action continue; a bluff makes the claimant lose one
     * influence card and cancels the action (for an Exchange the actor's
     * original hand is restored, for an Assassination no coins are deducted).
     * The verdict is returned as {@code lastChallenge} on the game state and
     * stays until the next action begins.
     *
     * @param loserCardId optional physical card ID the challenger loses/stakes
     *                    if the claim turns out truthful (must belong to the
     *                    challenger); when absent the first card is removed
     */
    @PostMapping("/{matchId}/challenge")
    public ResponseEntity<ApiResponse<GameStateResponse>> challenge(
            @PathVariable UUID matchId,
            @RequestParam(value = "loserCardId", required = false) UUID loserCardId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        log.info("Player '{}' challenging the pending action in match: {}",
                userPrincipal.getUsername(), matchId);

        GameStateResponse response = challengeManager.challenge(
                matchId, userPrincipal.getId(), loserCardId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}