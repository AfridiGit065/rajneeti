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
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Module 25 — live full-match proof that the <b>real LLM brain</b> (not the
 * heuristic fallback) can drive a six-bot game end to end through the actual
 * driver + engine pipeline.
 *
 * <p>Runs only when {@code AI_API_KEY_1} is present in the environment (JUnit
 * skips otherwise). Set {@code AI_PROVIDER=gemini} (or anthropic/openai),
 * {@code AI_BASE_URL}, {@code AI_MODEL} and the keys, exactly as documented on
 * {@code LLMBotDecisionProviderLiveSmokeTest}.
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
        "bot.use-llm=true",
        "bot.think-delay-enabled=false",
        "bot.window-grace-ms=0",
        "ai.use-key-rotation=true",
        "ai.max-key-retries=3",
        "ai.request-timeout-ms=30000",
        "logging.level.root=WARN",
        "logging.level.com.rajneeti.bot=INFO"
})
@EnabledIfEnvironmentVariable(named = "AI_API_KEY_1", matches = ".+")
class LLMBotGameSimulationTest {

    private static final int MAX_POLLS = 60_000;
    private static final int STALL_POLLS = 600;

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
    @DisplayName("Six LLM-driven bots play a full match to a single winner")
    void sixLLMBots_playFullMatchToCompletion() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);

        User host = userRepository.save(User.builder()
                .username("llm-bot-host-" + suffix)
                .email("llm-bot-host-" + suffix + "@rajneeti.test")
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

        long seededBots = userRepository.findAll().stream()
                .filter(u -> Boolean.TRUE.equals(u.getIsBot())).count();
        assertThat(seededBots).isEqualTo(6);
        assertThat(room.getPlayers()).hasSize(6);

        MatchResponse match = matchService.startMatch(room.getId(), hostId);
        matchId = match.getId();
        assertThat(registry.botIds(matchId)).hasSize(6);

        int polls = 0;
        int pollsWithoutTurnAdvance = 0;
        int lastTurn = 0;
        UUID winnerId = null;
        while (polls++ < MAX_POLLS) {
            botDriver.processMatch(matchId);
            GameState state = gameEngine.getGameState(matchId);
            if (state.getStatus() == MatchStatus.FINISHED) {
                winnerId = state.getWinnerUserId();
                break;
            }
            int turn = state.getTurnNumber() != null ? state.getTurnNumber() : 0;
            if (turn == lastTurn) {
                if (++pollsWithoutTurnAdvance >= STALL_POLLS) {
                    throw new AssertionError("game is stuck on turn " + lastTurn
                            + " (no turn advance for " + STALL_POLLS + " polls): "
                            + snapshot(state));
                }
            } else {
                pollsWithoutTurnAdvance = 0;
                lastTurn = turn;
            }
        }

        if (winnerId == null) {
            throw new AssertionError("LLM bot game did not finish in " + MAX_POLLS
                    + " polls: " + snapshot(gameEngine.getGameState(matchId)));
        }

        assertThat(winnerId).isNotNull();
        assertThat(registry.botIds(matchId)).contains(winnerId);

        botDriver.processMatch(matchId);
        assertThat(registry.isRegistered(matchId)).isFalse();
    }

    private String snapshot(GameState state) {
        StringBuilder players = new StringBuilder();
        state.getPlayers().forEach(p -> players.append(String.format(
                "{user=%s coins=%d cards=%d status=%s} ",
                p.getUsername(), p.getCoins(),
                p.getCards() != null ? p.getCards().size() : 0, p.getStatus())));
        return String.format("(turn=%d phase=%s pending=%s players=%s)",
                state.getTurnNumber(), state.getPhase(),
                state.getPendingAction() != null
                        ? state.getPendingAction().getType() : "none",
                players);
    }
}