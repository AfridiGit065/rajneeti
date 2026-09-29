package com.rajneeti.game;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rajneeti.dto.game.GameStateResponse;
import com.rajneeti.dto.websocket.WebSocketEventType;
import com.rajneeti.entity.enums.MatchStatus;
import com.rajneeti.entity.enums.PlayerStatus;
import com.rajneeti.websocket.WebSocketEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Module 24 — an eliminated player keeps no private state.
 *
 * <p>A player who has been eliminated has left the live table. Their influence
 * cards and the Exchange pool must never reach them again, not even their own
 * hand: a spectator at a live table has no private state to receive.
 *
 * <p>These assertions run against the <em>actual serialized JSON</em> that the
 * publisher receives, not against the DTO getters. A getter returning {@code
 * null} is not proof that the wire payload is clean — a field could still be
 * emitted, a name could still leak an identity — so the serialized form is what
 * is checked here.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EliminatedPlayerPrivacyTest {

    @Mock
    private WebSocketEventPublisher publisher;

    private final GameStateMapper mapper = new GameStateMapper();
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    private GameStateSyncService syncService;
    private CardManager cardManager;

    private UUID matchId;
    private UUID survivorId;
    private UUID eliminatedId;
    private UUID survivorCardId;
    private UUID eliminatedCardId;
    @BeforeEach
    void setUp() {
        matchId = UUID.randomUUID();
        survivorId = UUID.randomUUID();
        eliminatedId = UUID.randomUUID();
        survivorCardId = UUID.randomUUID();
        eliminatedCardId = UUID.randomUUID();

        cardManager = new CardManager();
        syncService = new GameStateSyncService(mapper, publisher);
    }

    private GameState stateWithEliminatedPlayer() {
        List<GameCard> deck = cardManager.createDeck();

        // A full, legal hand so hand-size and deck-draw assertions stay realistic.
        List<GameCard> survivorHand = cardManager.drawMany(deck, GameEngine.STARTING_INFLUENCE);
        List<GameCard> eliminatedHand = cardManager.drawMany(deck, GameEngine.STARTING_INFLUENCE);
        survivorCardId = survivorHand.get(0).getId();
        eliminatedCardId = eliminatedHand.get(0).getId();

        GamePlayerState survivor = GamePlayerState.builder()
                .userId(survivorId).username("survivor").seatNumber(1)
                .status(PlayerStatus.ACTIVE).coins(3)
                .cards(survivorHand)
                .host(true).build();
        GamePlayerState eliminated = GamePlayerState.builder()
                .userId(eliminatedId).username("eliminated").seatNumber(2)
                .status(PlayerStatus.ELIMINATED).coins(0)
                .cards(eliminatedHand)
                .host(false).build();

        return GameState.builder()
                .matchId(matchId)
                .roomId(UUID.randomUUID())
                .roomCode("RAJEL")
                .status(MatchStatus.IN_PROGRESS)
                .phase(GameEngine.PHASE_IN_PROGRESS)
                .players(new ArrayList<>(List.of(survivor, eliminated)))
                .turnOrder(List.of(survivorId, eliminatedId))
                .currentTurnPlayerId(survivorId)
                .turnNumber(3)
                .deck(deck)
                .actionExecuted(false)
                .pendingAction(null)
                .log(new ArrayList<>())
                .stateVersion(0L)
                .build();
    }

    /** All {@code PRIVATE_STATE} payloads the publisher was asked to send. */
    private List<GameStateResponse> privateStatePayloads() {
        @SuppressWarnings("unchecked")
        ArgumentCaptor<GameStateResponse> captor =
                ArgumentCaptor.forClass(GameStateResponse.class);
        verify(publisher, atLeastOnce()).sendToUser(
                any(), eq(matchId), eq(WebSocketEventType.PRIVATE_STATE), any(), captor.capture());
        return captor.getAllValues();
    }

    private JsonNode toJson(Object value) {
        try {
            return objectMapper.valueToTree(value);
        } catch (RuntimeException ex) {
            throw new AssertionError("Payload could not be serialized", ex);
        }
    }

    /** The player node for {@code userId} inside a serialized payload. */
    private JsonNode playerNode(JsonNode payload, UUID userId) {
        for (JsonNode player : payload.path("players")) {
            if (userId.toString().equals(player.path("userId").asText())) {
                return player;
            }
        }
        return null;
    }

    /* ------------------------------------------------------------------ */
    /*  No private push at all                                             */
    /* ------------------------------------------------------------------ */

    @Test
    @DisplayName("A - an eliminated player is never sent a PRIVATE_STATE push")
    void sync_neverPushesPrivateStateToEliminatedPlayer() {
        GameState state = stateWithEliminatedPlayer();

        syncService.sync(state);

        verify(publisher, never()).sendToUser(
                eq(eliminatedId), any(), any(), any(), any());
        // The surviving player still gets their authoritative private snapshot.
        verify(publisher).sendToUser(
                eq(survivorId), eq(matchId), eq(WebSocketEventType.PRIVATE_STATE), any(), any());
    }

    @Test
    @DisplayName("A - an eliminated player is not hidden from the public topic")
    void sync_stillPublishesEliminatedPlayerOnThePublicTopic() {
        GameState state = stateWithEliminatedPlayer();

        syncService.sync(state);

        // They stay in the match, so the public broadcast must still list them:
        // hiding them would make coins, status and the log inconsistent for
        // everyone else.
        @SuppressWarnings("unchecked")
        ArgumentCaptor<GameStateResponse> captor =
                ArgumentCaptor.forClass(GameStateResponse.class);
        verify(publisher).publishToMatch(
                eq(matchId), eq(WebSocketEventType.STATE_UPDATED), any(), captor.capture());

        JsonNode json = toJson(captor.getValue());
        JsonNode eliminatedNode = playerNode(json, eliminatedId);
        assertThat(eliminatedNode).as("eliminated player is still listed publicly").isNotNull();
        assertThat(eliminatedNode.path("status").asText()).isEqualTo(PlayerStatus.ELIMINATED.name());
    }

    /* ------------------------------------------------------------------ */
    /*  The serialized payload must be clean                               */
    /* ------------------------------------------------------------------ */

    @Test
    @DisplayName("B - no serialized private payload contains an eliminated player's cards")
    void privatePayloads_neverContainEliminatedPlayerCards() {
        GameState state = stateWithEliminatedPlayer();

        syncService.sync(state);

        String eliminatedCardIdText = eliminatedCardId.toString();
        for (GameStateResponse payload : privateStatePayloads()) {
            String json = toJson(payload).toString();
            assertThat(json)
                    .as("the eliminated player's card id must never be serialized")
                    .doesNotContain(eliminatedCardIdText);

            JsonNode node = playerNode(toJson(payload), eliminatedId);
            if (node != null) {
                JsonNode cards = node.path("cards");
                assertThat(cards.isMissingNode() || cards.isNull())
                        .as("an eliminated player's cards must not be serialized at all")
                        .isTrue();
            }
        }
    }

    @Test
    @DisplayName("B - the public broadcast contains no cards for anyone, eliminated or not")
    void publicBroadcast_containsNoCardsAtAll() {
        GameState state = stateWithEliminatedPlayer();

        syncService.sync(state);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<GameStateResponse> captor =
                ArgumentCaptor.forClass(GameStateResponse.class);
        verify(publisher).publishToMatch(
                eq(matchId), eq(WebSocketEventType.STATE_UPDATED), any(), captor.capture());

        JsonNode json = toJson(captor.getValue());
        for (JsonNode player : json.path("players")) {
            JsonNode cards = player.path("cards");
            assertThat(cards.isMissingNode() || cards.isNull())
                    .as("public projection must not serialize any cards")
                    .isTrue();
        }
        assertThat(json.toString()).doesNotContain(survivorCardId.toString());
    }

    /* ------------------------------------------------------------------ */
    /*  Resync must not become a back door                                 */
    /* ------------------------------------------------------------------ */

    @Test
    @DisplayName("C - a resync from an eliminated viewer is answered with the public projection")
    void resyncFromEliminatedViewer_returnsPublicProjection() {
        GameState state = stateWithEliminatedPlayer();
        state.setStateVersion(12L);

        syncService.sendCurrentState(state, eliminatedId);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<GameStateResponse> captor =
                ArgumentCaptor.forClass(GameStateResponse.class);
        verify(publisher).sendToUser(
                eq(eliminatedId), eq(matchId), eq(WebSocketEventType.PRIVATE_STATE),
                any(), captor.capture());

        JsonNode json = toJson(captor.getValue());

        // Not even their OWN hand comes back.
        assertThat(json.toString()).doesNotContain(eliminatedCardId.toString());
        JsonNode self = playerNode(json, eliminatedId);
        assertThat(self).isNotNull();
        JsonNode ownCards = self.path("cards");
        assertThat(ownCards.isMissingNode() || ownCards.isNull())
                .as("an eliminated viewer must not re-obtain even their own cards")
                .isTrue();

        // A resync must never bump the authoritative version.
        assertThat(state.getStateVersion()).isEqualTo(12L);
    }

    @Test
    @DisplayName("C - a resync from a surviving viewer still returns their own hand")
    void resyncFromActiveViewer_stillReturnsOwnHand() {
        GameState state = stateWithEliminatedPlayer();

        syncService.sendCurrentState(state, survivorId);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<GameStateResponse> captor =
                ArgumentCaptor.forClass(GameStateResponse.class);
        verify(publisher).sendToUser(
                eq(survivorId), eq(matchId), eq(WebSocketEventType.PRIVATE_STATE),
                any(), captor.capture());

        JsonNode self = playerNode(toJson(captor.getValue()), survivorId);
        assertThat(self).isNotNull();
        assertThat(self.path("cards").size())
                .as("an active player must still receive their own hand on resync")
                .isEqualTo(GameEngine.STARTING_INFLUENCE);
    }

    @Test
    @DisplayName("C - a resync from a non-member returns no private state at all")
    void resyncFromStranger_returnsPublicProjection() {
        GameState state = stateWithEliminatedPlayer();
        UUID stranger = UUID.randomUUID();

        syncService.sendCurrentState(state, stranger);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<GameStateResponse> captor =
                ArgumentCaptor.forClass(GameStateResponse.class);
        verify(publisher).sendToUser(
                eq(stranger), eq(matchId), eq(WebSocketEventType.PRIVATE_STATE),
                any(), captor.capture());

        String json = toJson(captor.getValue()).toString();
        assertThat(json).doesNotContain(survivorCardId.toString());
        assertThat(json).doesNotContain(eliminatedCardId.toString());
    }

    /* ------------------------------------------------------------------ */
    /*  The Exchange pool follows the same rule                            */
    /* ------------------------------------------------------------------ */

    @Test
    @DisplayName("D - an eliminated player never receives an open Exchange pool")
    void eliminatedPlayer_neverReceivesExchangePool() {
        GameState state = stateWithEliminatedPlayer();
        // An Exchange owned by the SURVIVOR is open while the eliminated player
        // is still listed in the state.
        state.setPendingAction(PendingAction.builder()
                .id(UUID.randomUUID())
                .type(GameEngine.ACTION_EXCHANGE)
                .actorUserId(survivorId)
                .startedAt(java.time.LocalDateTime.now())
                .deadlineAt(java.time.LocalDateTime.now().plusSeconds(60))
                .exchangePool(cardManager.drawMany(state.getDeck(), 4))
                .build());

        syncService.sync(state);
        syncService.sendCurrentState(state, eliminatedId);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<GameStateResponse> captor =
                ArgumentCaptor.forClass(GameStateResponse.class);
        verify(publisher).sendToUser(
                eq(eliminatedId), eq(matchId), eq(WebSocketEventType.PRIVATE_STATE),
                any(), captor.capture());

        JsonNode json = toJson(captor.getValue());

        // The action is visible (the table is live) but the pool is not: the
        // field is present and null, and no pooled card id is serialized.
        JsonNode pending = json.path("pendingAction");
        assertThat(pending.path("type").asText()).isEqualTo(GameEngine.ACTION_EXCHANGE);
        assertThat(pending.path("exchangePool").isNull())
                .as("an eliminated viewer must never see the Exchange pool")
                .isTrue();
        String serialized = json.toString();
        for (GameCard pooled : state.getPendingAction().getExchangePool()) {
            assertThat(serialized).doesNotContain(pooled.getId().toString());
        }
    }
}
