package com.rajneeti.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rajneeti.config.CorsProperties;
import com.rajneeti.config.JwtProperties;
import com.rajneeti.dto.room.CreateRoomRequest;
import com.rajneeti.dto.room.JoinRoomRequest;
import com.rajneeti.dto.room.RoomResponse;
import com.rajneeti.entity.enums.RoomStatus;
import com.rajneeti.exception.GlobalExceptionHandler;
import com.rajneeti.exception.NotRoomHostException;
import com.rajneeti.exception.RoomNotFoundException;
import com.rajneeti.security.JwtAuthenticationEntryPoint;
import com.rajneeti.security.JwtTokenProvider;
import com.rajneeti.security.SecurityConfig;
import com.rajneeti.security.UserPrincipal;
import com.rajneeti.service.RoomService;
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

import java.util.Collections;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = RoomController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class RoomControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RoomService roomService;

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
    private UUID testRoomId;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testRoomId = UUID.randomUUID();
        testPrincipal = UserPrincipal.builder()
                .id(testUserId)
                .username("gamer1")
                .email("gamer1@rajneeti.com")
                .password("hashedPassword")
                .build();
    }

    @Test
    @DisplayName("POST /api/rooms - Success")
    void createRoom_Success() throws Exception {
        CreateRoomRequest request = new CreateRoomRequest(6);

        RoomResponse response = RoomResponse.builder()
                .id(testRoomId)
                .roomCode("RAJ100")
                .hostId(testUserId)
                .hostUsername("gamer1")
                .status(RoomStatus.WAITING)
                .maxPlayers(6)
                .currentPlayers(1)
                .canStart(false)
                .players(Collections.emptyList())
                .build();

        when(roomService.createRoom(eq(testUserId), any(CreateRoomRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/rooms")
                        .with(user(testPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.roomCode").value("RAJ100"));
    }

    @Test
    @DisplayName("POST /api/rooms/join - Success")
    void joinRoom_Success() throws Exception {
        JoinRoomRequest request = new JoinRoomRequest("RAJ100");

        RoomResponse response = RoomResponse.builder()
                .id(testRoomId)
                .roomCode("RAJ100")
                .status(RoomStatus.WAITING)
                .currentPlayers(2)
                .build();

        when(roomService.joinRoom(eq(testUserId), any(JoinRoomRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/rooms/join")
                        .with(user(testPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.roomCode").value("RAJ100"));
    }

    @Test
    @DisplayName("DELETE /api/rooms/{roomId} - Fails if not host (403)")
    void deleteRoom_NotHost() throws Exception {
        doThrow(new NotRoomHostException("Only host can delete"))
                .when(roomService).deleteRoom(eq(testRoomId), eq(testUserId));

        mockMvc.perform(delete("/api/rooms/" + testRoomId)
                        .with(user(testPrincipal)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("NOT_ROOM_HOST"));
    }

    @Test
    @DisplayName("GET /api/rooms/{roomId} - Fails if not found (404)")
    void getRoom_NotFound() throws Exception {
        when(roomService.getRoom(testRoomId))
                .thenThrow(new RoomNotFoundException("Room not found"));

        mockMvc.perform(get("/api/rooms/" + testRoomId)
                        .with(user(testPrincipal)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("RESOURCE_NOT_FOUND"));
    }
}
