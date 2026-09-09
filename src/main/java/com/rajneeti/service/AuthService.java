package com.rajneeti.service;

import com.rajneeti.dto.auth.AuthResponse;
import com.rajneeti.dto.auth.LoginRequest;
import com.rajneeti.dto.auth.RefreshTokenRequest;
import com.rajneeti.dto.auth.RegisterRequest;
import com.rajneeti.dto.auth.UserResponse;

import java.util.UUID;

/**
 * Service handling player registration, authentication, token lifecycle, and session invalidation.
 */
public interface AuthService {

    /**
     * Registers a new user account with BCrypt-hashed password.
     */
    UserResponse register(RegisterRequest request);

    /**
     * Authenticates player credentials and issues access and refresh tokens.
     */
    AuthResponse login(LoginRequest request);

    /**
     * Validates a refresh token and generates a fresh access token.
     */
    AuthResponse refreshToken(RefreshTokenRequest request);

    /**
     * Revokes user refresh tokens and clears authentication context.
     */
    void logout(String refreshToken, UUID currentUserId);

    /**
     * Retrieves the profile of the currently authenticated player.
     */
    UserResponse getCurrentUser(UUID currentUserId);
}