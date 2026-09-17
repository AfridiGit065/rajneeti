package com.rajneeti.game;

import com.rajneeti.dto.game.GamePlayerDto;
import com.rajneeti.dto.game.GameStateResponse;
import com.rajneeti.dto.websocket.WebSocketEventType;
import com.rajneeti.entity.enums.MatchStatus;
import com.rajneeti.entity.enums.PlayerStatus;
import com.rajneeti.websocket.WebSocketEventPublisher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Module 23 — unit tests for the authoritative snapshot broadcaster.
 *
 * <p>Verifies the exact contract of {@code sync}: a single version bump per
 * mutation, one viewer-neutral {@code STATE_UPDATED} on the match topic and one
 * per-viewer {@code PRIVATE_STATE} per player, both sharing the SAME version so
 * clients can order across the two channels without skew.
 */
class GameStateSyncServiceTest {

    private final GameStateMapper mapper = new GameStateMapper();
    private final WebSocketEventPublisher publisher = mock(WebSocketEventPublisher.class);
    private final GameStateSyncService service = new GameStateSyncService(mapper, publisher);

    private final UUID matchId = UUID.randomUUID();
    private final UUID alice = UUID.randomUUID();
    private final UUID bob = UUID.randomUUID();

    private GameState state() {
        return GameState.builder()
                .matchId(matchId)
                .roomId(UUID.randomUUID())
                .roomCode("RJNG02")
                .status(MatchStatus.CREATED)
                .phase(GameEngine.PHASE_IN_PROGRESS)
                .players(List.of(player(alice, "alice"), player(bob, "bob")))
                .turnOrder(List.of(alice, bob))
                .currentTurnPlayerId(alice)
                .turnNumber(1)
                .deck(List.of(GameCard.builder()
                        .id(UUID.randomUUID()).character(CharacterType.MINISTER).build()))
                .revealedCardsCount(0)
                .log(List.of(GameLogEntry.info("started")))
                .startedAt(LocalDateTime.now())
                .build();
    }

    private GamePlayerState player(UUID userId, String username) {
        return GamePlayerState.builder()
                .userId(userId)
                .username(username)
                .seatNumber(1)
                .status(PlayerStatus.ACTIVE)
                .coins(2)
                .cards(List.of(
                        GameCard.builder().id(UUID.randomUUID()).character(CharacterType.GHATOK).build(),
                        GameCard.builder().id(UUID.randomUUID()).character(CharacterType.DALAL).build()))
                .build();
    }

    @Test
    @DisplayName("sync bumps the version once and broadcasts a viewer-neutral public snapshot")
    void sync_bumpsVersionAndBroadcastsPublicSnapshot() {
        GameState state = state();

        long version = service.sync(state);

        assertThat(version).isEqualTo(1);
        assertThat(state.getStateVersion()).isEqualTo(1);

        ArgumentCaptor<GameStateResponse> captor = ArgumentCaptor.forClass(GameStateResponse.class);
        verify(publisher, times(1)).publishToMatch(
                eq(matchId), eq(WebSocketEventType.STATE_UPDATED), isNull(), captor.capture());

        GameStateResponse snapshot = captor.getValue();
        assertThat(snapshot.getMatchId()).isEqualTo(matchId);
        assertThat(snapshot.getStateVersion()).isEqualTo(1);
        // Viewer-neutral public snapshot: NO player exposes any card.
        assertThat(snapshot.getPlayers()).hasSize(2);
        assertThat(snapshot.getPlayers())
                .allSatisfy(p -> assertThat(p.getCards()).isNull());
    }

    @Test
    @DisplayName("every player receives their own PRIVATE_STATE with the SAME version and their own cards")
    void sync_sendsPerViewerPrivateState() {
        GameState state = state();

        service.sync(state);

        verify(publisher, times(2)).sendToUser(
                any(UUID.class), eq(matchId), eq(WebSocketEventType.PRIVATE_STATE), isNull(),
                any(GameStateResponse.class));

        ArgumentCaptor<GameStateResponse> captor = ArgumentCaptor.forClass(GameStateResponse.class);
        verify(publisher, times(1)).sendToUser(
                eq(alice), eq(matchId), eq(WebSocketEventType.PRIVATE_STATE), isNull(), captor.capture());

        GameStateResponse aliceView = captor.getValue();
        assertThat(aliceView.getStateVersion()).isEqualTo(1);

        GamePlayerDto aliceDto = playerOf(aliceView, alice);
        assertThat(aliceDto.getCards()).hasSize(2);

        GamePlayerDto bobInAliceView = playerOf(aliceView, bob);
        assertThat(bobInAliceView.getCards()).isNull();
    }

    @Test
    @DisplayName("a second mutation bumps the version monotonically to 2")
    void sync_isMonotonic() {
        GameState state = state();

        service.sync(state);
        long second = service.sync(state);

        assertThat(second).isEqualTo(2);
        assertThat(state.getStateVersion()).isEqualTo(2);

        ArgumentCaptor<GameStateResponse> captor = ArgumentCaptor.forClass(GameStateResponse.class);
        verify(publisher, times(2)).publishToMatch(
                eq(matchId), eq(WebSocketEventType.STATE_UPDATED), isNull(), captor.capture());
        assertThat(captor.getAllValues().get(1).getStateVersion()).isEqualTo(2);
    }

    @Test
    @DisplayName("sendCurrentState replies with the CURRENT version WITHOUT bumping or broadcasting")
    void sendCurrentStateDoesNotBumpOrBroadcast() {
        GameState state = state();
        service.sync(state);

        service.sendCurrentState(state, alice);

        // The version is untouched by a resync reply.
        assertThat(state.getStateVersion()).isEqualTo(1);
        // Exactly one public broadcast happened (from the sync, not the resync).
        verify(publisher, times(1)).publishToMatch(
                eq(matchId), eq(WebSocketEventType.STATE_UPDATED), isNull(), any(GameStateResponse.class));

        // Alice received TWO private snapshots: one from the sync, one from the
        // resync — both carrying the SAME (current) version, never a bumped one.
        ArgumentCaptor<GameStateResponse> captor = ArgumentCaptor.forClass(GameStateResponse.class);
        verify(publisher, times(2)).sendToUser(
                eq(alice), eq(matchId), eq(WebSocketEventType.PRIVATE_STATE), isNull(), captor.capture());
        assertThat(captor.getAllValues())
                .allSatisfy(snapshot -> assertThat(snapshot.getStateVersion()).isEqualTo(1));

        // Bob only received the one from the original sync.
        verify(publisher, times(1)).sendToUser(
                eq(bob), eq(matchId), eq(WebSocketEventType.PRIVATE_STATE), isNull(), any(GameStateResponse.class));
    }

    @Test
    @DisplayName("a null state / null match id / null user is a safe no-op")
    void nullInputsAreSafeNoOps() {
        assertThat(service.sync(null)).isEqualTo(0);
        service.sync(GameState.builder().roomCode("X").build());
        service.sendCurrentState(null, alice);
        service.sendCurrentState(state(), null);

        verify(publisher, never()).publishToMatch(
                any(), any(), any(), any());
        verify(publisher, never()).sendToUser(
                any(), any(), any(), isNull(), any());
    }

    private GamePlayerDto playerOf(GameStateResponse response, UUID userId) {
        return response.getPlayers().stream()
                .filter(p -> p.getUserId().equals(userId))
                .findFirst()
                .orElseThrow();
    }
}