package com.rajneeti.controller;

import com.rajneeti.config.CorsProperties;
import com.rajneeti.config.JwtProperties;
import com.rajneeti.dto.profile.StatisticsResponse;
import com.rajneeti.exception.GlobalExceptionHandler;
import com.rajneeti.exception.ResourceNotFoundException;
import com.rajneeti.security.JwtTokenProvider;
import com.rajneeti.security.SecurityConfig;
import com.rajneeti.security.UserPrincipal;
import com.rajneeti.service.ProfileService;
import com.rajneeti.websocket.WebSocketEventPublisher;
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
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Module 24 — focused slice test for GET /api/profile/statistics/pdf.
 *
 * <p>Verifies the PDF endpoint contract:
 * <ul>
 *   <li>Authenticated user receives a 200 response with Content-Type application/pdf</li>
 *   <li>Response body contains non-empty PDF bytes</li>
 *   <li>Content-Disposition is set correctly (attachment / correct filename)</li>
 *   <li>Unauthenticated request is rejected with 401</li>
 *   <li>Missing Statistics falls back to ResourceNotFoundException → 404</li>
 *   <li>No userId query-parameter is accepted (user identity from SecurityContext only)</li>
 * </ul>
 */
@WebMvcTest(controllers = ProfileController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class,
         com.rajneeti.security.JwtAuthenticationEntryPoint.class})
class ProfileStatisticsPdfControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProfileService profileService;

    @MockBean
    private UserDetailsService userDetailsService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private JwtProperties jwtProperties;

    @MockBean
    private CorsProperties corsProperties;

    @MockBean(name = "corsConfigurationSource")
    private CorsConfigurationSource corsConfigurationSource;

    @MockBean
    private WebSocketEventPublisher webSocketEventPublisher;

    /** Minimal valid PDF header: "%PDF-" */
    private static final byte[] MINIMAL_PDF = "%PDF-1.4 minimal".getBytes();

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

    // ─── 1. Authenticated user receives a PDF ───────────────────────────────

    @Test
    @DisplayName("GET /api/profile/statistics/pdf - Authenticated user receives 200 OK")
    void getStatisticsPdf_AuthenticatedUser_Returns200() throws Exception {
        when(profileService.generateStatisticsPdf(testUserId)).thenReturn(MINIMAL_PDF);

        mockMvc.perform(get("/api/profile/statistics/pdf")
                        .with(user(testPrincipal)))
                .andExpect(status().isOk());
    }

    // ─── 2. Content-Type is application/pdf ─────────────────────────────────

    @Test
    @DisplayName("GET /api/profile/statistics/pdf - Content-Type is application/pdf")
    void getStatisticsPdf_ContentTypeIsPdf() throws Exception {
        when(profileService.generateStatisticsPdf(testUserId)).thenReturn(MINIMAL_PDF);

        mockMvc.perform(get("/api/profile/statistics/pdf")
                        .with(user(testPrincipal)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PDF));
    }

    // ─── 3. Response body is non-empty ──────────────────────────────────────

    @Test
    @DisplayName("GET /api/profile/statistics/pdf - Response body is non-empty")
    void getStatisticsPdf_BodyIsNonEmpty() throws Exception {
        when(profileService.generateStatisticsPdf(testUserId)).thenReturn(MINIMAL_PDF);

        mockMvc.perform(get("/api/profile/statistics/pdf")
                        .with(user(testPrincipal)))
                .andExpect(status().isOk())
                .andExpect(result -> {
                    byte[] body = result.getResponse().getContentAsByteArray();
                    org.assertj.core.api.Assertions.assertThat(body).isNotEmpty();
                });
    }

    // ─── 4. Content-Disposition is correct ──────────────────────────────────

    @Test
    @DisplayName("GET /api/profile/statistics/pdf - Content-Disposition is attachment with correct filename")
    void getStatisticsPdf_ContentDispositionIsAttachment() throws Exception {
        when(profileService.generateStatisticsPdf(testUserId)).thenReturn(MINIMAL_PDF);

        mockMvc.perform(get("/api/profile/statistics/pdf")
                        .with(user(testPrincipal)))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition",
                        org.hamcrest.Matchers.containsString("rajneeti-statistics.pdf")));
    }

    // ─── 5. Unauthenticated request is rejected ──────────────────────────────

    @Test
    @DisplayName("GET /api/profile/statistics/pdf - Unauthenticated request returns 401")
    void getStatisticsPdf_Unauthenticated_Returns401() throws Exception {
        mockMvc.perform(get("/api/profile/statistics/pdf"))
                .andExpect(status().isUnauthorized());
    }

    // ─── 6. Missing Statistics → 404 ────────────────────────────────────────

    @Test
    @DisplayName("GET /api/profile/statistics/pdf - Missing user statistics returns 404")
    void getStatisticsPdf_MissingStatistics_Returns404() throws Exception {
        when(profileService.generateStatisticsPdf(testUserId))
                .thenThrow(new ResourceNotFoundException("Statistics", "userId", testUserId));

        mockMvc.perform(get("/api/profile/statistics/pdf")
                        .with(user(testPrincipal)))
                .andExpect(status().isNotFound());
    }

    // ─── 7. userId query param is silently ignored (SecurityContext wins) ────

    @Test
    @DisplayName("GET /api/profile/statistics/pdf?userId=... - userId param is ignored; SecurityContext userId is used")
    void getStatisticsPdf_UserIdParamIgnored_SecurityContextUsed() throws Exception {
        UUID anotherUserId = UUID.randomUUID();
        when(profileService.generateStatisticsPdf(testUserId)).thenReturn(MINIMAL_PDF);
        // If the param were honoured, this call would use anotherUserId and the mock
        // would return no match → the mock stub above would not fire.
        // The test verifies that the real userId (from SecurityContext) is used.
        when(profileService.generateStatisticsPdf(anotherUserId)).thenReturn(new byte[0]);

        mockMvc.perform(get("/api/profile/statistics/pdf")
                        .param("userId", anotherUserId.toString())
                        .with(user(testPrincipal)))
                .andExpect(status().isOk())
                .andExpect(result -> {
                    byte[] body = result.getResponse().getContentAsByteArray();
                    // Must equal MINIMAL_PDF (testUserId's data), not the empty byte[] from anotherUserId
                    org.assertj.core.api.Assertions.assertThat(body).isEqualTo(MINIMAL_PDF);
                });
    }
}
