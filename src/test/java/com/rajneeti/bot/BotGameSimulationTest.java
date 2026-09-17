package com.rajneeti.bot;

import com.rajneeti.bot.driver.BotDriver;
import com.rajneeti.bot.driver.BotMatchRegistry;
import com.rajneeti.bot.service.BotUserService;
import com.rajneeti.dto.match.MatchResponse;
import com.rajneeti.dto.room.CreateRoomRequest;
import com.rajneeti.dto.room.RoomResponse;
import com.rajneeti.entity.User;
import com.rajneeti.entity.enums.MatchStatus;
import com.rajneeti.game.GameEngine;
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

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Module 25 — an all-bot six-player match played end to end through the real
 * Spring pipeline (room seeding -> match registration -> bot driver polling the
 * real engine). The scheduler is parked in the test profile, so this test drives
 * {@link BotDriver#processMatch(UUID)} directly until a winner emerges.
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
class BotGameSimulationTest {

    private static final int MAX_POLLS = 30_000;

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
    private BotUserService botUserService;

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
    @DisplayName("Six bots play a full match to a single winner")
    void sixBots_playFullMatchToCompletion() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);

        User host = userRepository.save(User.builder()
                .username("sim-bot-host-" + suffix)
                .email("sim-bot-host-" + suffix + "@rajneeti.test")
                .password("secret")
                .isBot(true)
                .botDifficulty("MEDIUM")
                .botPersonality("BALANCED")
                .build());
        UUID hostId = host.getId();

        RoomResponse room = roomService.createRoom(hostId,
                CreateRoomRequest.builder()
                        .maxPlayers(6)
                        .botCount(5)
                        .botDifficulty("MEDIUM")
                        .botPersonality("BALANCED")
                        .build());
        roomService.setReadyStatus(room.getId(), hostId, true);

        // The six participants are all bots (host + five seeded).
        long seededBots = userRepository.findAll().stream()
                .filter(u -> Boolean.TRUE.equals(u.getIsBot())).count();
        assertThat(seededBots).isEqualTo(6);
        assertThat(room.getPlayers()).hasSize(6);
        assertThat(room.getPlayers()).allMatch(p -> Boolean.TRUE.equals(p.getIsBot()));
        assertThat(roomService.canStartMatch(room.getId())).isTrue();

        MatchResponse match = matchService.startMatch(room.getId(), hostId);
        matchId = match.getId();

        // The driver now owns this match.
        assertThat(registry.isRegistered(matchId)).isTrue();
        assertThat(registry.botIds(matchId)).hasSize(6);

        int polls = 0;
        UUID winnerId = null;
        int lastTurn = 0;
        while (polls++ < MAX_POLLS) {
            botDriver.processMatch(matchId);
            GameState state = gameEngine.getGameState(matchId);
            if (state.getStatus() == MatchStatus.FINISHED) {
                winnerId = state.getWinnerUserId();
                break;
            }
            if (state.getTurnNumber() != null) {
                lastTurn = state.getTurnNumber();
            }
        }

        if (winnerId == null) {
            GameState state = gameEngine.getGameState(matchId);
            StringBuilder players = new StringBuilder();
            state.getPlayers().forEach(p -> players.append(String.format(
                    "{user=%s coins=%d cards=%d status=%s} ",
                    p.getUsername(), p.getCoins(), p.getCards().size(), p.getStatus())));
            throw new AssertionError(String.format(
                    "bot game did not finish in %d polls (turn=%d phase=%s pending=%s players=%s)",
                    MAX_POLLS, lastTurn, state.getPhase(),
                    state.getPendingAction() != null ? state.getPendingAction().getType() : "none",
                    players));
        }

        assertThat(polls).as("the bot game must converge within the poll budget")
                .isLessThan(MAX_POLLS);
        assertThat(winnerId).isNotNull();

        List<UUID> botIds = registry.botIds(matchId);
        assertThat(botIds).contains(winnerId);

        // Once finished, the driver detaches from the match.
        botDriver.processMatch(matchId);
        assertThat(registry.isRegistered(matchId)).isFalse();
    }
}