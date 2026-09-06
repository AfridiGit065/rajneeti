package com.rajneeti.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rajneeti.config.CorsProperties;
import com.rajneeti.config.JwtProperties;
import com.rajneeti.dto.profile.ProfileResponse;
import com.rajneeti.dto.profile.StatisticsResponse;
import com.rajneeti.dto.profile.UpdateProfileRequest;
import com.rajneeti.exception.GlobalExceptionHandler;
import com.rajneeti.exception.UsernameAlreadyExistsException;
import com.rajneeti.security.JwtAuthenticationEntryPoint;
import com.rajneeti.security.JwtAuthenticationFilter;
import com.rajneeti.security.JwtTokenProvider;
import com.rajneeti.security.SecurityConfig;
import com.rajneeti.security.UserPrincipal;
import com.rajneeti.service.ProfileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ProfileController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class ProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProfileService profileService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @MockBean
    private UserDetailsService userDetailsService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private JwtProperties jwtProperties;

    @MockBean
    private CorsProperties corsProperties;

    @MockBean
    private CorsConfigurationSource corsConfigurationSource;

    private UserPrincipal testPrincipal;
    private UUID testUserId;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testPrincipal = UserPrincipal.builder()
                .id(testUserId)
                .username("player123")
                .email("player@rajneeti.com")
                .password("hashedPassword")
                .build();
    }

    @Test
    @DisplayName("GET /api/profile - Unauthorized without authentication")
    void getProfile_Unauthorized() throws Exception {
        mockMvc.perform(get("/api/profile"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/profile - Success with authenticated user")
    void getProfile_Success() throws Exception {
        ProfileResponse response = ProfileResponse.builder()
                .id(testUserId)
                .username("player123")
                .email("player@rajneeti.com")
                .rating(1000)
                .totalMatches(5)
                .wins(3)
                .losses(2)
                .winRate(60.0)
                .build();

        when(profileService.getProfile(testUserId)).thenReturn(response);

        mockMvc.perform(get("/api/profile")
                        .with(user(testPrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.username").value("player123"))
                .andExpect(jsonPath("$.data.rating").value(1000))
                .andExpect(jsonPath("$.data.winRate").value(60.0));
    }

    @Test
    @DisplayName("PUT /api/profile - Success")
    void updateProfile_Success() throws Exception {
        UpdateProfileRequest request = UpdateProfileRequest.builder()
                .username("newPlayer")
                .avatarUrl("https://example.com/avatar.png")
                .build();

        ProfileResponse response = ProfileResponse.builder()
                .id(testUserId)
                .username("newPlayer")
                .email("player@rajneeti.com")
                .avatarUrl("https://example.com/avatar.png")
                .rating(1000)
                .build();

        when(profileService.updateProfile(eq(testUserId), any(UpdateProfileRequest.class)))
                .thenReturn(response);

        mockMvc.perform(put("/api/profile")
                        .with(user(testPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.username").value("newPlayer"));
    }

    @Test
    @DisplayName("PUT /api/profile - Fails on duplicate username (409 Conflict)")
    void updateProfile_DuplicateUsername() throws Exception {
        UpdateProfileRequest request = UpdateProfileRequest.builder()
                .username("takenUser")
                .build();

        when(profileService.updateProfile(eq(testUserId), any(UpdateProfileRequest.class)))
                .thenThrow(new UsernameAlreadyExistsException("Username 'takenUser' is already taken."));

        mockMvc.perform(put("/api/profile")
                        .with(user(testPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("USERNAME_ALREADY_EXISTS"));
    }

    @Test
    @DisplayName("GET /api/profile/statistics - Success")
    void getStatistics_Success() throws Exception {
        StatisticsResponse stats = StatisticsResponse.builder()
                .userId(testUserId)
                .username("player123")
                .rating(1000)
                .totalMatches(10)
                .wins(6)
                .losses(4)
                .winRate(60.0)
                .totalCoinsEarned(1200L)
                .totalCoinsSpent(400L)
                .build();

        when(profileService.getStatistics(testUserId)).thenReturn(stats);

        mockMvc.perform(get("/api/profile/statistics")
                        .with(user(testPrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.rating").value(1000))
                .andExpect(jsonPath("$.data.totalCoinsEarned").value(1200));
    }
}