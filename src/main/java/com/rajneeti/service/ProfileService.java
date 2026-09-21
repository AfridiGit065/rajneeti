package com.rajneeti.service;

import com.rajneeti.dto.profile.ProfileResponse;
import com.rajneeti.dto.profile.StatisticsResponse;
import com.rajneeti.dto.profile.UpdateProfileRequest;

import java.util.UUID;

/**
 * Service for retrieving and modifying player profile and game statistics.
 */
public interface ProfileService {

    /**
     * Retrieves the profile of the player identified by {@code userId}.
     */
    ProfileResponse getProfile(UUID userId);

    /**
     * Updates editable profile fields (username and avatarUrl) for the player.
     * Protected statistics fields cannot be altered through this method.
     */
    ProfileResponse updateProfile(UUID userId, UpdateProfileRequest request);

    /**
     * Retrieves game statistics for the player identified by {@code userId}.
     */
    StatisticsResponse getStatistics(UUID userId);

    /**
     * Generates an in-memory PDF document containing the statistics for the
     * player identified by {@code userId}. The returned byte array contains a
     * fully formed PDF and may be streamed directly to the HTTP response.
     *
     * @param userId the authenticated player's user ID
     * @return raw PDF bytes
     */
    byte[] generateStatisticsPdf(UUID userId);
}