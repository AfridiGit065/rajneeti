package com.rajneeti.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rajneeti.dto.auth.LoginRequest;
import com.rajneeti.dto.auth.RegisterRequest;
import com.rajneeti.dto.room.CreateRoomRequest;
import com.rajneeti.dto.room.JoinRoomRequest;
import com.rajneeti.entity.Room;
import com.rajneeti.entity.RoomPlayer;
import com.rajneeti.entity.enums.RoomStatus;
import com.rajneeti.repository.RoomPlayerRepository;
import com.rajneeti.repository.RoomRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Module 06 — Ready System end-to-end tests.
 *
 * <p>Unlike {@code RoomControllerTest} (MockMvc slice with a mocked service) and
 * {@code RoomServiceTest} (Mockito unit test), this boots the real application
 * on an in-memory H2 database and drives the very same REST endpoints the
 * frontend calls, with real JWTs issued by the real auth endpoints. It therefore
 * covers the whole Module 06 contract end to end: authentication, room
 * membership, room validity, persistence, and the "only before the match
 * starts" rule.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:readytestdb;DB_CLOSE_DELAY=-1;MODE=MySQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "jwt.secret=dGVzdC1zZWNyZXQtdGhhdC1pcy1sb25nLWVub3VnaC1mb3ItaG1hYy1zaGEyNTY=",
        "logging.level.root=WARN",
        "logging.level.com.rajneeti=WARN"
})
class RoomReadyIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private RoomPlayerRepository roomPlayerRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    /* ------------------------------------------------------------------ */
    /*  Ready state defaults                                                */
    /* ------------------------------------------------------------------ */

    @Test
    @DisplayName("The host joins a new room as NOT_READY")
    void roomCreatorJoinsAsNotReady() throws Exception {
        Player host = registerAndLogin("ready-host");
        JsonNode room = createRoom(host.token(), 6);

        assertThat(room.at("/players/0/ready").asBoolean()).isFalse();
        assertThat(persistedReady(roomId(room), host.userId())).isFalse();
    }

    @Test
    @DisplayName("A player joining an existing room joins as NOT_READY")
    void joiningPlayerIsNotReady() throws Exception {
        Player host = registerAndLogin("ready-join-host");
        Player guest = registerAndLogin("ready-join-guest");
        JsonNode room = createRoom(host.token(), 6);

        MvcResult result = mockMvc.perform(post("/api/rooms/join")
                        .header("Authorization", bearer(guest.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                JoinRoomRequest.builder().roomCode(roomCode(room)).build())))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode joined = dataOf(result);
        assertThat(joined.at("/players/1/ready").asBoolean()).isFalse();
        assertThat(persistedReady(roomId(room), guest.userId())).isFalse();
    }

    /* ------------------------------------------------------------------ */
    /*  Ready / Unready                                                     */
    /* ------------------------------------------------------------------ */

    @Test
    @DisplayName("Ready marks the caller READY and the state persists")
    void readyMarksCallerReadyAndPersists() throws Exception {
        Player host = registerAndLogin("ready-up-host");
        Player guest = registerAndLogin("ready-up-guest");
        JsonNode room = createRoomWithGuest(host, guest);
        UUID roomId = roomId(room);

        MvcResult result = mockMvc.perform(post("/api/rooms/" + roomId + "/ready")
                        .header("Authorization", bearer(guest.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        // The response reflects the caller's new state…
        assertThat(readyOf(dataOf(result), guest.userId())).isTrue();
        // …and so does a completely separate database transaction.
        assertThat(persistedReady(roomId, guest.userId())).isTrue();
    }

    @Test
    @DisplayName("Unready marks the caller NOT_READY and the state persists")
    void unreadyMarksCallerNotReadyAndPersists() throws Exception {
        Player host = registerAndLogin("unready-host");
        Player guest = registerAndLogin("unready-guest");
        JsonNode room = createRoomWithGuest(host, guest);
        UUID roomId = roomId(room);

        mockMvc.perform(post("/api/rooms/" + roomId + "/ready")
                        .header("Authorization", bearer(guest.token())))
                .andExpect(status().isOk());
        assertThat(persistedReady(roomId, guest.userId())).isTrue();

        MvcResult result = mockMvc.perform(post("/api/rooms/" + roomId + "/unready")
                        .header("Authorization", bearer(guest.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        assertThat(readyOf(dataOf(result), guest.userId())).isFalse();
        assertThat(persistedReady(roomId, guest.userId())).isFalse();
    }

    @Test
    @DisplayName("Ready state is independent per player and visible to everyone in the room")
    void readyStateIsPerPlayerAndVisibleToEveryone() throws Exception {
        Player host = registerAndLogin("per-player-host");
        Player guest = registerAndLogin("per-player-guest");
        JsonNode room = createRoomWithGuest(host, guest);
        UUID roomId = roomId(room);

        mockMvc.perform(post("/api/rooms/" + roomId + "/ready")
                        .header("Authorization", bearer(host.token())))
                .andExpect(status().isOk());

        // The guest re-reads the room: the host is READY, the guest is still NOT_READY.
        MvcResult read = mockMvc.perform(get("/api/rooms/" + roomId)
                        .header("Authorization", bearer(guest.token())))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode snapshot = dataOf(read);
        assertThat(readyOf(snapshot, host.userId())).isTrue();
        assertThat(readyOf(snapshot, guest.userId())).isFalse();
        assertThat(snapshot.at("/canStart").asBoolean()).isFalse();

        // Once everybody is READY the room reports that it can start.
        mockMvc.perform(post("/api/rooms/" + roomId + "/ready")
                        .header("Authorization", bearer(guest.token())))
                .andExpect(status().isOk());

        MvcResult allReady = mockMvc.perform(get("/api/rooms/" + roomId)
                        .header("Authorization", bearer(guest.token())))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(dataOf(allReady).at("/canStart").asBoolean()).isTrue();
    }

    @Test
    @DisplayName("Ready survives a player leaving and rejoining as NOT_READY")
    void rejoiningPlayerIsNotReadyAgain() throws Exception {
        Player host = registerAndLogin("rejoin-host");
        Player guest = registerAndLogin("rejoin-guest");
        JsonNode room = createRoomWithGuest(host, guest);
        UUID roomId = roomId(room);

        mockMvc.perform(post("/api/rooms/" + roomId + "/ready")
                        .header("Authorization", bearer(guest.token())))
                .andExpect(status().isOk());
        assertThat(persistedReady(roomId, guest.userId())).isTrue();

        mockMvc.perform(post("/api/rooms/" + roomId + "/leave")
                        .header("Authorization", bearer(guest.token())))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/rooms/join")
                        .header("Authorization", bearer(guest.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                JoinRoomRequest.builder().roomCode(roomCode(room)).build())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.players[1].ready").value(false));

        assertThat(persistedReady(roomId, guest.userId())).isFalse();
    }

    /* ------------------------------------------------------------------ */
    /*  Rejections                                                          */
    /* ------------------------------------------------------------------ */

    @Test
    @DisplayName("A player outside the room cannot Ready")
    void nonRoomPlayerCannotReady() throws Exception {
        Player host = registerAndLogin("outside-ready-host");
        Player outsider = registerAndLogin("outside-ready-guest");
        UUID roomId = roomId(createRoom(host.token(), 6));

        mockMvc.perform(post("/api/rooms/" + roomId + "/ready")
                        .header("Authorization", bearer(outsider.token())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("NOT_IN_ROOM"));
    }

    @Test
    @DisplayName("A player outside the room cannot Unready")
    void nonRoomPlayerCannotUnready() throws Exception {
        Player host = registerAndLogin("outside-unready-host");
        Player outsider = registerAndLogin("outside-unready-guest");
        UUID roomId = roomId(createRoom(host.token(), 6));

        mockMvc.perform(post("/api/rooms/" + roomId + "/unready")
                        .header("Authorization", bearer(outsider.token())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("NOT_IN_ROOM"));
    }

    @Test
    @DisplayName("A rejected request never changes anybody's ready state")
    void rejectedRequestLeavesReadyStateUntouched() throws Exception {
        Player host = registerAndLogin("untouched-host");
        Player outsider = registerAndLogin("untouched-outsider");
        JsonNode room = createRoom(host.token(), 6);
        UUID roomId = roomId(room);

        mockMvc.perform(post("/api/rooms/" + roomId + "/ready")
                        .header("Authorization", bearer(host.token())))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/rooms/" + roomId + "/unready")
                        .header("Authorization", bearer(outsider.token())))
                .andExpect(status().isBadRequest());

        assertThat(persistedReady(roomId, host.userId())).isTrue();
    }

    @Test
    @DisplayName("An unauthenticated Ready request is rejected")
    void unauthenticatedReadyIsRejected() throws Exception {
        Player host = registerAndLogin("unauth-ready-host");
        UUID roomId = roomId(createRoom(host.token(), 6));

        mockMvc.perform(post("/api/rooms/" + roomId + "/ready"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("An unauthenticated Unready request is rejected")
    void unauthenticatedUnreadyIsRejected() throws Exception {
        Player host = registerAndLogin("unauth-unready-host");
        UUID roomId = roomId(createRoom(host.token(), 6));

        mockMvc.perform(post("/api/rooms/" + roomId + "/unready"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("A request with a bogus bearer token is rejected")
    void bogusTokenIsRejected() throws Exception {
        Player host = registerAndLogin("bogus-host");
        UUID roomId = roomId(createRoom(host.token(), 6));

        mockMvc.perform(post("/api/rooms/" + roomId + "/ready")
                        .header("Authorization", "Bearer not-a-real-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Ready against an unknown room is rejected as 404")
    void readyWithUnknownRoomIsRejected() throws Exception {
        Player player = registerAndLogin("unknown-room-ready");

        mockMvc.perform(post("/api/rooms/" + UUID.randomUUID() + "/ready")
                        .header("Authorization", bearer(player.token())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    @DisplayName("Unready against an unknown room is rejected as 404")
    void unreadyWithUnknownRoomIsRejected() throws Exception {
        Player player = registerAndLogin("unknown-room-unready");

        mockMvc.perform(post("/api/rooms/" + UUID.randomUUID() + "/unready")
                        .header("Authorization", bearer(player.token())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    @DisplayName("Ready with a malformed room id is rejected as 400")
    void readyWithMalformedRoomIdIsRejected() throws Exception {
        Player player = registerAndLogin("malformed-room");

        mockMvc.perform(post("/api/rooms/not-a-uuid/ready")
                        .header("Authorization", bearer(player.token())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("TYPE_MISMATCH"));
    }

    @Test
    @DisplayName("Ready is rejected once the match has started")
    void readyIsRejectedAfterMatchStarted() throws Exception {
        Player host = registerAndLogin("in-game-host");
        UUID roomId = roomId(createRoom(host.token(), 6));
        moveRoomTo(roomId, RoomStatus.IN_GAME);

        mockMvc.perform(post("/api/rooms/" + roomId + "/ready")
                        .header("Authorization", bearer(host.token())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_ROOM_STATE"));
    }

    @Test
    @DisplayName("Unready is rejected once the room is no longer WAITING")
    void unreadyIsRejectedAfterRoomFinished() throws Exception {
        Player host = registerAndLogin("finished-host");
        UUID roomId = roomId(createRoom(host.token(), 6));
        moveRoomTo(roomId, RoomStatus.FINISHED);

        mockMvc.perform(post("/api/rooms/" + roomId + "/unready")
                        .header("Authorization", bearer(host.token())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_ROOM_STATE"));
    }

    /* ------------------------------------------------------------------ */
    /*  Phase 1 regression                                                  */
    /* ------------------------------------------------------------------ */

    @Test
    @DisplayName("Phase 1 room lifecycle still works: create, list, read, ready, leave, cancel")
    void phaseOneRoomLifecycleStillWorks() throws Exception {
        Player host = registerAndLogin("phase1-host");
        Player guest = registerAndLogin("phase1-guest");

        MvcResult created = mockMvc.perform(post("/api/rooms")
                        .header("Authorization", bearer(host.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                CreateRoomRequest.builder().name("Legacy Table").maxPlayers(4).build())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("WAITING"))
                .andExpect(jsonPath("$.data.currentPlayers").value(1))
                .andReturn();

        JsonNode room = dataOf(created);
        UUID roomId = roomId(room);
        assertThat(room.at("/name").asText()).isEqualTo("Legacy Table");
        assertThat(room.at("/players/0/isHost").asBoolean()).isTrue();

        // The room shows up in the joinable list.
        mockMvc.perform(get("/api/rooms").header("Authorization", bearer(guest.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.id == '" + roomId + "')]").exists());

        // A guest joins by code and is not ready yet.
        mockMvc.perform(post("/api/rooms/join")
                        .header("Authorization", bearer(guest.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                JoinRoomRequest.builder().roomCode(roomCode(room)).build())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.currentPlayers").value(2))
                .andExpect(jsonPath("$.data.players[1].ready").value(false));

        // Module 06 sits on top of that lifecycle without changing it.
        mockMvc.perform(post("/api/rooms/" + roomId + "/ready")
                        .header("Authorization", bearer(host.token())))
                .andExpect(status().isOk());

        // Leaving hands the host role to the remaining player.
        mockMvc.perform(post("/api/rooms/" + roomId + "/leave")
                        .header("Authorization", bearer(host.token())))
                .andExpect(status().isOk());

        MvcResult afterLeave = mockMvc.perform(get("/api/rooms/" + roomId)
                        .header("Authorization", bearer(guest.token())))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(dataOf(afterLeave).at("/hostId").asText()).isEqualTo(guest.userId().toString());
        assertThat(dataOf(afterLeave).at("/players").size()).isEqualTo(1);

        // The new host can cancel the room.
        mockMvc.perform(delete("/api/rooms/" + roomId)
                        .header("Authorization", bearer(guest.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        MvcResult cancelled = mockMvc.perform(get("/api/rooms/" + roomId)
                        .header("Authorization", bearer(guest.token())))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(dataOf(cancelled).at("/status").asText()).isEqualTo("CANCELLED");
    }

    /* ------------------------------------------------------------------ */
    /*  Helpers                                                             */
    /* ------------------------------------------------------------------ */

    /** A real, JWT-authenticated player obtained through the public auth API. */
    private record Player(String userId, String token) {
    }

    private Player registerAndLogin(String prefix) throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String username = prefix + "-" + suffix;
        String email = username + "@rajneeti.test";
        String password = "readyPass123";

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                RegisterRequest.builder()
                                        .username(username)
                                        .email(email)
                                        .password(password)
                                        .build())))
                .andExpect(status().isCreated());

        MvcResult login = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                LoginRequest.builder()
                                        .email(email)
                                        .password(password)
                                        .build())))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode data = dataOf(login);
        return new Player(data.at("/user/id").asText(), data.at("/accessToken").asText());
    }

    private JsonNode createRoom(String token, int maxPlayers) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/rooms")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                CreateRoomRequest.builder().maxPlayers(maxPlayers).build())))
                .andExpect(status().isCreated())
                .andReturn();
        return dataOf(result);
    }

    /** Creates a room and seats a second, human player in it. */
    private JsonNode createRoomWithGuest(Player host, Player guest) throws Exception {
        JsonNode room = createRoom(host.token(), 6);
        mockMvc.perform(post("/api/rooms/join")
                        .header("Authorization", bearer(guest.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                JoinRoomRequest.builder().roomCode(roomCode(room)).build())))
                .andExpect(status().isOk());
        return room;
    }

    /** Forces a room into a given lifecycle status, standing in for a match start. */
    private void moveRoomTo(UUID roomId, RoomStatus status) {
        new TransactionTemplate(transactionManager).executeWithoutResult(tx -> {
            Room room = roomRepository.findById(roomId).orElseThrow();
            room.setStatus(status);
            roomRepository.save(room);
        });
    }

    /**
     * Reads the ready flag in a brand-new transaction, so the value comes from
     * the database and never from a first-level cache of the writing request.
     */
    private boolean persistedReady(UUID roomId, String userId) {
        return new TransactionTemplate(transactionManager)
                .execute(tx -> roomPlayerRepository
                        .findByRoomIdAndUserId(roomId, UUID.fromString(userId))
                        .map(RoomPlayer::getReady)
                        .orElse(null)) == Boolean.TRUE;
    }

    private static boolean readyOf(JsonNode room, String userId) {
        for (JsonNode player : room.at("/players")) {
            if (userId.equals(player.path("id").asText())) {
                return player.path("ready").asBoolean();
            }
        }
        throw new AssertionError("Player " + userId + " is not present in the room payload.");
    }

    private static UUID roomId(JsonNode room) {
        return UUID.fromString(room.get("id").asText());
    }

    private static String roomCode(JsonNode room) {
        return room.get("roomCode").asText();
    }

    private JsonNode dataOf(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("data");
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }
}
