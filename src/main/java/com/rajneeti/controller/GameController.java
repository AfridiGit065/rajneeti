package com.rajneeti.controller;

import com.rajneeti.dto.ApiResponse;
import com.rajneeti.dto.game.AssassinateRequest;
import com.rajneeti.dto.game.BlockRequest;
import com.rajneeti.dto.game.CoupRequest;
import com.rajneeti.dto.game.ExchangeConfirmRequest;
import com.rajneeti.dto.game.GameStateResponse;
import com.rajneeti.dto.game.StealRequest;
import com.rajneeti.game.ActionResolver;
import com.rajneeti.game.BlockManager;
import com.rajneeti.game.ChallengeManager;
import com.rajneeti.game.GameEngine;
import com.rajneeti.game.DuplicateRequestGuard;
import com.rajneeti.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * REST controller exposing the live, player-safe game state and instant
 * gameplay actions.
 *
 * <p>Instant actions that resolve without a block/challenge window (income)
 * and claim-based actions (foreign aid, exchange, assassination, tax, steal)
 * are declared here. Challenge claims are resolved server-side by the
 * {@link ChallengeManager} (Module 18), block claims by the
 * {@link BlockManager} (Module 19), and the final verdict is applied by the
 * {@link ActionResolver} (Module 20) through {@code POST /resolve}. No
 * client-supplied outcome boolean is accepted.
 */
@Slf4j
@RestController
@RequestMapping({"/api/matches", "/api/v1/matches"})
@RequiredArgsConstructor
public class GameController {

    private final GameEngine gameEngine;
    private final ChallengeManager challengeManager;
    private final BlockManager blockManager;
    private final ActionResolver actionResolver;
    private final DuplicateRequestGuard duplicateRequestGuard;

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
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        duplicateRequestGuard.rejectDuplicate(userPrincipal.getId().toString(), requestId);

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
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        duplicateRequestGuard.rejectDuplicate(userPrincipal.getId().toString(), requestId);

        log.info("Player '{}' performing Foreign Aid in match: {}",
                userPrincipal.getUsername(), matchId);

        GameStateResponse response = gameEngine.performForeignAid(matchId, userPrincipal.getId());
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
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        duplicateRequestGuard.rejectDuplicate(userPrincipal.getId().toString(), requestId);

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
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        duplicateRequestGuard.rejectDuplicate(userPrincipal.getId().toString(), requestId);

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
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        duplicateRequestGuard.rejectDuplicate(userPrincipal.getId().toString(), requestId);

        UUID targetPlayerId = request != null ? request.getTargetPlayerId() : null;
        log.info("Player '{}' performing Assassination on '{}' in match: {}",
                userPrincipal.getUsername(), targetPlayerId, matchId);

        GameStateResponse response = gameEngine.performAssassinate(
                matchId, userPrincipal.getId(), targetPlayerId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * POST /api/matches/{matchId}/coup
     * Launches a Coup against an opponent. Coup cannot be blocked or
     * challenged: after validation it deducts 7 coins, removes one influence
     * card from the target and advances the turn instantly (no pending window).
     *
     * @param request body containing the {@code targetPlayerId}
     */
    @PostMapping("/{matchId}/coup")
    public ResponseEntity<ApiResponse<GameStateResponse>> performCoup(
            @PathVariable UUID matchId,
            @RequestBody CoupRequest request,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        duplicateRequestGuard.rejectDuplicate(userPrincipal.getId().toString(), requestId);

        UUID targetPlayerId = request != null ? request.getTargetPlayerId() : null;
        log.info("Player '{}' launching a Coup on '{}' in match: {}",
                userPrincipal.getUsername(), targetPlayerId, matchId);

        GameStateResponse response = gameEngine.performCoup(
                matchId, userPrincipal.getId(), targetPlayerId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    /**
     * POST /api/matches/{matchId}/tax
     * Declares Tax, claiming Minister. Opens a challenge window. Coins are
     * awarded only when the action resolves server-side via {@code /resolve}.
     */
    @PostMapping("/{matchId}/tax")
    public ResponseEntity<ApiResponse<GameStateResponse>> performTax(
            @PathVariable UUID matchId,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        duplicateRequestGuard.rejectDuplicate(userPrincipal.getId().toString(), requestId);

        log.info("Player '{}' performing Tax in match: {}",
                userPrincipal.getUsername(), matchId);

        GameStateResponse response = gameEngine.performTax(matchId, userPrincipal.getId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * POST /api/matches/{matchId}/steal
     * Declares Steal, claiming Dalal, targeting an opponent. Opens a challenge
     * window. Coins are transferred only when the action resolves server-side
     * via {@code /resolve}.
     *
     * @param request body containing the {@code targetPlayerId}
     */
    @PostMapping("/{matchId}/steal")
    public ResponseEntity<ApiResponse<GameStateResponse>> performSteal(
            @PathVariable UUID matchId,
            @RequestBody StealRequest request,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        duplicateRequestGuard.rejectDuplicate(userPrincipal.getId().toString(), requestId);

        UUID targetPlayerId = request != null ? request.getTargetPlayerId() : null;
        log.info("Player '{}' performing Steal on '{}' in match: {}",
                userPrincipal.getUsername(), targetPlayerId, matchId);

        GameStateResponse response = gameEngine.performSteal(
                matchId, userPrincipal.getId(), targetPlayerId);
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
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        duplicateRequestGuard.rejectDuplicate(userPrincipal.getId().toString(), requestId);

        log.info("Player '{}' challenging the pending action in match: {}",
                userPrincipal.getUsername(), matchId);

        GameStateResponse response = challengeManager.challenge(
                matchId, userPrincipal.getId(), loserCardId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * POST /api/matches/{matchId}/block
     * Module 19 — records a block claim by the caller against the currently
     * pending blockable action. The blocker asserts a character id; ownership
     * of that character is verified only if the block is challenged later.
     *
     * @param request body containing the {@code claimedCharacter}
     */
    @PostMapping("/{matchId}/block")
    public ResponseEntity<ApiResponse<GameStateResponse>> block(
            @PathVariable UUID matchId,
            @RequestBody BlockRequest request,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        duplicateRequestGuard.rejectDuplicate(userPrincipal.getId().toString(), requestId);

        String claimedCharacter = request != null ? request.getClaimedCharacter() : null;
        log.info("Player '{}' claiming a block (character={}) in match: {}",
                userPrincipal.getUsername(), claimedCharacter, matchId);

        GameStateResponse response = blockManager.block(
                matchId, userPrincipal.getId(), claimedCharacter);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * POST /api/matches/{matchId}/resolve
     * Module 20 — resolves the currently pending action for the actor, closing
     * the block/challenge window and awarding (or cancelling) the action's
     * effect entirely server-side. No client-supplied boolean is accepted for
     * the decision; the Action Resolver reads the flags that the Challenge
     * Manager (Module 18) and the Block Manager (Module 19) recorded on the
     * pending action and derives the outcome server-side.
     *
     * <p>The single verdict is returned as {@code lastActionResult} on the
     * game state and persists until the next action is declared.
     */
    @PostMapping("/{matchId}/resolve")
    public ResponseEntity<ApiResponse<GameStateResponse>> resolveAction(
            @PathVariable UUID matchId,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        duplicateRequestGuard.rejectDuplicate(userPrincipal.getId().toString(), requestId);

        log.info("Player '{}' resolving the pending action in match: {}",
                userPrincipal.getUsername(), matchId);

        GameStateResponse response = actionResolver.resolve(
                matchId, userPrincipal.getId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
