package com.rajneeti.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rajneeti.dto.match.MatchResponse;
import com.rajneeti.dto.room.CreateRoomRequest;
import com.rajneeti.dto.room.JoinRoomRequest;
import com.rajneeti.dto.room.RoomResponse;
import com.rajneeti.entity.User;
import com.rajneeti.game.ActionResolver;
import com.rajneeti.game.BlockManager;
import com.rajneeti.game.ChallengeManager;
import com.rajneeti.game.GameCard;
import com.rajneeti.game.GameEngine;
import com.rajneeti.game.GameStore;
import com.rajneeti.repository.UserRepository;
import com.rajneeti.security.JwtTokenProvider;
import com.rajneeti.service.MatchService;
import com.rajneeti.service.RoomService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Module 22 — end-to-end WebSocket integration tests.
 *
 * <p>Boots the real application on a random port and drives it with a real
 * STOMP client over SockJS, exactly like the frontend will. Each scenario
 * exercises the JWT-authenticated CONNECT, the subscription authorization
 * policy, the broadcast channels for room/match topics and the private
 * per-user queue, without any Mockito stubs.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:wstestdb;DB_CLOSE_DELAY=-1;MODE=MySQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "jwt.secret=dGVzdC1zZWNyZXQtdGhhdC1pcy1sb25nLWVub3VnaC1mb3ItaG1hYy1zaGEyNTY=",
        "websocket.allowed-origins=http://localhost:3000,http://localhost:3001",
        "logging.level.root=WARN",
        "logging.level.com.rajneeti=INFO"
})
class WebSocketIntegrationTest {

    private static final String ALLOWED_ORIGIN = "http://localhost:3000";

    @LocalServerPort
    private int port;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoomService roomService;

    @Autowired
    private MatchService matchService;

    @Autowired
    private GameEngine gameEngine;

    @Autowired
    private ChallengeManager challengeManager;

    @Autowired
    private BlockManager blockManager;

    @Autowired
    private ActionResolver actionResolver;

    @Autowired
    private GameStore gameStore;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private ObjectMapper objectMapper;

    private WebSocketStompClient client;

    private final List<StompSession> openSessions = Collections.synchronizedList(new ArrayList<>());
    private UUID matchId;

    @BeforeEach
    void setUp() {
        SockJsClient sockJsClient = new SockJsClient(
                List.of(new WebSocketTransport(new StandardWebSocketClient())));
        client = new WebSocketStompClient(sockJsClient);
        client.setMessageConverter(new MappingJackson2MessageConverter(objectMapper));
        // No heartbeats: the tests are short and no TaskScheduler is needed.
        client.setDefaultHeartbeat(new long[]{0, 0});
    }

    @AfterEach
    void cleanup() throws Exception {
        for (StompSession session : openSessions) {
            if (session != null && session.isConnected()) {
                session.disconnect();
            }
        }
        openSessions.clear();
        if (matchId != null) {
            gameStore.remove(matchId);
        }
        if (client != null) {
            client.stop();
        }
    }

    /* ------------------------------------------------------------------ */
    /*  Authentication                                                     */
    /* ------------------------------------------------------------------ */

    @Test
    @DisplayName("CONNECT without a JWT is rejected")
    void connectWithoutTokenIsRejected() {
        StompHeaders connectHeaders = new StompHeaders();

        CompletableFuture<StompSession> future = client.connectAsync(wsUrl(), handshakeHeaders(),
                connectHeaders, new ConnectionResult());
        assertThatThrownBy(() -> future.get(5, TimeUnit.SECONDS))
                .isInstanceOf(ExecutionException.class);
    }

    @Test
    @DisplayName("CONNECT with an invalid JWT is rejected")
    void connectWithInvalidTokenIsRejected() {
        StompHeaders connectHeaders = new StompHeaders();
        connectHeaders.add("Authorization", "Bearer not-a-real-token");

        CompletableFuture<StompSession> future = client.connectAsync(wsUrl(), handshakeHeaders(),
                connectHeaders, new ConnectionResult());
        assertThatThrownBy(() -> future.get(5, TimeUnit.SECONDS))
                .isInstanceOf(ExecutionException.class);
    }

    @Test
    @DisplayName("CONNECT with a valid JWT establishes a STOMP session")
    void connectWithValidTokenSucceeds() throws Exception {
        User user = createUser("ws-auth");
        Connection connection = connect(user);

        assertThat(connection.session().isConnected()).isTrue();
    }

    /* ------------------------------------------------------------------ */
    /*  Subscription policy                                                */
    /* ------------------------------------------------------------------ */

    @Test
    @DisplayName("An authenticated user can subscribe to the lobby topic")
    void authenticatedUserCanSubscribeToLobby() throws Exception {
        User user = createUser("ws-lobby");
        Connection connection = connect(user);

        connection.session().subscribe("/topic/lobby", queueHandler());
        // The subscription succeeds if no ERROR frame arrives for 2 seconds.
        awaitSettled();
        assertThat(awaitErrors(connection, 2)).isEmpty();
    }

    @Test
    @DisplayName("A non-member cannot subscribe to a room topic")
    void nonMemberCannotSubscribeToRoomTopic() throws Exception {
        TestRoom room = twoPlayerLobby();
        User outsider = createUser("ws-outsider");

        Connection connection = connect(outsider);
        connection.session().subscribe("/topic/rooms/" + room.roomId(), queueHandler());

        List<String> errors = awaitErrors(connection, 5);
        assertThat(errors).isNotEmpty();
        assertThat(errors).anyMatch(message -> message.contains("Not authorized"));
    }

    @Test
    @DisplayName("A match player can subscribe to the match topic")
    void matchPlayerCanSubscribeToMatchTopic() throws Exception {
        TestRoom room = twoPlayerLobby();
        TestUser host = room.host();

        Connection connection = startMatchAndSubscribeToMatchTopic(room, host);
        // The subscription succeeds if no ERROR frame arrives for 2 seconds.
        awaitSettled();
        assertThat(awaitErrors(connection, 2)).isEmpty();
    }

    /* ------------------------------------------------------------------ */
    /*  Room events                                                        */
    /* ------------------------------------------------------------------ */

    @Test
    @DisplayName("JOIN_ROOM is broadcast to the room topic")
    void joinRoomIsBroadcastToRoomTopic() throws Exception {
        TestRoom room = lobby(4);
        TestUser host = room.host();
        StompSession hostSession = connect(host.user()).session();
        BlockingQueue<Map<String, Object>> events =
                subscribeMap(hostSession, "/topic/rooms/" + room.roomId());
        awaitSettled();

        createUserAndJoin(room, "ws-join-guest");

        Map<String, Object> event = awaitEvent(events, "JOIN_ROOM", 5);
        assertThat(event.get("roomId").toString()).isEqualTo(room.roomId().toString());
        assertThat(((Map<?, ?>) event.get("payload")).get("playerCount")).isEqualTo(3);
    }

    @Test
    @DisplayName("READY is broadcast to the room topic")
    void readyIsBroadcastToRoomTopic() throws Exception {
        TestRoom room = twoPlayerLobby();
        TestUser guest = room.guest();
        StompSession guestSession = connect(guest.user()).session();
        BlockingQueue<Map<String, Object>> events =
                subscribeMap(guestSession, "/topic/rooms/" + room.roomId());
        awaitSettled();

        roomService.setReadyStatus(room.roomId(), guest.userId(), false);

        assertThat(awaitEvent(events, "UNREADY", 5)).isNotNull();
        roomService.setReadyStatus(room.roomId(), guest.userId(), true);
        assertThat(awaitEvent(events, "READY", 5)).isNotNull();
    }

    /* ------------------------------------------------------------------ */
    /*  Chat + private queue                                               */
    /* ------------------------------------------------------------------ */

    @Test
    @DisplayName("Chat by a room member is broadcast to every subscriber")
    void chatMessageIsBroadcastToRoomSubscribers() throws Exception {
        TestRoom room = twoPlayerLobby();
        User host = room.host().user();
        User guest = room.guest().user();

        StompSession hostSession = connect(host).session();
        StompSession guestSession = connect(guest).session();
        BlockingQueue<Map<String, Object>> hostEvents =
                subscribeMap(hostSession, "/topic/rooms/" + room.roomId());
        BlockingQueue<Map<String, Object>> guestEvents =
                subscribeMap(guestSession, "/topic/rooms/" + room.roomId());
        awaitSettled();

        Map<String, Object> chat = new HashMap<>();
        chat.put("message", "hello from the lobby");
        hostSession.send("/app/rooms/" + room.roomId() + "/chat", chat);

        Map<String, Object> hostEvent = awaitEvent(hostEvents, "CHAT_MESSAGE", 5);
        assertChatPayload(hostEvent, host.getId(), "hello from the lobby");

        Map<String, Object> guestEvent = awaitEvent(guestEvents, "CHAT_MESSAGE", 5);
        assertChatPayload(guestEvent, host.getId(), "hello from the lobby");
    }

    @Test
    @DisplayName("A non-member gets a private WEBSOCKET_ERROR for chat")
    void nonMemberChatGetsPrivateWebSocketError() throws Exception {
        TestRoom room = twoPlayerLobby();
        User outsider = createUser("ws-chat-outsider");

        StompSession session = connect(outsider).session();
        BlockingQueue<Map<String, Object>> privateQueue =
                subscribeMap(session, "/user/queue/events");
        awaitSettled();

        Map<String, Object> chat = new HashMap<>();
        chat.put("message", "intruder");
        session.send("/app/rooms/" + room.roomId() + "/chat", chat);

        Map<String, Object> event = awaitEvent(privateQueue, "WEBSOCKET_ERROR", 5);
        assertThat(((Map<?, ?>) event.get("payload")).get("errorCode"))
                .isEqualTo("NOT_IN_ROOM");
    }

    @Test
    @DisplayName("An empty chat message gets a private WEBSOCKET_ERROR")
    void emptyChatGetsPrivateWebSocketError() throws Exception {
        TestRoom room = twoPlayerLobby();
        User host = room.host().user();

        StompSession session = connect(host).session();
        BlockingQueue<Map<String, Object>> privateQueue =
                subscribeMap(session, "/user/queue/events");
        awaitSettled();

        Map<String, Object> chat = new HashMap<>();
        chat.put("message", "   ");
        session.send("/app/rooms/" + room.roomId() + "/chat", chat);

        Map<String, Object> event = awaitEvent(privateQueue, "WEBSOCKET_ERROR", 5);
        assertThat(((Map<?, ?>) event.get("payload")).get("errorCode"))
                .isEqualTo("CHAT_EMPTY_MESSAGE");
    }

    /* ------------------------------------------------------------------ */
    /*  Match / game events                                                */
    /* ------------------------------------------------------------------ */

    @Test
    @DisplayName("START_GAME is broadcast to the room topic with the new match id")
    void startGameIsBroadcastToRoomTopic() throws Exception {
        TestRoom room = twoPlayerLobby();
        TestUser host = room.host();

        StompSession hostSession = connect(host.user()).session();
        BlockingQueue<Map<String, Object>> events =
                subscribeMap(hostSession, "/topic/rooms/" + room.roomId());
        awaitSettled();

        MatchResponse match = matchService.startMatch(room.roomId(), host.userId());
        matchId = match.getId();

        Map<String, Object> event = awaitEvent(events, "START_GAME", 5);
        assertThat(((Map<?, ?>) event.get("payload")).get("matchId").toString())
                .isEqualTo(match.getId().toString());
        assertThat(((Map<?, ?>) event.get("payload")).get("roomId").toString())
                .isEqualTo(room.roomId().toString());
    }

    @Test
    @DisplayName("PLAYER_ACTION and TURN_CHANGE are broadcast to the match topic")
    void playerActionAndTurnChangeAreBroadcastToMatchTopic() throws Exception {
        TestRoom room = twoPlayerLobby();
        TestUser host = room.host();
        TestUser guest = room.guest();

        StompSession hostSession = connect(host.user()).session();
        BlockingQueue<Map<String, Object>> events =
                subscribeMap(hostSession, "/topic/matches/" + startMatchAndGetId(room, host));
        awaitSettled();

        gameEngine.performIncome(matchId, host.userId());

        // The engine advances the turn (TURN_CHANGE) before publishing the
        // resolved action (PLAYER_ACTION), so assert the exact arrival order.
        List<Map<String, Object>> ordered = awaitSequence(events,
                List.of("TURN_CHANGE", "PLAYER_ACTION"), 5);
        assertThat(ordered).hasSize(2);
        assertThat(ordered.get(0).get("eventType")).isEqualTo("TURN_CHANGE");
        assertThat(((Map<?, ?>) ordered.get(0).get("payload")).get("currentTurnPlayerId").toString())
                .isEqualTo(guest.userId().toString());
        assertThat(((Map<?, ?>) ordered.get(0).get("payload")).get("turnNumber"))
                .isEqualTo(2);
        Map<String, Object> action = ordered.get(1);
        assertThat(action.get("eventType")).isEqualTo("PLAYER_ACTION");
        assertThat(((Map<?, ?>) action.get("payload")).get("actionType"))
                .isEqualTo("INCOME");
        assertThat(((Map<?, ?>) action.get("payload")).get("outcome"))
                .isEqualTo("RESOLVED");
    }

    @Test
    @DisplayName("A BLOCK claim is broadcast to the match topic")
    void blockClaimIsBroadcastToMatchTopic() throws Exception {
        TestRoom room = twoPlayerLobby();
        TestUser host = room.host();
        TestUser guest = room.guest();

        StompSession hostSession = connect(host.user()).session();
        StompSession guestSession = connect(guest.user()).session();
        BlockingQueue<Map<String, Object>> events =
                subscribeMap(hostSession, "/topic/matches/" + startMatchAndGetId(room, host));
        awaitSettled();

        // Turn 1: host income (moves focus to the guest).
        gameEngine.performIncome(matchId, host.userId());
        // Turn 2: guest declares Foreign Aid, host blocks as Minister.
        gameEngine.performForeignAid(matchId, guest.userId());
        blockManager.block(matchId, host.userId(), GameEngine.CHARACTER_MINISTER);

        Map<String, Object> block = awaitEvent(events, "BLOCK", 5);
        assertThat(((Map<?, ?>) block.get("payload")).get("blockerUserId").toString())
                .isEqualTo(host.userId().toString());
        assertThat(((Map<?, ?>) block.get("payload")).get("blockedCharacter"))
                .isEqualTo("minister");
        assertThat(((Map<?, ?>) block.get("payload")).get("actionType"))
                .isEqualTo(GameEngine.ACTION_FOREIGN_AID);
    }

    @Test
    @DisplayName("A resolved bluff challenge emits CHALLENGE and CARD_REVEAL")
    void bluffChallengeEmitsChallengeAndCardReveal() throws Exception {
        TestRoom room = twoPlayerLobby();
        TestUser host = room.host();
        TestUser guest = room.guest();

        StompSession hostSession = connect(host.user()).session();
        StompSession guestSession = connect(guest.user()).session();
        BlockingQueue<Map<String, Object>> events =
                subscribeMap(hostSession, "/topic/matches/" + startMatchAndGetId(room, host));
        awaitSettled();

        // Turn 1: host bluffs a character they provably do not hold.
        String bluff = chooseBluff(host.userId());
        if (GameEngine.CHARACTER_MINISTER.equals(bluff)) {
            gameEngine.performTax(matchId, host.userId());
        } else if (GameEngine.CHARACTER_DALAL.equals(bluff)) {
            gameEngine.performSteal(matchId, host.userId(), guest.userId());
        } else {
            // AMLA fallback: host holds both MINISTER and DALAL.
            gameEngine.performExchange(matchId, host.userId());
        }

        // Turn 2: the guest challenges the bluff.
        challengeManager.challenge(matchId, guest.userId(), null);

        Map<String, Object> challenge = awaitEvent(events, "CHALLENGE", 5);
        assertThat(((Map<?, ?>) challenge.get("payload")).get("claimTrue"))
                .isEqualTo(false);
        assertThat(((Map<?, ?>) challenge.get("payload")).get("claimantUserId").toString())
                .isEqualTo(host.userId().toString());

        Map<String, Object> reveal = awaitEvent(events, "CARD_REVEAL", 5);
        assertThat(((Map<?, ?>) reveal.get("payload")).get("reason"))
                .isEqualTo("revealed");
        assertThat(((Map<?, ?>) reveal.get("payload")).get("playerId").toString())
                .isEqualTo(host.userId().toString());
    }

    @Test
    @DisplayName("GAME_OVER is broadcast to the match topic with the winner")
    void gameOverIsBroadcastToMatchTopic() throws Exception {
        TestRoom room = twoPlayerLobby();
        TestUser host = room.host();
        TestUser guest = room.guest();

        StompSession hostSession = connect(host.user()).session();
        StompSession guestSession = connect(guest.user()).session();
        BlockingQueue<Map<String, Object>> events =
                subscribeMap(hostSession, "/topic/matches/" + startMatchAndGetId(room, host));
        awaitSettled();

        // Reproduce the deterministic Module 21 scenario: ship income, one
        // assassination, then a host bluff that the guest challenges —
        // exactly like FullGameSimulationTest#fullGame_bluffChallengeEndsTheGame.
        gameEngine.performIncome(matchId, host.userId());
        gameEngine.performIncome(matchId, guest.userId());
        gameEngine.performIncome(matchId, host.userId());
        gameEngine.performAssassinate(matchId, guest.userId(), host.userId());
        actionResolver.resolve(matchId, guest.userId());
        gameEngine.performIncome(matchId, host.userId());
        gameEngine.performIncome(matchId, guest.userId());

        String bluff = chooseBluff(host.userId());
        if (GameEngine.CHARACTER_MINISTER.equals(bluff)) {
            gameEngine.performTax(matchId, host.userId());
        } else if (GameEngine.CHARACTER_DALAL.equals(bluff)) {
            gameEngine.performSteal(matchId, host.userId(), guest.userId());
        } else {
            // AMLA fallback: host holds both MINISTER and DALAL.
            gameEngine.performExchange(matchId, host.userId());
        }
        challengeManager.challenge(matchId, guest.userId(), null);

        Map<String, Object> gameOver = awaitEvent(events, "GAME_OVER", 5);
        assertThat(((Map<?, ?>) gameOver.get("payload")).get("winnerId").toString())
                .isEqualTo(guest.userId().toString());
        assertThat(((Map<?, ?>) gameOver.get("payload")).get("winnerUsername"))
                .isEqualTo(guest.user().getUsername());
    }

    /* ------------------------------------------------------------------ */
    /*  Module 23 — authoritative full-state snapshots + resync            */
    /* ------------------------------------------------------------------ */

    @Test
    @DisplayName("STATE_UPDATED (public) and PRIVATE_STATE (per-viewer) carry the SAME version and no cards leak")
    void stateUpdatedAndPrivateStateCarrySameVersion() throws Exception {
        TestRoom room = twoPlayerLobby();
        TestUser host = room.host();
        TestUser guest = room.guest();

        startMatchAndGetId(room, host);
        StompSession hostSession = connect(host.user()).session();
        StompSession guestSession = connect(guest.user()).session();
        BlockingQueue<Map<String, Object>> topicEvents =
                subscribeMap(hostSession, "/topic/matches/" + matchId);
        BlockingQueue<Map<String, Object>> hostPrivate =
                subscribeMap(hostSession, "/user/queue/events");
        BlockingQueue<Map<String, Object>> guestPrivate =
                subscribeMap(guestSession, "/user/queue/events");
        awaitSettled();

        gameEngine.performIncome(matchId, host.userId());

        // Public snapshot: versioned, viewer-neutral, no card data anywhere.
        Map<String, Object> publicEvent = awaitEvent(topicEvents, "STATE_UPDATED", 5);
        assertThat(publicEvent).isNotNull();
        Map<?, ?> publicPayload = payloadOf(publicEvent);
        assertThat(publicPayload.get("matchId").toString()).isEqualTo(matchId.toString());
        long publicVersion = number(publicPayload.get("stateVersion"));
        List<?> publicPlayers = (List<?>) publicPayload.get("players");
        assertThat(publicPlayers).hasSize(2);
        assertThat(publicPlayers)
                .allSatisfy(p -> assertThat(((Map<?, ?>) p).get("cards")).isNull());

        // Every player gets a private snapshot with the very same version.
        Map<String, Object> hostPrivateEvent = awaitEvent(hostPrivate, "PRIVATE_STATE", 5);
        Map<?, ?> hostPayload = payloadOf(hostPrivateEvent);
        assertThat(number(hostPayload.get("stateVersion"))).isEqualTo(publicVersion);

        // Host sees their OWN two cards, but not the opponent's.
        List<?> hostPlayers = (List<?>) hostPayload.get("players");
        assertThat(((List<?>) playerOf(hostPlayers, host.userId()).get("cards"))).hasSize(2);
        assertThat(playerOf(hostPlayers, guest.userId()).get("cards")).isNull();

        Map<String, Object> guestPrivateEvent = awaitEvent(guestPrivate, "PRIVATE_STATE", 5);
        assertThat(number(payloadOf(guestPrivateEvent).get("stateVersion"))).isEqualTo(publicVersion);
    }

    @Test
    @DisplayName("STATE_UPDATED versions increase monotonically across mutations")
    void stateVersionBumpsMonotonically() throws Exception {
        TestRoom room = twoPlayerLobby();
        TestUser host = room.host();

        StompSession hostSession = connect(host.user()).session();
        BlockingQueue<Map<String, Object>> events =
                subscribeMap(hostSession, "/topic/matches/" + startMatchAndGetId(room, host));
        awaitSettled();

        // First action: it lazily initializes the state (broadcast v1) and then
        // bumps to v2 for the income itself.
        gameEngine.performIncome(matchId, host.userId());

        Map<String, Object> first = awaitEvent(events, "STATE_UPDATED", 5);
        long firstVersion = number(payloadOf(first).get("stateVersion"));

        // Second action (guest's turn now): the version must strictly increase.
        gameEngine.performIncome(matchId, room.guest().userId());

        Map<String, Object> second = awaitEvent(events, "STATE_UPDATED", 5);
        long secondVersion = number(payloadOf(second).get("stateVersion"));

        assertThat(secondVersion).isGreaterThan(firstVersion);
    }

    @Test
    @DisplayName("POST /app/matches/{matchId}/sync replies with a fresh PRIVATE_STATE and rejects duplicates")
    void syncCommandRepliesWithFreshPrivateStateAndRejectsDuplicates() throws Exception {
        TestRoom room = twoPlayerLobby();
        TestUser host = room.host();

        startMatchAndGetId(room, host);
        gameEngine.performIncome(matchId, host.userId());

        StompSession hostSession = connect(host.user()).session();
        BlockingQueue<Map<String, Object>> hostPrivate =
                subscribeMap(hostSession, "/user/queue/events");
        awaitSettled();

        String requestId = "req-" + UUID.randomUUID();
        Map<String, Object> sync = new HashMap<>();
        sync.put("requestId", requestId);
        sync.put("version", 0L);
        hostSession.send("/app/matches/" + matchId + "/sync", sync);

        Map<String, Object> reply = awaitEvent(hostPrivate, "PRIVATE_STATE", 5);
        assertThat(reply).isNotNull();
        Map<?, ?> payload = payloadOf(reply);
        assertThat(payload.get("matchId").toString()).isEqualTo(matchId.toString());
        assertThat(number(payload.get("stateVersion"))).isGreaterThanOrEqualTo(1);
        assertThat(reply.get("matchId").toString()).isEqualTo(matchId.toString());

        // A delayed retry with the SAME requestId must NOT re-arm the client:
        // it is rejected as a duplicate and surfaced as a private error.
        Map<String, Object> duplicateSync = new HashMap<>();
        duplicateSync.put("requestId", requestId);
        duplicateSync.put("version", 0L);
        hostSession.send("/app/matches/" + matchId + "/sync", duplicateSync);

        Map<String, Object> error = awaitEvent(hostPrivate, "WEBSOCKET_ERROR", 5);
        assertThat(error).isNotNull();
        assertThat(payloadOf(error).get("errorCode")).isEqualTo("DUPLICATE_REQUEST");
    }

    @Test
    @DisplayName("A non-member cannot resync a match and receives a private NOT_IN_MATCH error")
    void nonMemberResyncGetsPrivateWebSocketError() throws Exception {
        TestRoom room = twoPlayerLobby();
        TestUser host = room.host();
        startMatchAndGetId(room, host);

        User outsider = createUser("ws-sync-outsider");
        StompSession session = connect(outsider).session();
        BlockingQueue<Map<String, Object>> privateQueue =
                subscribeMap(session, "/user/queue/events");
        awaitSettled();

        Map<String, Object> sync = new HashMap<>();
        sync.put("requestId", "req-" + UUID.randomUUID());
        sync.put("version", 0L);
        session.send("/app/matches/" + matchId + "/sync", sync);

        Map<String, Object> error = awaitEvent(privateQueue, "WEBSOCKET_ERROR", 5);
        assertThat(error).isNotNull();
        assertThat(payloadOf(error).get("errorCode")).isEqualTo("NOT_IN_MATCH");
    }

    /* ------------------------------------------------------------------ */
    /*  Setup helpers                                                      */
    /* ------------------------------------------------------------------ */

    private User createUser(String prefix) {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        return userRepository.save(User.builder()
                .username(prefix + "-" + suffix)
                .email(prefix + "-" + suffix + "@rajneeti.test")
                .password("secret")
                .build());
    }

    /** A lobby with a host, a ready guest and the host ready: ready to start. */
    private TestRoom twoPlayerLobby() {
        return lobby(2);
    }

    /**
     * Creates a {@code maxPlayers}-capacity lobby with a host, a ready guest
     * and the host ready (seats 1 and 2).
     */
    private TestRoom lobby(int maxPlayers) {
        User host = createUser("ws-host");
        User guest = createUser("ws-guest");

        RoomResponse room = roomService.createRoom(host.getId(),
                CreateRoomRequest.builder().maxPlayers(maxPlayers).build());
        roomService.joinRoom(guest.getId(), JoinRoomRequest.builder()
                .roomCode(room.getRoomCode())
                .build());
        roomService.setReadyStatus(room.getId(), host.getId(), true);
        roomService.setReadyStatus(room.getId(), guest.getId(), true);

        return new TestRoom(room.getId(), new TestUser(host), new TestUser(guest));
    }

    private TestUser createUserAndJoin(TestRoom room, String prefix) {
        User user = createUser(prefix);
        roomService.joinRoom(user.getId(), JoinRoomRequest.builder()
                .roomCode(roomCodeOf(room.roomId()))
                .build());
        return new TestUser(user);
    }

    private String roomCodeOf(UUID roomId) {
        return roomService.getRoom(roomId).getRoomCode();
    }

    /**
     * Starts the match for a ready lobby and returns the match id, keeping the
     * {@code matchId} field wired for {@code @AfterEach} cleanup.
     */
    private UUID startMatchAndGetId(TestRoom room, TestUser host) {
        MatchResponse match = matchService.startMatch(room.roomId(), host.userId());
        matchId = match.getId();
        return matchId;
    }

    /**
     * Starts the match, subscribes the host to the freshly-known match topic
     * and returns the host's connection (session + error handler).
     */
    private Connection startMatchAndSubscribeToMatchTopic(TestRoom room, TestUser host)
            throws Exception {
        Connection connection = connect(host.user());
        connection.session().subscribe(
                "/topic/matches/" + startMatchAndGetId(room, host), queueHandler());
        return connection;
    }

    /**
     * Returns a character the host provably does NOT hold, so claiming it is a
     * guaranteed bluff. A player starts with 2 cards, so at most 2 character types
     * are owned; at least 3 of the 5 types are always absent.
     *
     * <p>Preference order (to keep the TAX / STEAL test paths alive):
     * <ol>
     *   <li>MINISTER absent → return MINISTER (TAX path)</li>
     *   <li>DALAL absent → return DALAL (STEAL path)</li>
     *   <li>Host holds both MINISTER and DALAL → return AMLA (Exchange path).
     *       AMLA is guaranteed absent because both hand slots are already taken
     *       by MINISTER and DALAL, so the Exchange bluff will also yield
     *       {@code claimTrue = false}.</li>
     * </ol>
     */
    private String chooseBluff(UUID userId) {
        List<GameCard> cards = gameEngine.getOrInitialize(matchId).getPlayers().stream()
                .filter(p -> p.getUserId().equals(userId))
                .findFirst().orElseThrow()
                .getCards();
        java.util.Set<String> owned = cards.stream()
                .map(card -> card.getCharacter().name().toLowerCase())
                .collect(java.util.stream.Collectors.toSet());
        // Prefer TAX path: claim MINISTER when host does not hold one.
        if (!owned.contains(GameEngine.CHARACTER_MINISTER)) {
            return GameEngine.CHARACTER_MINISTER;
        }
        // Prefer STEAL path: claim DALAL when host does not hold one.
        if (!owned.contains(GameEngine.CHARACTER_DALAL)) {
            return GameEngine.CHARACTER_DALAL;
        }
        // Host holds both MINISTER and DALAL (possible with a 2-card hand).
        // AMLA is absent because both hand slots are occupied — Exchange bluff
        // also goes through challenge resolution and yields claimTrue = false.
        return GameEngine.CHARACTER_AMLA;
    }

    /* ------------------------------------------------------------------ */
    /*  STOMP client plumbing                                              */
    /* ------------------------------------------------------------------ */

    private String wsUrl() {
        return "ws://localhost:" + port + "/ws";
    }

    private WebSocketHttpHeaders handshakeHeaders() {
        WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
        headers.setOrigin(ALLOWED_ORIGIN);
        return headers;
    }

    private Connection connect(User user) throws Exception {
        StompHeaders connectHeaders = new StompHeaders();
        connectHeaders.add("Authorization", "Bearer " + tokenOf(user));

        ConnectionResult handler = new ConnectionResult();
        CompletableFuture<StompSession> future = client.connectAsync(wsUrl(), handshakeHeaders(),
                connectHeaders, handler);
        StompSession session = future.get(8, TimeUnit.SECONDS);
        openSessions.add(session);
        return new Connection(session, handler);
    }

    private String tokenOf(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId().toString());
        return jwtTokenProvider.generateAccessToken(user.getUsername(), claims);
    }

    private StompFrameHandler queueHandler() {
        return new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return Map.class;
            }

            @Override
            @SuppressWarnings("unchecked")
            public void handleFrame(StompHeaders headers, Object payload) {
                // Ignored; the connection-level handler tracks errors.
            }
        };
    }

    /**
     * Subscribes and waits a short moment so the (asynchronous) simple broker
     * registers the subscription before the test triggers an event.
     */
    private BlockingQueue<Map<String, Object>> subscribeMap(StompSession session,
                                                             String destination)
            throws InterruptedException {
        BlockingQueue<Map<String, Object>> received = new LinkedBlockingQueue<>();
        session.subscribe(destination, new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return Map.class;
            }

            @Override
            @SuppressWarnings("unchecked")
            public void handleFrame(StompHeaders headers, Object payload) {
                if (payload instanceof Map) {
                    received.add((Map<String, Object>) payload);
                }
            }
        });
        awaitSettled();
        return received;
    }

    private void awaitSettled() throws InterruptedException {
        Thread.sleep(250);
    }

    /**
     * Polls for an event with the given {@code eventType}, skipping any frames
     * that arrive before it (e.g. the PLAYER_ACTION that precedes the
     * TURN_CHANGE). Returns the envelope or null when the deadline passes.
     */
    private Map<String, Object> awaitEvent(BlockingQueue<Map<String, Object>> events,
                                           String eventType, long seconds)
            throws InterruptedException {
        long deadline = System.nanoTime() + seconds * 1_000_000_000L;
        while (System.nanoTime() < deadline) {
            long remainingNanos = deadline - System.nanoTime();
            Map<String, Object> event = events.poll(
                    Math.max(remainingNanos / 1_000_000L, 1), TimeUnit.MILLISECONDS);
            if (event == null) {
                continue;
            }
            if (eventType.equals(event.get("eventType"))) {
                return event;
            }
        }
        return null;
    }

    /**
     * Waits for a specific sequence of events in arrival order. Events that do
     * not match the next expected type are skipped, so only events the test
     * needs afterwards should be asserted together (use {@link #awaitEvent} for
     * the terminal business outcome instead).
     */
    private List<Map<String, Object>> awaitSequence(BlockingQueue<Map<String, Object>> events,
                                                    List<String> eventTypes, long seconds)
            throws InterruptedException {
        long deadline = System.nanoTime() + seconds * 1_000_000_000L;
        List<Map<String, Object>> matched = new ArrayList<>();
        for (String eventType : eventTypes) {
            while (System.nanoTime() < deadline) {
                long remainingNanos = deadline - System.nanoTime();
                Map<String, Object> event = events.poll(
                        Math.max(remainingNanos / 1_000_000L, 1), TimeUnit.MILLISECONDS);
                if (event != null && eventType.equals(event.get("eventType"))) {
                    matched.add(event);
                    break;
                }
            }
        }
        return matched;
    }

    /** Waits up to {@code seconds} for the connection handler to record an error. */
    private List<String> awaitErrors(Connection connection, long seconds)
            throws InterruptedException {
        long deadline = System.nanoTime() + seconds * 1_000_000_000L;
        while (System.nanoTime() < deadline) {
            List<String> errors = connection.handler().errors();
            if (!errors.isEmpty()) {
                return errors;
            }
            Thread.sleep(50);
        }
        return connection.handler().errors();
    }

    private void assertChatPayload(Map<String, Object> event, UUID senderId, String message) {
        assertThat(event.get("roomId")).isNotNull();
        Map<?, ?> payload = (Map<?, ?>) event.get("payload");
        assertThat(payload.get("senderId").toString()).isEqualTo(senderId.toString());
        assertThat(payload.get("message")).isEqualTo(message);
    }

    @SuppressWarnings("unchecked")
    private Map<?, ?> payloadOf(Map<String, Object> event) {
        return (Map<?, ?>) event.get("payload");
    }

    private long number(Object value) {
        return ((Number) value).longValue();
    }

    @SuppressWarnings("unchecked")
    private Map<?, ?> playerOf(List<?> playerMaps, UUID userId) {
        return (Map<?, ?>) playerMaps.stream()
                .map(p -> (Map<?, ?>) p)
                .filter(p -> p.get("userId").toString().equals(userId.toString()))
                .findFirst()
                .orElseThrow();
    }

    /* ------------------------------------------------------------------ */
    /*  Internal records/adapters                                          */
    /* ------------------------------------------------------------------ */

    private record TestRoom(UUID roomId, TestUser host, TestUser guest) {
    }

    private record TestUser(User user, UUID userId) {
        TestUser(User user) {
            this(user, user.getId());
        }
    }

    private record Connection(StompSession session, ConnectionResult handler) {
    }

    /**
     * A {@code StompSessionHandler} that records every failure surfaced by the
     * client: session-level ERROR frames, send-time exceptions and transport
     * errors. SUBSCRIBE rejections arrive as an ERROR frame (message header)
     * followed by a protocol close, so both are captured here.
     */
    private static class ConnectionResult extends StompSessionHandlerAdapter {

        private final List<String> errors = Collections.synchronizedList(new ArrayList<>());

        List<String> errors() {
            synchronized (errors) {
                return new ArrayList<>(errors);
            }
        }

        @Override
        public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
            // Nothing to do; the connect future is completed by the client.
        }

        @Override
        public void handleFrame(StompHeaders headers, Object payload) {
            // After the connection is established the session-level handler is
            // only invoked for STOMP ERROR frames, so record every one of them.
            errors.add(describe(headers, payload));
        }

        @Override
        public void handleException(StompSession session, StompCommand command,
                                    StompHeaders headers, byte[] payload, Throwable exception) {
            errors.add(String.valueOf(exception == null ? null : exception.getMessage()));
        }

        @Override
        public void handleTransportError(StompSession session, Throwable exception) {
            errors.add(String.valueOf(exception == null ? null : exception.getMessage()));
        }

        private String describe(StompHeaders headers, Object payload) {
            String headerMessage = headers == null ? null : headers.getFirst("message");
            StringBuilder sb = new StringBuilder();
            if (StringUtils.hasText(headerMessage)) {
                sb.append(headerMessage);
            }
            if (payload != null) {
                if (sb.length() > 0) {
                    sb.append(": ");
                }
                sb.append(payload);
            }
            return sb.toString();
        }
    }
}