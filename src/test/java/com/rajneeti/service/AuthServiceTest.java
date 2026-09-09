package com.rajneeti.service;

import com.rajneeti.config.JwtProperties;
import com.rajneeti.dto.auth.AuthResponse;
import com.rajneeti.dto.auth.LoginRequest;
import com.rajneeti.dto.auth.RefreshTokenRequest;
import com.rajneeti.dto.auth.RegisterRequest;
import com.rajneeti.dto.auth.UserResponse;
import com.rajneeti.entity.RefreshToken;
import com.rajneeti.entity.User;
import com.rajneeti.exception.EmailAlreadyExistsException;
import com.rajneeti.exception.InvalidCredentialsException;
import com.rajneeti.exception.TokenRefreshException;
import com.rajneeti.exception.UsernameAlreadyExistsException;
import com.rajneeti.mapper.UserMapper;
import com.rajneeti.repository.LeaderboardRepository;
import com.rajneeti.repository.RefreshTokenRepository;
import com.rajneeti.repository.StatisticsRepository;
import com.rajneeti.repository.UserRepository;
import com.rajneeti.security.JwtTokenProvider;
import com.rajneeti.security.UserPrincipal;
import com.rajneeti.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private LeaderboardRepository leaderboardRepository;

    @Mock
    private StatisticsRepository statisticsRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private JwtProperties jwtProperties;

    @Spy
    private UserMapper userMapper = new UserMapper();

    @InjectMocks
    private AuthServiceImpl authService;

    private User testUser;
    private UUID testUserId;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testUser = User.builder()
                .id(testUserId)
                .username("player123")
                .email("player@example.com")
                .password("$2a$12$hashedPasswordExample")
                .rating(1000)
                .totalMatches(0)
                .wins(0)
                .losses(0)
                .build();
    }

    @Test
    @DisplayName("Register - Success")
    void register_Success() {
        RegisterRequest request = RegisterRequest.builder()
                .username("player123")
                .email("player@example.com")
                .password("password123")
                .build();

        when(userRepository.existsByUsername("player123")).thenReturn(false);
        when(userRepository.existsByEmail("player@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("$2a$12$hashedPasswordExample");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        UserResponse response = authService.register(request);

        assertThat(response).isNotNull();
        assertThat(response.getUsername()).isEqualTo("player123");
        assertThat(response.getEmail()).isEqualTo("player@example.com");
        verify(userRepository).save(any(User.class));
        verify(leaderboardRepository).save(any());
        verify(statisticsRepository).save(any());
    }

    @Test
    @DisplayName("Register - Fails when username already exists")
    void register_UsernameAlreadyExists() {
        RegisterRequest request = RegisterRequest.builder()
                .username("player123")
                .email("player@example.com")
                .password("password123")
                .build();

        when(userRepository.existsByUsername("player123")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(UsernameAlreadyExistsException.class)
                .hasMessageContaining("player123");
    }

    @Test
    @DisplayName("Register - Fails when email already exists")
    void register_EmailAlreadyExists() {
        RegisterRequest request = RegisterRequest.builder()
                .username("player123")
                .email("player@example.com")
                .password("password123")
                .build();

        when(userRepository.existsByUsername("player123")).thenReturn(false);
        when(userRepository.existsByEmail("player@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(EmailAlreadyExistsException.class)
                .hasMessageContaining("player@example.com");
    }

    @Test
    @DisplayName("Login - Success")
    void login_Success() {
        LoginRequest request = LoginRequest.builder()
                .email("player@example.com")
                .password("password123")
                .build();

        when(userRepository.findByEmail("player@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("password123", testUser.getPassword())).thenReturn(true);
        when(jwtTokenProvider.generateAccessToken(any(UserPrincipal.class))).thenReturn("mock-access-token");
        when(jwtTokenProvider.getExpirationInSeconds()).thenReturn(86400L);
        when(jwtProperties.getRefreshExpirationMs()).thenReturn(604800000L);
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AuthResponse response = authService.login(request);

        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("mock-access-token");
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        assertThat(response.getExpiresIn()).isEqualTo(86400L);
        assertThat(response.getUser().getUsername()).isEqualTo("player123");
    }

    @Test
    @DisplayName("Login - Fails on incorrect password")
    void login_InvalidPassword() {
        LoginRequest request = LoginRequest.builder()
                .email("player@example.com")
                .password("wrongpassword")
                .build();

        when(userRepository.findByEmail("player@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrongpassword", testUser.getPassword())).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessageContaining("Invalid email or password");
    }

    @Test
    @DisplayName("Refresh Token - Success")
    void refreshToken_Success() {
        RefreshTokenRequest request = RefreshTokenRequest.builder()
                .refreshToken("valid-refresh-token")
                .build();

        RefreshToken refreshToken = RefreshToken.builder()
                .id(UUID.randomUUID())
                .user(testUser)
                .token("valid-refresh-token")
                .expiryDate(Instant.now().plusSeconds(3600))
                .revoked(false)
                .build();

        when(refreshTokenRepository.findByTokenAndRevokedFalse("valid-refresh-token"))
                .thenReturn(Optional.of(refreshToken));
        when(jwtTokenProvider.generateAccessToken(any(UserPrincipal.class))).thenReturn("new-access-token");
        when(jwtTokenProvider.getExpirationInSeconds()).thenReturn(86400L);

        AuthResponse response = authService.refreshToken(request);

        assertThat(response.getAccessToken()).isEqualTo("new-access-token");
        assertThat(response.getRefreshToken()).isEqualTo("valid-refresh-token");
    }

    @Test
    @DisplayName("Refresh Token - Fails when token expired")
    void refreshToken_Expired() {
        RefreshTokenRequest request = RefreshTokenRequest.builder()
                .refreshToken("expired-token")
                .build();

        RefreshToken expiredToken = RefreshToken.builder()
                .id(UUID.randomUUID())
                .user(testUser)
                .token("expired-token")
                .expiryDate(Instant.now().minusSeconds(60))
                .revoked(false)
                .build();

        when(refreshTokenRepository.findByTokenAndRevokedFalse("expired-token"))
                .thenReturn(Optional.of(expiredToken));

        assertThatThrownBy(() -> authService.refreshToken(request))
                .isInstanceOf(TokenRefreshException.class)
                .hasMessageContaining("expired");
    }
}