package com.rajneeti.service;

import com.rajneeti.dto.action.ActionRequest;
import com.rajneeti.dto.action.MatchActionResponse;
import com.rajneeti.dto.match.MatchResponse;
import com.rajneeti.dto.turn.TurnInfo;

import java.util.UUID;

/**
 * Service managing match lifecycle: creation, querying, and transitions.
 */
public interface MatchService {

    /**
     * Starts a match for the given room. Validates all preconditions:
     * room exists, requester is host, room is WAITING, all players ready,
     * and no active match already exists.
     *
     * Creates Match + MatchPlayer records in a single transaction,
     * assigns the first turn, then transitions room status to IN_GAME.
     *
     * @param roomId the room to start a match in
     * @param userId the authenticated user requesting (must be host)
     * @return the created match details
     */
    MatchResponse startMatch(UUID roomId, UUID userId);

    /**
     * Retrieves full match details by match ID.
     */
    MatchResponse getMatch(UUID matchId);

    /**
     * Retrieves the active match (CREATED or IN_PROGRESS) for a room.
     */
    MatchResponse getActiveMatchByRoom(UUID roomId);

    /**
     * Retrieves the current turn state for a match (read-only).
     */
    TurnInfo getCurrentTurn(UUID matchId);

    /**
     * Accepts a gameplay action claimed by the current player.
     *
     * <p>Validates that the match is active, the caller belongs to the match,
     * is not eliminated, it is their turn, and no other action is already
     * pending. The claimed action is then stored as a pending action in the
     * challenge window. No effects (coin rewards, turn advancement) are applied
     * yet — those happen when the pending action is resolved.
     *
     * <p>The caller does not need to genuinely possess the claimed character;
     * this is a bluff game and the claim is always allowed.
     *
     * @param matchId the match to act in
     * @param userId  the authenticated user performing the action
     * @param request the action details
     * @return the accepted pending action
     */
    MatchActionResponse performAction(UUID matchId, UUID userId, ActionRequest request);
}
