package com.rajneeti.controller;

import com.rajneeti.dto.websocket.MatchSyncRequest;
import com.rajneeti.exception.BusinessException;
import com.rajneeti.game.DuplicateRequestGuard;
import com.rajneeti.game.GameEngine;
import com.rajneeti.game.GameState;
import com.rajneeti.game.GameStateSyncService;
import com.rajneeti.repository.MatchPlayerRepository;
import com.rajneeti.websocket.WebSocketPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.UUID;

/**
 * Module 23 — STOMP endpoint for client-initiated game-state resynchronization.
 *
 * <p>Clients send {@code { requestId, version }} to {@code /app/matches/{matchId}/sync}
 * whenever they detect a stale or missing snapshot (a version gap on
 * {@code STATE_UPDATED}, or a full reload). The handler verifies that the sender
 * is a member of the match, de-duplicates the {@code requestId} so a delayed
 * retry cannot re-arm the client twice, then replies with a fresh
 * {@code PRIVATE_STATE} snapshot (current authoritative version, no bump) on the
 * sender's own queue.
 *
 * <p>Errors thrown here (e.g. {@code NOT_IN_MATCH}, {@code DUPLICATE_REQUEST}) are
 * converted by {@code WebSocketErrorAdvice} into private WEBSOCKET_ERROR events.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class GameWebSocketController {

    private final GameEngine gameEngine;
    private final GameStateSyncService gameStateSyncService;
    private final MatchPlayerRepository matchPlayerRepository;
    private final DuplicateRequestGuard duplicateRequestGuard;

    /**
     * Destination: {@code /app/matches/{matchId}/sync}
     * Replies to: the caller's private {@code /user/queue/events}
     */
    @MessageMapping("/matches/{matchId}/sync")
    public void syncMatchState(
            @DestinationVariable UUID matchId,
            @Payload MatchSyncRequest request,
            Principal principal) {

        UUID userId = resolveUserId(principal);
        if (userId == null) {
            throw new BusinessException("NOT_AUTHENTICATED",
                    "An authenticated connection is required to synchronize game state.");
        }

        // Membership authz: only players of THIS match may resync it.
        if (matchPlayerRepository.findByMatchIdAndUserId(matchId, userId).isEmpty()) {
            throw new BusinessException("NOT_IN_MATCH",
                    "You must be a member of the match to synchronize its game state.");
        }

        String requestId = request != null ? request.getRequestId() : null;
        duplicateRequestGuard.rejectDuplicate(userId.toString(), requestId);

        GameState state = gameEngine.getOrInitialize(matchId);

        log.info("Resync for match {} requested by user {} (their version: {}, serving {})",
                matchId, userId,
                request != null ? request.getVersion() : null,
                state.getStateVersion());

        gameStateSyncService.sendCurrentState(state, userId);
    }

    private UUID resolveUserId(Principal principal) {
        if (principal instanceof WebSocketPrincipal wsPrincipal) {
            return wsPrincipal.getUserId();
        }
        if (principal != null && principal.getName() != null) {
            try {
                return UUID.fromString(principal.getName());
            } catch (IllegalArgumentException ignored) {
                return null;
            }
        }
        return null;
    }
}