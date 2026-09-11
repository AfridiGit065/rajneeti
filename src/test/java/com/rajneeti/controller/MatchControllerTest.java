package com.rajneeti.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rajneeti.config.CorsProperties;
import com.rajneeti.config.JwtProperties;
import com.rajneeti.dto.action.ActionRequest;
import com.rajneeti.dto.action.MatchActionResponse;
import com.rajneeti.dto.match.MatchResponse;
import com.rajneeti.entity.enums.CharacterType;
import com.rajneeti.entity.enums.MatchActionType;
import com.rajneeti.entity.enums.MatchStatus;
import com.rajneeti.entity.enums.PendingActionStatus;
import com.rajneeti.exception.BusinessException;
import com.rajneeti.exception.GlobalExceptionHandler;
import com.rajneeti.exception.MatchAlreadyExistsException;
import com.rajneeti.exception.MatchNotFoundException;
import com.rajneeti.exception.NotRoomHostException;
import com.rajneeti.security.JwtAuthenticationEntryPoint;
import com.rajneeti.security.JwtTokenProvider;
import com.rajneeti.security.SecurityConfig;
import com.rajneeti.security.UserPrincipal;
import com.rajneeti.service.MatchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.Collections;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = MatchController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class, JwtAuthenticationEntryPoint.class})
class MatchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private MatchService matchService;

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

    private UserPrincipal testPrincipal;
    private UUID testUserId;
    private UUID testRoomId;
    private UUID testMatchId;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testRoomId = UUID.randomUUID();
        testMatchId = UUID.randomUUID();
        testPrincipal = UserPrincipal.builder()
                .id(testUserId)
                .username("gamer1")
                .email("gamer1@rajneeti.com")
                .password("hashedPassword")
                .build();
    }

    private MatchResponse buildMatchResponse() {
        return MatchResponse.builder()
                .id(testMatchId)
                .roomId(testRoomId)
                .roomCode("RAJ100")
                .status(MatchStatus.CREATED)
                .playerCount(2)
                .players(Collections.emptyList())
                .createdAt(java.time.LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("POST /api/rooms/{roomId}/start - Success")
    void startMatch_Success() throws Exception {
        MatchResponse response = buildMatchResponse();

        when(matchService.startMatch(eq(testRoomId), eq(testUserId))).thenReturn(response);

        mockMvc.perform(post("/api/rooms/" + testRoomId + "/start")
                        .with(user(testPrincipal)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("CREATED"))
                .andExpect(jsonPath("$.data.roomCode").value("RAJ100"));
    }

    @Test
    @DisplayName("POST /api/rooms/{roomId}/start - Fails if not host (403)")
    void startMatch_NotHost() throws Exception {
        when(matchService.startMatch(eq(testRoomId), eq(testUserId)))
                .thenThrow(new NotRoomHostException("Only the room host can start a match."));

        mockMvc.perform(post("/api/rooms/" + testRoomId + "/start")
                        .with(user(testPrincipal)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("NOT_ROOM_HOST"));
    }

    @Test
    @DisplayName("POST /api/rooms/{roomId}/start - Fails if match already exists (409)")
    void startMatch_DuplicateMatch() throws Exception {
        when(matchService.startMatch(eq(testRoomId), eq(testUserId)))
                .thenThrow(new MatchAlreadyExistsException("A match is already in progress or created for this room."));

        mockMvc.perform(post("/api/rooms/" + testRoomId + "/start")
                        .with(user(testPrincipal)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("MATCH_ALREADY_EXISTS"));
    }

    @Test
    @DisplayName("GET /api/rooms/{roomId}/match - Success")
    void getActiveMatch_Success() throws Exception {
        MatchResponse response = buildMatchResponse();

        when(matchService.getActiveMatchByRoom(eq(testRoomId))).thenReturn(response);

        mockMvc.perform(get("/api/rooms/" + testRoomId + "/match")
                        .with(user(testPrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("CREATED"));
    }

    @Test
    @DisplayName("GET /api/rooms/{roomId}/match - Fails if no active match (404)")
    void getActiveMatch_NotFound() throws Exception {
        when(matchService.getActiveMatchByRoom(eq(testRoomId)))
                .thenThrow(new MatchNotFoundException("No active match found for this room."));

        mockMvc.perform(get("/api/rooms/" + testRoomId + "/match")
                        .with(user(testPrincipal)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("MATCH_NOT_FOUND"));
    }

    @Test
    @DisplayName("GET /api/rooms/{roomId}/match/{matchId} - Success")
    void getMatch_Success() throws Exception {
        MatchResponse response = buildMatchResponse();

        when(matchService.getMatch(eq(testMatchId))).thenReturn(response);

        mockMvc.perform(get("/api/rooms/" + testRoomId + "/match/" + testMatchId)
                        .with(user(testPrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(testMatchId.toString()));
    }

    @Test
    @DisplayName("GET /api/rooms/{roomId}/match/{matchId} - Fails if not found (404)")
    void getMatch_NotFound() throws Exception {
        when(matchService.getMatch(eq(testMatchId)))
                .thenThrow(new MatchNotFoundException("Match not found."));

        mockMvc.perform(get("/api/rooms/" + testRoomId + "/match/" + testMatchId)
                        .with(user(testPrincipal)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("MATCH_NOT_FOUND"));
    }

    @Test
    @DisplayName("POST /{roomId}/match/{matchId}/actions - Tax success")
    void performAction_Tax_Success() throws Exception {
        MatchActionResponse response = MatchActionResponse.builder()
                .matchId(testMatchId)
                .roomId(testRoomId)
                .action(MatchActionType.TAX)
                .claimedCharacter(CharacterType.MINISTER)
                .actorUserId(testUserId)
                .actorUsername("gamer1")
                .status(PendingActionStatus.AWAITING_CHALLENGE)
                .coinsToAward(3)
                .currentTurnPlayerId(testUserId)
                .turnNumber(1)
                .createdAt(java.time.LocalDateTime.now())
                .build();

        ActionRequest request = ActionRequest.builder()
                .action(MatchActionType.TAX)
                .claimedCharacter(CharacterType.MINISTER)
                .build();

        when(matchService.performAction(eq(testMatchId), eq(testUserId), eq(request)))
                .thenReturn(response);

        mockMvc.perform(post("/api/rooms/" + testRoomId + "/match/" + testMatchId + "/actions")
                        .with(user(testPrincipal))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.action").value("TAX"))
                .andExpect(jsonPath("$.data.claimedCharacter").value("MINISTER"))
                .andExpect(jsonPath("$.data.status").value("AWAITING_CHALLENGE"));
    }

    @Test
    @DisplayName("POST /{roomId}/match/{matchId}/actions - Not your turn (422)")
    void performAction_NotYourTurn() throws Exception {
        ActionRequest request = ActionRequest.builder()
                .action(MatchActionType.TAX)
                .build();

        when(matchService.performAction(eq(testMatchId), eq(testUserId), eq(request)))
                .thenThrow(new BusinessException("NOT_YOUR_TURN", "It is not your turn."));

        mockMvc.perform(post("/api/rooms/" + testRoomId + "/match/" + testMatchId + "/actions")
                        .with(user(testPrincipal))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("NOT_YOUR_TURN"));
    }

    @Test
    @DisplayName("POST /{roomId}/match/{matchId}/actions - Match not found (404)")
    void performAction_MatchNotFound() throws Exception {
        ActionRequest request = ActionRequest.builder()
                .action(MatchActionType.TAX)
                .build();

        when(matchService.performAction(eq(testMatchId), eq(testUserId), eq(request)))
                .thenThrow(new MatchNotFoundException("Match not found."));

        mockMvc.perform(post("/api/rooms/" + testRoomId + "/match/" + testMatchId + "/actions")
                        .with(user(testPrincipal))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("MATCH_NOT_FOUND"));
    }

    @Test
    @DisplayName("POST /{roomId}/match/{matchId}/actions - Unauthorized (401)")
    void performAction_Unauthorized() throws Exception {
        ActionRequest request = ActionRequest.builder()
                .action(MatchActionType.TAX)
                .build();

        mockMvc.perform(post("/api/rooms/" + testRoomId + "/match/" + testMatchId + "/actions")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }
}
