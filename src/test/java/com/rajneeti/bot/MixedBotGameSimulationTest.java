package com.rajneeti.bot;

import com.rajneeti.bot.driver.BotDriver;
import com.rajneeti.bot.driver.BotMatchRegistry;
import com.rajneeti.dto.match.MatchResponse;
import com.rajneeti.dto.room.CreateRoomRequest;
import com.rajneeti.dto.room.RoomResponse;
import com.rajneeti.entity.User;
import com.rajneeti.entity.enums.MatchStatus;
import com.rajneeti.game.ActionResolver;
import com.rajneeti.game.GameEngine;
import com.rajneeti.game.GamePlayerState;
import com.rajneeti.game.GameState;
import com.rajneeti.game.GameStore;
import com.rajneeti.repository.UserRepository;
import com.rajneeti.service.MatchService;
import com.rajneeti.service.RoomService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Module 25 — a hybrid lobby: a real human host plays against three bots. The
 * human's turns are driven manually (income / forced coup / resolving its own
 * pending action) while {@link BotDriver} drives every bot through the same
 * engine. Proves bots and humans coexist on one match without racing.
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=MySQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "jwt.secret=dGVzdC1zZWNyZXQtdGhhdC1pcy1sb25nLWVub3VnaC1mb3ItaG1hYy1zaGEyNTY=",
        "logging.level.root=WARN",
        "logging.level.com.rajneeti.bot=OFF"
})
class MixedBotGameSimulationTest {

    private static final int MAX_POLLS = 40_000;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoomService roomService;

    @Autowired
    private MatchService matchService;

    @Autowired
    private GameEngine gameEngine;

    @Autowired
    private GameStore gameStore;

    @Autowired
    private ActionResolver actionResolver;

    @Autowired
    private BotDriver botDriver;

    @Autowired
    private BotMatchRegistry registry;

    private UUID matchId;

    @AfterEach
    void cleanup() {
        if (matchId != null) {
            registry.unregister(matchId);
            gameStore.remove(matchId);
        }
    }

    @Test
    @DisplayName("A human host and three bots play a full match to one winner")
    void humanAndBots_playFullMatchToCompletion() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);

        User host = userRepository.save(User.builder()
                .username("sim-human-" + suffix)
                .email("sim-human-" + suffix + "@rajneeti.test")
                .password("secret")
                .build());
        UUID humanId = host.getId();

        RoomResponse room = roomService.createRoom(humanId,
                CreateRoomRequest.builder()
                        .maxPlayers(4)
                        .botCount(3)
                        .botDifficulty("MEDIUM")
                        .botPersonality("BALANCED")
                        .build());
        roomService.setReadyStatus(room.getId(), humanId, true);

        assertThat(room.getPlayers()).hasSize(4);
        assertThat(room.getPlayers()).anyMatch(p -> Boolean.FALSE.equals(p.getIsBot()));
        assertThat(room.getPlayers()).filteredOn(p -> Boolean.TRUE.equals(p.getIsBot())).hasSize(3);
        assertThat(roomService.canStartMatch(room.getId())).isTrue();

        MatchResponse match = matchService.startMatch(room.getId(), humanId);
        matchId = match.getId();

        assertThat(registry.isRegistered(matchId)).isTrue();
        assertThat(registry.botIds(matchId)).hasSize(3);

        int polls = 0;
        UUID winnerId = null;
        while (polls++ < MAX_POLLS) {
            botDriver.processMatch(matchId);

            GameState state = gameEngine.getGameState(matchId);
            if (state.getStatus() == MatchStatus.FINISHED) {
                winnerId = state.getWinnerUserId();
                break;
            }

            if (humanIsCurrent(state, humanId)) {
                playHumanTurn(state, humanId);
            }
        }

        assertThat(polls).as("the hybrid game must converge within the poll budget")
                .isLessThan(MAX_POLLS);
        assertThat(winnerId).isNotNull();
        assertThat(winnerId.equals(humanId) || registry.botIds(matchId).contains(winnerId))
                .as("the winner is either the human host or one of the bots")
                .isTrue();
    }

    private boolean humanIsCurrent(GameState state, UUID humanId) {
        return humanId.equals(state.getCurrentTurnPlayerId());
    }

    private void playHumanTurn(GameState state, UUID humanId) {
        if (state.getPendingAction() != null
                && humanId.equals(state.getPendingAction().getActorUserId())) {
            actionResolver.resolve(matchId, humanId);
            return;
        }
        if (state.getPendingAction() != null || state.isActionExecuted()) {
            return;
        }

        int coins = coinsOf(state, humanId);
        if (coins >= GameEngine.FORCED_COUP_THRESHOLD) {
            UUID target = anyActiveOpponent(state, humanId);
            if (target != null) {
                gameEngine.performCoup(matchId, humanId, target);
            }
        } else {
            gameEngine.performIncome(matchId, humanId);
        }
    }

    private int coinsOf(GameState state, UUID userId) {
        return state.getPlayers().stream()
                .filter(p -> p.getUserId().equals(userId))
                .findFirst()
                .orElseThrow()
                .getCoins();
    }

    private UUID anyActiveOpponent(GameState state, UUID userId) {
        return state.getPlayers().stream()
                .filter(p -> !p.getUserId().equals(userId))
                .filter(p -> p.getStatus() == com.rajneeti.entity.enums.PlayerStatus.ACTIVE)
                .map(GamePlayerState::getUserId)
                .findFirst()
                .orElse(null);
    }
}