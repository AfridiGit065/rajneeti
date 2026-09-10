package com.rajneeti.game;

import com.rajneeti.dto.game.GameCardDto;
import com.rajneeti.dto.game.GamePlayerDto;
import com.rajneeti.dto.game.GameStateResponse;
import com.rajneeti.entity.enums.MatchStatus;
import com.rajneeti.entity.enums.PlayerStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class GameStateMapperTest {

    private final GameStateMapper mapper = new GameStateMapper();

    private GameState buildState(List<GamePlayerState> players, UUID currentTurnId) {
        return GameState.builder()
                .matchId(UUID.randomUUID())
                .roomId(UUID.randomUUID())
                .roomCode("RJNG02")
                .status(MatchStatus.CREATED)
                .phase(GameEngine.PHASE_SETUP)
                .hostUserId(players.get(0).getUserId())
                .players(players)
                .turnOrder(List.of(
                        players.get(0).getUserId(),
                        players.get(1).getUserId()))
                .currentTurnPlayerId(currentTurnId)
                .turnNumber(1)
                .deck(List.of(
                        GameCard.builder().id(UUID.randomUUID()).character(CharacterType.MINISTER).build()))
                .revealedCardsCount(0)
                .log(List.of(GameLogEntry.info("started")))
                .startedAt(LocalDateTime.now())
                .build();
    }

    private GamePlayerState player(UUID userId, String username, UUID... cardIds) {
        List<GameCard> cards = List.of(cardIds).stream()
                .map(id -> GameCard.builder().id(id).character(CharacterType.GHATOK).build())
                .toList();
        return GamePlayerState.builder()
                .userId(userId)
                .username(username)
                .seatNumber(1)
                .status(PlayerStatus.ACTIVE)
                .coins(2)
                .cards(cards)
                .build();
    }

    @Test
    @DisplayName("Projection - viewer sees own cards with details")
    void toResponse_showsOwnCardsToViewer() {
        UUID viewerId = UUID.randomUUID();
        UUID otherId = UUID.randomUUID();
        UUID card1 = UUID.randomUUID();
        UUID card2 = UUID.randomUUID();

        GamePlayerState viewer = player(viewerId, "viewer", card1, card2);
        GamePlayerState other = player(otherId, "other");

        GameStateResponse response = mapper.toResponse(buildState(List.of(viewer, other), viewerId), viewerId);

        GamePlayerDto viewerDto = response.getPlayers().get(0);
        assertThat(viewerDto.getCards()).hasSize(2);
        assertThat(viewerDto.getCards())
                .extracting(GameCardDto::getCardId)
                .containsExactlyInAnyOrder(card1, card2);
        assertThat(viewerDto.getCards())
                .allSatisfy(card -> assertThat(card.getCharacterId()).isEqualTo("ghatok"));
        assertThat(viewerDto.getInfluenceCount()).isEqualTo(2);
        assertThat(viewerDto.isTurn()).isTrue();
    }

    @Test
    @DisplayName("Projection - opponent cards are hidden, influence count still public")
    void toResponse_hidesOpponentCards() {
        UUID viewerId = UUID.randomUUID();
        UUID opponentId = UUID.randomUUID();
        UUID hidden1 = UUID.randomUUID();
        UUID hidden2 = UUID.randomUUID();

        GamePlayerState viewer = player(viewerId, "viewer");
        GamePlayerState opponent = player(opponentId, "opponent", hidden1, hidden2);

        GameStateResponse response = mapper.toResponse(buildState(List.of(viewer, opponent), viewerId), viewerId);

        GamePlayerDto opponentDto = response.getPlayers().get(1);
        assertThat(opponentDto.getCards()).isNull();
        assertThat(opponentDto.getInfluenceCount()).isEqualTo(2);
        assertThat(opponentDto.getCoins()).isEqualTo(2);
        assertThat(opponentDto.isAlive()).isTrue();
        assertThat(opponentDto.isTurn()).isFalse();
    }

    @Test
    @DisplayName("Projection - exposes only public game-level fields")
    void toResponse_exposesPublicFieldsOnly() {
        UUID viewerId = UUID.randomUUID();
        UUID otherId = UUID.randomUUID();

        GameStateResponse response = mapper.toResponse(
                buildState(List.of(player(viewerId, "viewer"), player(otherId, "other")), viewerId),
                viewerId);

        assertThat(response.getMatchId()).isNotNull();
        assertThat(response.getRoomCode()).isEqualTo("RJNG02");
        assertThat(response.getStatus()).isEqualTo(MatchStatus.CREATED);
        assertThat(response.getPhase()).isEqualTo(GameEngine.PHASE_SETUP);
        assertThat(response.getCurrentTurnPlayerId()).isEqualTo(viewerId);
        assertThat(response.getTurnNumber()).isEqualTo(1);
        assertThat(response.getDeckCount()).isEqualTo(1);
        assertThat(response.getRevealedCardsCount()).isZero();
        assertThat(response.getPlayers()).hasSize(2);
        assertThat(response.getLog()).hasSize(1);
    }
}