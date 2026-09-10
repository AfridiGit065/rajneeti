package com.rajneeti.controller;

import com.rajneeti.config.CorsProperties;
import com.rajneeti.config.JwtProperties;
import com.rajneeti.dto.game.GamePlayerDto;
import com.rajneeti.dto.game.GameStateResponse;
import com.rajneeti.dto.game.PendingActionDto;
import com.rajneeti.entity.enums.MatchStatus;
import com.rajneeti.entity.enums.PlayerStatus;
import com.rajneeti.exception.BusinessException;
import com.rajneeti.exception.GlobalExceptionHandler;
import com.rajneeti.exception.MatchNotFoundException;
import com.rajneeti.game.GameEngine;
import com.rajneeti.security.JwtAuthenticationEntryPoint;
import com.rajneeti.security.JwtTokenProvider;
import com.rajneeti.security.SecurityConfig;
import com.rajneeti.security.UserPrincipal;
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

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = GameController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class GameControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GameEngine gameEngine;

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

    @MockBean(name = "corsConfigurationSource")
    private CorsConfigurationSource corsConfigurationSource;

    private UserPrincipal testPrincipal;
    private UUID testUserId;
    private UUID testMatchId;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testMatchId = UUID.randomUUID();
        testPrincipal = UserPrincipal.builder()
                .id(testUserId)
                .username("gamer1")
                .email("gamer1@rajneeti.com")
                .password("hashedPassword")
                .build();
    }

    private GameStateResponse buildGameResponse() {
        return GameStateResponse.builder()
                .matchId(testMatchId)
                .roomId(UUID.randomUUID())
                .roomCode("RAJ200")
                .status(MatchStatus.CREATED)
                .phase("setup")
                .hostUserId(testUserId)
                .players(List.of(GamePlayerDto.builder()
                        .userId(testUserId)
                        .username("gamer1")
                        .status(PlayerStatus.ACTIVE)
                        .coins(2)
                        .influenceCount(2)
                        .alive(true)
                        .turn(true)
                        .build()))
                .turnNumber(1)
                .deckCount(13)
                .revealedCardsCount(0)
                .log(List.of())
                .build();
    }

    @Test
    @DisplayName("GET /api/matches/{matchId}/game - Success")
    void getGame_Success() throws Exception {
        GameStateResponse response = buildGameResponse();

        when(gameEngine.getSafeGameState(eq(testMatchId), eq(testUserId))).thenReturn(response);

        mockMvc.perform(get("/api/matches/" + testMatchId + "/game")
                        .with(user(testPrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.matchId").value(testMatchId.toString()))
                .andExpect(jsonPath("$.data.status").value("CREATED"))
                .andExpect(jsonPath("$.data.players[0].username").value("gamer1"))
                .andExpect(jsonPath("$.data.players[0].influenceCount").value(2))
                .andExpect(jsonPath("$.data.deckCount").value(13));
    }

    @Test
    @DisplayName("GET /api/matches/{matchId}/game - Fails for unknown match (404)")
    void getGame_NotFound() throws Exception {
        when(gameEngine.getSafeGameState(eq(testMatchId), eq(testUserId)))
                .thenThrow(new MatchNotFoundException("Match with ID '" + testMatchId + "' not found."));

        mockMvc.perform(get("/api/matches/" + testMatchId + "/game")
                        .with(user(testPrincipal)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("MATCH_NOT_FOUND"));
    }

    @Test
    @DisplayName("GET /api/matches/{matchId}/game - Fails when state cannot be initialized (422)")
    void getGame_NotInitializable() throws Exception {
        when(gameEngine.getSafeGameState(eq(testMatchId), eq(testUserId)))
                .thenThrow(new BusinessException("NO_PLAYERS", "Cannot initialize game state."));

        mockMvc.perform(get("/api/matches/" + testMatchId + "/game")
                        .with(user(testPrincipal)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("NO_PLAYERS"));
    }

    @Test
    @DisplayName("POST /api/matches/{matchId}/income - Success")
    void performIncome_Success() throws Exception {
        GameStateResponse response = buildGameResponse();
        response.getPlayers().get(0).setCoins(3);

        when(gameEngine.performIncome(eq(testMatchId), eq(testUserId))).thenReturn(response);

        mockMvc.perform(post("/api/matches/" + testMatchId + "/income")
                        .with(user(testPrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.matchId").value(testMatchId.toString()))
                .andExpect(jsonPath("$.data.players[0].coins").value(3))
                .andExpect(jsonPath("$.data.turnNumber").value(1));
    }

    @Test
    @DisplayName("POST /api/matches/{matchId}/income - Fails when it is not the player's turn (422)")
    void performIncome_NotYourTurn() throws Exception {
        when(gameEngine.performIncome(eq(testMatchId), eq(testUserId)))
                .thenThrow(new BusinessException("NOT_YOUR_TURN", "It is not your turn."));

        mockMvc.perform(post("/api/matches/" + testMatchId + "/income")
                        .with(user(testPrincipal)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("NOT_YOUR_TURN"));
    }

    @Test
    @DisplayName("POST /api/matches/{matchId}/foreign-aid - Success (opens block window)")
    void performForeignAid_Success() throws Exception {
        GameStateResponse response = buildGameResponse();
        response.setPendingAction(PendingActionDto.builder()
                .type("FOREIGN_AID")
                .actorUserId(testUserId)
                .startedAt(java.time.LocalDateTime.now())
                .build());

        when(gameEngine.performForeignAid(eq(testMatchId), eq(testUserId))).thenReturn(response);

        mockMvc.perform(post("/api/matches/" + testMatchId + "/foreign-aid")
                        .with(user(testPrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.pendingAction.type").value("FOREIGN_AID"))
                .andExpect(jsonPath("$.data.pendingAction.actorUserId").value(testUserId.toString()));
    }

    @Test
    @DisplayName("POST /api/matches/{matchId}/foreign-aid - Fails when not your turn (422)")
    void performForeignAid_NotYourTurn() throws Exception {
        when(gameEngine.performForeignAid(eq(testMatchId), eq(testUserId)))
                .thenThrow(new BusinessException("NOT_YOUR_TURN", "It is not your turn."));

        mockMvc.perform(post("/api/matches/" + testMatchId + "/foreign-aid")
                        .with(user(testPrincipal)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("NOT_YOUR_TURN"));
    }

    @Test
    @DisplayName("POST /api/matches/{matchId}/foreign-aid/resolve - Success")
    void resolveForeignAid_Success() throws Exception {
        GameStateResponse response = buildGameResponse();
        response.getPlayers().get(0).setCoins(4);
        response.setTurnNumber(2);

        when(gameEngine.resolveForeignAid(eq(testMatchId), eq(testUserId), eq(false)))
                .thenReturn(response);

        mockMvc.perform(post("/api/matches/" + testMatchId + "/foreign-aid/resolve?blocked=false")
                        .with(user(testPrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.players[0].coins").value(4))
                .andExpect(jsonPath("$.data.pendingAction").doesNotExist());
    }
}