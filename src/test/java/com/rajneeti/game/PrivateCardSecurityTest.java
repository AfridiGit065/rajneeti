package com.rajneeti.game;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rajneeti.dto.game.GameStateResponse;
import com.rajneeti.entity.enums.MatchStatus;
import com.rajneeti.entity.enums.PlayerStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Critical Security Test: Private Card Isolation.
 *
 * <p>Verifies server-authoritative hidden card protection:
 * <ul>
 *   <li>Player A receives Player A's own cards and NEVER Player B's hidden cards.</li>
 *   <li>Player B receives Player B's own cards and NEVER Player A's hidden cards.</li>
 *   <li>Public broadcast (null viewer) carries zero player cards.</li>
 *   <li>The actual server serialized JSON payload omits opponent cards entirely.</li>
 *   <li>Exchange pool is only visible to the acting player.</li>
 * </ul>
 */
class PrivateCardSecurityTest {

    private final GameStateMapper mapper = new GameStateMapper();
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    private UUID playerAId;
    private UUID playerBId;
    private UUID playerACard1;
    private UUID playerACard2;
    private UUID playerBCard1;
    private UUID playerBCard2;

    @BeforeEach
    void setUp() {
        playerAId = UUID.randomUUID();
        playerBId = UUID.randomUUID();
        playerACard1 = UUID.randomUUID();
        playerACard2 = UUID.randomUUID();
        playerBCard1 = UUID.randomUUID();
        playerBCard2 = UUID.randomUUID();
    }

    private GameState buildMatchState() {
        GamePlayerState playerA = GamePlayerState.builder()
                .userId(playerAId)
                .username("playerA")
                .seatNumber(1)
                .status(PlayerStatus.ACTIVE)
                .coins(2)
                .cards(List.of(
                        GameCard.builder().id(playerACard1).character(CharacterType.MINISTER).build(),
                        GameCard.builder().id(playerACard2).character(CharacterType.DALAL).build()))
                .build();

        GamePlayerState playerB = GamePlayerState.builder()
                .userId(playerBId)
                .username("playerB")
                .seatNumber(2)
                .status(PlayerStatus.ACTIVE)
                .coins(2)
                .cards(List.of(
                        GameCard.builder().id(playerBCard1).character(CharacterType.GHATOK).build(),
                        GameCard.builder().id(playerBCard2).character(CharacterType.GOYENDA).build()))
                .build();

        return GameState.builder()
                .matchId(UUID.randomUUID())
                .roomId(UUID.randomUUID())
                .roomCode("SECURE1")
                .status(MatchStatus.IN_PROGRESS)
                .phase(GameEngine.PHASE_IN_PROGRESS)
                .hostUserId(playerAId)
                .players(new ArrayList<>(List.of(playerA, playerB)))
                .turnOrder(List.of(playerAId, playerBId))
                .currentTurnPlayerId(playerAId)
                .turnNumber(1)
                .stateVersion(5)
                .deck(new ArrayList<>())
                .revealedCardsCount(0)
                .log(new ArrayList<>())
                .startedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Security: Player A never receives Player B's hidden cards in server payload")
    void playerA_neverReceivesPlayerBHiddenCards() throws Exception {
        GameState state = buildMatchState();

        GameStateResponse responseA = mapper.toResponse(state, playerAId);

        // In DTO: Player A sees own cards, Player B cards are null
        assertThat(responseA.getPlayers().get(0).getCards()).hasSize(2);
        assertThat(responseA.getPlayers().get(1).getCards()).isNull();

        // In Serialized JSON:
        String jsonA = objectMapper.writeValueAsString(responseA);

        // Must contain Player A's secret cards
        assertThat(jsonA).contains(playerACard1.toString());
        assertThat(jsonA).contains(playerACard2.toString());

        // MUST NOT contain Player B's secret card IDs anywhere in the payload
        assertThat(jsonA).doesNotContain(playerBCard1.toString());
        assertThat(jsonA).doesNotContain(playerBCard2.toString());

        JsonNode root = objectMapper.readTree(jsonA);
        JsonNode playerBJson = root.path("players").get(1);
        assertThat(playerBJson.path("cards").isMissingNode() || playerBJson.path("cards").isNull()).isTrue();
    }

    @Test
    @DisplayName("Security: Player B never receives Player A's hidden cards in server payload")
    void playerB_neverReceivesPlayerAHiddenCards() throws Exception {
        GameState state = buildMatchState();

        GameStateResponse responseB = mapper.toResponse(state, playerBId);

        // In DTO: Player B sees own cards, Player A cards are null
        assertThat(responseB.getPlayers().get(1).getCards()).hasSize(2);
        assertThat(responseB.getPlayers().get(0).getCards()).isNull();

        // In Serialized JSON:
        String jsonB = objectMapper.writeValueAsString(responseB);

        // Must contain Player B's secret cards
        assertThat(jsonB).contains(playerBCard1.toString());
        assertThat(jsonB).contains(playerBCard2.toString());

        // MUST NOT contain Player A's secret card IDs anywhere in the payload
        assertThat(jsonB).doesNotContain(playerACard1.toString());
        assertThat(jsonB).doesNotContain(playerACard2.toString());

        JsonNode root = objectMapper.readTree(jsonB);
        JsonNode playerAJson = root.path("players").get(0);
        assertThat(playerAJson.path("cards").isMissingNode() || playerAJson.path("cards").isNull()).isTrue();
    }

    @Test
    @DisplayName("Security: Public broadcast payload contains zero player cards")
    void publicBroadcast_containsZeroCards() throws Exception {
        GameState state = buildMatchState();

        GameStateResponse publicResponse = mapper.toResponse(state, null);
        String json = objectMapper.writeValueAsString(publicResponse);

        assertThat(json).doesNotContain(playerACard1.toString());
        assertThat(json).doesNotContain(playerACard2.toString());
        assertThat(json).doesNotContain(playerBCard1.toString());
        assertThat(json).doesNotContain(playerBCard2.toString());

        assertThat(publicResponse.getPlayers())
                .allSatisfy(p -> assertThat(p.getCards()).isNull());
    }

    @Test
    @DisplayName("Security: Exchange pool is isolated strictly to the acting player")
    void exchangePool_isolatedStrictlyToActor() throws Exception {
        GameState state = buildMatchState();
        UUID poolCard1 = UUID.randomUUID();
        UUID poolCard2 = UUID.randomUUID();

        state.setPendingAction(PendingAction.builder()
                .id(UUID.randomUUID())
                .type("EXCHANGE")
                .actorUserId(playerAId)
                .claimedCharacter("amla")
                .exchangePool(List.of(
                        GameCard.builder().id(poolCard1).character(CharacterType.AMLA).build(),
                        GameCard.builder().id(poolCard2).character(CharacterType.MINISTER).build()))
                .build());

        // Player A sees the exchange pool
        GameStateResponse responseA = mapper.toResponse(state, playerAId);
        String jsonA = objectMapper.writeValueAsString(responseA);
        assertThat(jsonA).contains(poolCard1.toString());
        assertThat(jsonA).contains(poolCard2.toString());

        // Player B (opponent) MUST NOT see the exchange pool
        GameStateResponse responseB = mapper.toResponse(state, playerBId);
        String jsonB = objectMapper.writeValueAsString(responseB);
        assertThat(jsonB).doesNotContain(poolCard1.toString());
        assertThat(jsonB).doesNotContain(poolCard2.toString());
        assertThat(responseB.getPendingAction().getExchangePool()).isNull();
    }
}
