package com.rajneeti.controller;

import com.rajneeti.dto.ApiResponse;
import com.rajneeti.dto.action.ActionRequest;
import com.rajneeti.dto.action.MatchActionResponse;
import com.rajneeti.dto.match.MatchResponse;
import com.rajneeti.dto.turn.TurnInfo;
import com.rajneeti.security.UserPrincipal;
import com.rajneeti.service.MatchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * REST Controller exposing match management endpoints.
 */
@Slf4j
@RestController
@RequestMapping({"/api/rooms", "/api/v1/rooms"})
@RequiredArgsConstructor
public class MatchController {

    private final MatchService matchService;

    /**
     * POST /api/rooms/{roomId}/start
     * Start a match for the given room (host only).
     * Creates Match + MatchPlayer records and transitions room to IN_GAME.
     */
    @PostMapping("/{roomId}/start")
    public ResponseEntity<ApiResponse<MatchResponse>> startMatch(
            @PathVariable UUID roomId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        log.info("Host '{}' requesting to start match in room ID: {}", userPrincipal.getUsername(), roomId);
        MatchResponse response = matchService.startMatch(roomId, userPrincipal.getId());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Match started successfully", response));
    }

    /**
     * GET /api/rooms/{roomId}/match
     * Get the active match for a room.
     */
    @GetMapping("/{roomId}/match")
    public ResponseEntity<ApiResponse<MatchResponse>> getActiveMatch(
            @PathVariable UUID roomId) {

        MatchResponse response = matchService.getActiveMatchByRoom(roomId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * GET /api/rooms/{roomId}/match/{matchId}
     * Get specific match details.
     */
    @GetMapping("/{roomId}/match/{matchId}")
    public ResponseEntity<ApiResponse<MatchResponse>> getMatch(
            @PathVariable UUID roomId,
            @PathVariable UUID matchId) {

        MatchResponse response = matchService.getMatch(matchId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * GET /api/rooms/{roomId}/match/{matchId}/turn
     * Read-only endpoint to get the current turn state of a match.
     * Clients must not directly set the current turn.
     */
    @GetMapping("/{roomId}/match/{matchId}/turn")
    public ResponseEntity<ApiResponse<TurnInfo>> getCurrentTurn(
            @PathVariable UUID roomId,
            @PathVariable UUID matchId) {

        TurnInfo turnInfo = matchService.getCurrentTurn(matchId);
        return ResponseEntity.ok(ApiResponse.success(turnInfo));
    }

    /**
     * POST /api/rooms/{roomId}/match/{matchId}/actions
     * Current player claims a gameplay action (e.g. Tax / Minister).
     * The action enters the pending challenge window; effects are applied
     * only when the action is resolved.
     */
    @PostMapping("/{roomId}/match/{matchId}/actions")
    public ResponseEntity<ApiResponse<MatchActionResponse>> performAction(
            @PathVariable UUID roomId,
            @PathVariable UUID matchId,
            @RequestBody @Valid ActionRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        log.info("Player '{}' claiming action {} in match ID: {}",
                userPrincipal.getUsername(), request.getAction(), matchId);

        MatchActionResponse response = matchService.performAction(matchId, userPrincipal.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Action accepted and pending resolution", response));
    }
}
