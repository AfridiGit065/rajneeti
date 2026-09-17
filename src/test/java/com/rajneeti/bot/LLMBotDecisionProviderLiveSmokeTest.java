package com.rajneeti.bot;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rajneeti.bot.ai.AiHttpTransport;
import com.rajneeti.bot.ai.AiKeyManager;
import com.rajneeti.bot.config.AiProperties;
import com.rajneeti.bot.decision.BotDecision;
import com.rajneeti.bot.decision.BotDecisionContext;
import com.rajneeti.bot.decision.BotDecisionType;
import com.rajneeti.bot.decision.LLMBotDecisionProvider;
import com.rajneeti.game.CharacterType;
import com.rajneeti.game.GamePlayerState;
import com.rajneeti.game.GameState;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Live smoke test for the LLM brain. Runs ONLY when a real {@code AI_API_KEY_1}
 * exists in the environment (JUnit skips it otherwise). The keys are read from
 * environment variables at boot — nothing is stored in the repository. Ideal
 * invocation (PowerShell):
 *
 * <pre>
 * $env:AI_PROVIDER="anthropic"
 * $env:AI_BASE_URL="https://api.anthropic.com/v1"
 * $env:AI_MODEL="claude-sonnet-4-5"
 * $env:AI_API_KEY_1="..."; $env:AI_API_KEY_2="..."; $env:AI_API_KEY_3="..."
 * mvn.cmd -o surefire:test -Dtest=LLMBotDecisionProviderLiveSmokeTest
 * </pre>
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "ai.use-key-rotation=true",
        "ai.max-key-retries=3",
        "ai.request-timeout-ms=30000"
})
@EnabledIfEnvironmentVariable(named = "AI_API_KEY_1", matches = ".+")
public class LLMBotDecisionProviderLiveSmokeTest {

    @Autowired
    private LLMBotDecisionProvider provider;

    @Autowired
    private AiHttpTransport transport;

    @Autowired
    private AiProperties aiProperties;

    @Autowired
    private AiKeyManager keyManager;

    @Autowired
    private ObjectMapper objectMapper;

    private String messagesUrl() {
        return (aiProperties.getBaseUrl().endsWith("/")
                ? aiProperties.getBaseUrl() : aiProperties.getBaseUrl() + "/")
                + "messages";
    }

    private String chatUrl() {
        return (aiProperties.getBaseUrl().endsWith("/")
                ? aiProperties.getBaseUrl() : aiProperties.getBaseUrl() + "/")
                + "chat/completions";
    }

    @Test
    @DisplayName("every configured API key authenticates against the live provider")
    void allConfiguredKeysAuthenticate() throws Exception {
        boolean anthropic = "anthropic".equalsIgnoreCase(aiProperties.getProvider());
        assertTrue(!aiProperties.getApiKeys().isEmpty(),
                "at least one API key must be configured");
        assertTrue(aiProperties.getApiKeys().size() == 1
                        || aiProperties.getApiKeys().size() == 3,
                "the manager is built for exactly one or three keys, got "
                        + aiProperties.getApiKeys().size());

        for (int i = 0; i < aiProperties.getApiKeys().size(); i++) {
            String key = aiProperties.getApiKeys().get(i);
            String body;
            String raw;
            if (anthropic) {
                body = objectMapper.writeValueAsString(Map.of(
                        "model", aiProperties.getModel(),
                        "max_tokens", 32,
                        "messages", List.of(Map.of("role", "user", "content",
                                List.of(Map.of("type", "text", "text", "Reply OK"))))));
                raw = transport.postMessages(messagesUrl(), key, body, 30_000);
            } else {
                body = objectMapper.writeValueAsString(Map.of(
                        "model", aiProperties.getModel(),
                        "max_tokens", 32,
                        "messages", List.of(Map.of("role", "user",
                                "content", "Reply OK"))));
                raw = transport.postChat(chatUrl(), key, body, 30_000);
            }

            JsonNode root = objectMapper.readTree(raw);
            if (anthropic) {
                assertTrue(root.has("content") && root.path("content").isArray(),
                        "key #" + (i + 1) + " response has no content blocks: " + raw);
            } else {
                assertTrue(root.path("choices").isArray()
                                && root.path("choices").path(0).path("message").has("content"),
                        "key #" + (i + 1) + " response has no message content: " + raw);
            }
        }
    }

    @Test
    @DisplayName("LLM brain returns a legal OWN_ACTION decision end-to-end")
    void llmBrainDecidesLegally() {
        UUID botId = UUID.randomUUID();
        UUID opponentId = UUID.randomUUID();

        GamePlayerState bot = BotTestFixtures.botPlayer(botId, "smoke-bot", 4,
                new ArrayList<>(List.of(
                        BotTestFixtures.card(CharacterType.MINISTER),
                        BotTestFixtures.card(CharacterType.AMLA))));
        GamePlayerState opponent = BotTestFixtures.player(opponentId, "smoke-opponent", 5,
                new ArrayList<>(List.of(
                        BotTestFixtures.card(CharacterType.GOYENDA))));

        GameState state = BotTestFixtures.baseState(bot, opponent)
                .currentTurnPlayerId(botId)
                .deck(new ArrayList<>(List.of(
                        BotTestFixtures.card(CharacterType.GHATOK),
                        BotTestFixtures.card(CharacterType.DALAL),
                        BotTestFixtures.card(CharacterType.GOYENDA))))
                .build();

        BotDecisionContext context = BotDecisionContext.create(state, bot,
                BotDecisionType.OWN_ACTION);

        BotDecision decision = provider.decide(context);

        assertNotNull(decision, "provider must always fall back to a heuristic decision");
        assertEquals(BotDecisionType.OWN_ACTION, decision.decisionType());
        assertNotNull(decision.action(), "a legal action must be chosen");
        assertTrue(keyManager.hasUsableKey(),
                "at least one configured key should still be usable after the call");
    }
}