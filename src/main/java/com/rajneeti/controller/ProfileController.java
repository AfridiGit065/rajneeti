package com.rajneeti.controller;

import com.rajneeti.dto.ApiResponse;
import com.rajneeti.dto.profile.ProfileResponse;
import com.rajneeti.dto.profile.StatisticsResponse;
import com.rajneeti.dto.profile.UpdateProfileRequest;
import com.rajneeti.security.UserPrincipal;
import com.rajneeti.service.ProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller exposing endpoints for player profile inspection and updates.
 *
 * <p>All endpoints are protected and derive user identity exclusively from the
 * authenticated {@link UserPrincipal} in the SecurityContext.
 */
@Slf4j
@RestController
@RequestMapping({"/api/profile", "/api/v1/profile"})
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    /**
     * GET /api/profile
     * Retrieve the authenticated user's own profile.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<ProfileResponse>> getProfile(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        log.info("Fetching profile for authenticated user ID: {}", userPrincipal.getId());
        ProfileResponse response = profileService.getProfile(userPrincipal.getId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * PUT /api/profile
     * Update editable profile fields (username and avatarUrl) for the authenticated player.
     */
    @PutMapping
    public ResponseEntity<ApiResponse<ProfileResponse>> updateProfile(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody UpdateProfileRequest request) {

        log.info("Updating profile for authenticated user ID: {}", userPrincipal.getId());
        ProfileResponse response = profileService.updateProfile(userPrincipal.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Profile updated successfully", response));
    }

    /**
     * GET /api/profile/statistics
     * Retrieve game statistics for the authenticated player.
     */
    @GetMapping("/statistics")
    public ResponseEntity<ApiResponse<StatisticsResponse>> getStatistics(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        log.info("Fetching statistics for authenticated user ID: {}", userPrincipal.getId());
        StatisticsResponse response = profileService.getStatistics(userPrincipal.getId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}