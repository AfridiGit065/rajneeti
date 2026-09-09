package com.rajneeti.service;

import com.rajneeti.dto.match.MatchResponse;

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
     * then transitions room status to IN_GAME.
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
}
