package com.rajneeti.controller;

import com.rajneeti.dto.ApiResponse;
import com.rajneeti.dto.auth.AuthResponse;
import com.rajneeti.dto.auth.LoginRequest;
import com.rajneeti.dto.auth.RefreshTokenRequest;
import com.rajneeti.dto.auth.RegisterRequest;
import com.rajneeti.dto.auth.UserResponse;
import com.rajneeti.security.UserPrincipal;
import com.rajneeti.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Controller exposing REST endpoints for user authentication and session management.
 */
@Slf4j
@RestController
@RequestMapping({"/api/auth", "/api/v1/auth"})
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * POST /api/auth/register
     * Register a new player account.
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserResponse>> register(
            @Valid @RequestBody RegisterRequest request) {

        log.info("Received registration request for username: {}", request.getUsername());
        UserResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Player registered successfully", response));
    }

    /**
     * POST /api/auth/login
     * Authenticate player with email and password, issuing access and refresh tokens.
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request) {

        log.info("Received login request for email: {}", request.getEmail());
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Authentication successful", response));
    }

    /**
     * POST /api/auth/refresh
     * Rotate or re-issue an access token using a valid refresh token.
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request) {

        log.info("Received token refresh request");
        AuthResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(ApiResponse.success("Token refreshed successfully", response));
    }

    /**
     * POST /api/auth/logout
     * Invalidate refresh tokens and clear security context.
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestBody(required = false) RefreshTokenRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        String token = (request != null) ? request.getRefreshToken() : null;
        UUID userId = (userPrincipal != null) ? userPrincipal.getId() : null;

        authService.logout(token, userId);
        return ResponseEntity.ok(ApiResponse.success("Logged out successfully"));
    }

    /**
     * GET /api/auth/me
     * Retrieve current authenticated user profile.
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        log.info("Fetching profile for current user: {}", userPrincipal.getUsername());
        UserResponse response = authService.getCurrentUser(userPrincipal.getId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}