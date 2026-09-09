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
}