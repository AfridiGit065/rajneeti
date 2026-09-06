package com.rajneeti.service.impl;

import com.rajneeti.config.JwtProperties;
import com.rajneeti.dto.auth.AuthResponse;
import com.rajneeti.dto.auth.LoginRequest;
import com.rajneeti.dto.auth.RefreshTokenRequest;
import com.rajneeti.dto.auth.RegisterRequest;
import com.rajneeti.dto.auth.UserResponse;
import com.rajneeti.entity.Leaderboard;
import com.rajneeti.entity.RefreshToken;
import com.rajneeti.entity.Statistics;
import com.rajneeti.entity.User;
import com.rajneeti.exception.EmailAlreadyExistsException;
import com.rajneeti.exception.InvalidCredentialsException;
import com.rajneeti.exception.ResourceNotFoundException;
import com.rajneeti.exception.TokenRefreshException;
import com.rajneeti.exception.UsernameAlreadyExistsException;
import com.rajneeti.mapper.UserMapper;
import com.rajneeti.repository.LeaderboardRepository;
import com.rajneeti.repository.RefreshTokenRepository;
import com.rajneeti.repository.StatisticsRepository;
import com.rajneeti.repository.UserRepository;
import com.rajneeti.security.JwtTokenProvider;
import com.rajneeti.security.UserPrincipal;
import com.rajneeti.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Implementation of {@link AuthService}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository         userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final LeaderboardRepository   leaderboardRepository;
    private final StatisticsRepository    statisticsRepository;
    private final PasswordEncoder        passwordEncoder;
    private final JwtTokenProvider       jwtTokenProvider;
    private final JwtProperties          jwtProperties;
    private final UserMapper             userMapper;

    @Override
    @Transactional
    public UserResponse register(RegisterRequest request) {
        String username = request.getUsername().trim();
        String email = request.getEmail().trim().toLowerCase();

        if (userRepository.existsByUsername(username)) {
            throw new UsernameAlreadyExistsException("Username '%s' is already taken.".formatted(username));
        }

        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyExistsException("Email '%s' is already registered.".formatted(email));
        }

        User user = User.builder()
                .username(username)
                .email(email)
                .password(passwordEncoder.encode(request.getPassword()))
                .rating(1000)
                .totalMatches(0)
                .wins(0)
                .losses(0)
                .build();

        User savedUser = userRepository.save(user);

        // Initialize corresponding Leaderboard and Statistics records
        Leaderboard leaderboard = Leaderboard.builder()
                .user(savedUser)
                .rating(1000)
                .wins(0)
                .losses(0)
                .totalMatches(0)
                .build();
        leaderboardRepository.save(leaderboard);

        Statistics statistics = Statistics.builder()
                .user(savedUser)
                .totalMatches(0)
                .wins(0)
                .losses(0)
                .winRate(0.0)
                .totalCoinsEarned(0L)
                .totalCoinsSpent(0L)
                .build();
        statisticsRepository.save(statistics);

        log.info("Player registered successfully with username: {}", savedUser.getUsername());

        return userMapper.toUserResponse(savedUser);
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        String email = request.getEmail().trim().toLowerCase();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password."));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new InvalidCredentialsException("Invalid email or password.");
        }

        UserPrincipal userPrincipal = UserPrincipal.create(user);
        String accessToken = jwtTokenProvider.generateAccessToken(userPrincipal);
        RefreshToken refreshToken = createRefreshToken(user);

        log.info("Player '{}' logged in successfully", user.getUsername());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getExpirationInSeconds())
                .user(userMapper.toUserResponse(user))
                .build();
    }

    @Override
    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        RefreshToken refreshToken = refreshTokenRepository
                .findByTokenAndRevokedFalse(request.getRefreshToken())
                .orElseThrow(() -> new TokenRefreshException("Refresh token is invalid or has been revoked."));

        if (refreshToken.isExpired()) {
            refreshToken.setRevoked(true);
            refreshTokenRepository.save(refreshToken);
            throw new TokenRefreshException("Refresh token has expired. Please log in again.");
        }

        User user = refreshToken.getUser();
        UserPrincipal userPrincipal = UserPrincipal.create(user);
        String newAccessToken = jwtTokenProvider.generateAccessToken(userPrincipal);

        log.info("Access token refreshed for user '{}'", user.getUsername());

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(refreshToken.getToken())
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getExpirationInSeconds())
                .user(userMapper.toUserResponse(user))
                .build();
    }

    @Override
    @Transactional
    public void logout(String refreshTokenStr, UUID currentUserId) {
        if (refreshTokenStr != null && !refreshTokenStr.isBlank()) {
            refreshTokenRepository.revokeByToken(refreshTokenStr.trim());
        }

        if (currentUserId != null) {
            refreshTokenRepository.revokeAllByUserId(currentUserId);
        }

        SecurityContextHolder.clearContext();
        log.info("Logout processed successfully");
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(UUID currentUserId) {
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", currentUserId));

        return userMapper.toUserResponse(user);
    }

    private RefreshToken createRefreshToken(User user) {
        // Revoke existing active refresh tokens for the user to ensure single-session or clean rotation
        refreshTokenRepository.revokeAllByUserId(user.getId());

        String tokenValue = UUID.randomUUID().toString().replace("-", "") +
                UUID.randomUUID().toString().replace("-", "");

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(tokenValue)
                .expiryDate(Instant.now().plusMillis(jwtProperties.getRefreshExpirationMs()))
                .revoked(false)
                .createdAt(Instant.now())
                .build();

        return refreshTokenRepository.save(refreshToken);
    }
}