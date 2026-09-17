package com.rajneeti.bot;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rajneeti.bot.ai.AiHttpException;
import com.rajneeti.bot.ai.AiHttpTransport;
import com.rajneeti.bot.ai.AiKeyManager;
import com.rajneeti.bot.config.AiProperties;
import com.rajneeti.bot.decision.BotActionType;
import com.rajneeti.bot.decision.BotDecision;
import com.rajneeti.bot.decision.BotDecisionContext;
import com.rajneeti.bot.decision.BotDecisionType;
import com.rajneeti.bot.decision.BotFallbackStrategy;
import com.rajneeti.bot.decision.LLMBotDecisionProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Module 25 — LLM provider resilience: successful parsing, malformed/illegal
 * responses, transport failures, auth-failure key burnout, and the final
 * handover to the heuristic fallback. Uses a stub transport (no real network).
 */
class LLMBotDecisionProviderTest {

    private static final UUID MATCH = UUID.randomUUID();
    private static final UUID BOT = UUID.randomUUID();
    private static final UUID OPP = UUID.randomUUID();

    private AiProperties props;
    private AiKeyManager keyManager;
    private StubTransport transport;
    private BotFallbackStrategy fallback;
    private LLMBotDecisionProvider provider;
    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ObjectMapper();
        fallback = new BotFallbackStrategy();
        props = new AiProperties();
        props.setUseKeyRotation(true);
        props.setKeyCooldownSeconds(3600);
        props.setMaxKeyRetries(3);
    }

    private BotDecisionContext ownAction() {
        UUID c1 = UUID.randomUUID();
        UUID c2 = UUID.randomUUID();
        return new BotDecisionContext(MATCH, BOT, "bot", "MEDIUM", "BALANCED",
                BotDecisionType.OWN_ACTION, 3, false, 2, 2,
                List.of(new BotDecisionContext.OwnCard(c1, "minister"),
                        new BotDecisionContext.OwnCard(c2, "ghatok")),
                List.of(new BotDecisionContext.OpponentView(OPP, "opp", 3, 2, false, null)),
                List.of(OPP), List.of(OPP), null, null, 20);
    }

    private BotDecisionContext exchangeConfirm() {
        UUID p1 = UUID.randomUUID();
        UUID p2 = UUID.randomUUID();
        UUID p3 = UUID.randomUUID();
        UUID p4 = UUID.randomUUID();
        List<BotDecisionContext.PoolCard> pool = List.of(
                new BotDecisionContext.PoolCard(p1, "minister"),
                new BotDecisionContext.PoolCard(p2, "dalal"),
                new BotDecisionContext.PoolCard(p3, "ghatok"),
                new BotDecisionContext.PoolCard(p4, "goyenda"));
        return new BotDecisionContext(MATCH, BOT, "bot", "MEDIUM", "BALANCED",
                BotDecisionType.EXCHANGE_CONFIRM, 3, false, 2, 2,
                List.of(new BotDecisionContext.OwnCard(p1, "minister"),
                        new BotDecisionContext.OwnCard(p2, "dalal")),
                List.of(new BotDecisionContext.OpponentView(OPP, "opp", 3, 2, false, null)),
                List.of(OPP), List.of(OPP), null, pool, 16);
    }

    private LlmResult jsonDecision(String decisionType, String action, String target,
                                   String claimed, List<String> keep, String loser) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"decisionType\":\"").append(decisionType)
                .append("\",\"action\":\"").append(action)
                .append("\",\"targetPlayerId\":").append(target == null ? "null" : "\"" + target + "\"")
                .append(",\"claimedCharacter\":").append(claimed == null ? "null" : "\"" + claimed + "\"")
                .append(",\"keepCardIds\":[");
        for (int i = 0; i < keep.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append("\"").append(keep.get(i)).append("\"");
        }
        sb.append("],\"loserCardId\":")
                .append(loser == null ? "null" : "\"" + loser + "\"")
                .append(",\"reason\":\"test\"}");
        return new LlmResult(sb.toString(), null);
    }

    /** A transport that replays a script: each entry is a body or an exception. */
    private static final class StubTransport implements AiHttpTransport {
        final List<LlmResult> script = new ArrayList<>();
        final List<String> usedKeys = new ArrayList<>();
        int calls = 0;

        @Override
        public String postChat(String url, String apiKey, String requestJson, long timeoutMs)
                throws IOException {
            calls++;
            usedKeys.add(apiKey);
            LlmResult next = script.get(Math.min(calls - 1, script.size() - 1));
            if (next.throwable != null) {
                throw next.throwable;
            }
            return next.body;
        }
    }

    private record LlmResult(String body, IOException throwable) {
    }

    /* ---------------- Success paths ---------------- */

    @Test
    @DisplayName("Valid LLM decision for an own action is parsed and returned")
    void validDecision_returnsParsedAction() {
        props.setApiKey1("k1");
        props.setApiKey2("k2");
        keyManager = new AiKeyManager(props);
        transport = new StubTransport();
        transport.script.add(jsonDecision("OWN_ACTION", "INCOME", null, null, List.of(), null));
        provider = new LLMBotDecisionProvider(keyManager, transport, props, fallback, mapper);

        BotDecision d = provider.decide(ownAction());

        assertThat(d.action()).isEqualTo(BotActionType.INCOME);
        assertThat(d.decisionType()).isEqualTo(BotDecisionType.OWN_ACTION);
        assertThat(transport.usedKeys).containsExactly("k1");
    }

    @Test
    @DisplayName("EXCHANGE_CONFIRM keepCardIds are parsed and validated")
    void validExchange_returnsParsedKeep() {
        props.setApiKey1("k1");
        keyManager = new AiKeyManager(props);
        transport = new StubTransport();
        BotDecisionContext ctx = exchangeConfirm();
        String keep1 = ctx.exchangePool().get(0).cardId().toString();
        String keep2 = ctx.exchangePool().get(1).cardId().toString();
        transport.script.add(jsonDecision("EXCHANGE_CONFIRM", "CONFIRM_EXCHANGE", null, null,
                List.of(keep1, keep2), null));
        provider = new LLMBotDecisionProvider(keyManager, transport, props, fallback, mapper);

        BotDecision d = provider.decide(ctx);

        assertThat(d.action()).isEqualTo(BotActionType.CONFIRM_EXCHANGE);
        assertThat(d.keepCardIds()).containsExactlyInAnyOrder(
                ctx.exchangePool().get(0).cardId(), ctx.exchangePool().get(1).cardId());
    }

    /* ---------------- Degraded paths ---------------- */

    @Test
    @DisplayName("Malformed JSON is discarded, then the fallback decides")
    void malformedJson_fallsBack() {
        props.setApiKey1("k1");
        keyManager = new AiKeyManager(props);
        transport = new StubTransport();
        transport.script.add(new LlmResult("this is not json", null));
        provider = new LLMBotDecisionProvider(keyManager, transport, props, fallback, mapper);

        BotDecision d = provider.decide(ownAction());

        assertThat(transport.calls).isEqualTo(3); // maxKeyRetries exhausted
        assertThat(d.action()).isEqualTo(fallback.decide(ownAction()).action());
        assertThat(d.action()).isEqualTo(BotActionType.TAX); // deterministic fallback keeps the minister
    }

    @Test
    @DisplayName("Transport failure rotates to the next key and succeeds")
    void transportFailure_failsOverToNextKey() throws IOException {
        props.setApiKey1("k1");
        props.setApiKey2("k2");
        keyManager = new AiKeyManager(props);
        transport = new StubTransport();
        transport.script.add(new LlmResult(null, new IOException("connection reset")));
        transport.script.add(jsonDecision("OWN_ACTION", "INCOME", null, null, List.of(), null));
        provider = new LLMBotDecisionProvider(keyManager, transport, props, fallback, mapper);

        BotDecision d = provider.decide(ownAction());

        assertThat(d.action()).isEqualTo(BotActionType.INCOME);
        assertThat(transport.usedKeys).containsExactly("k1", "k2");
        // k1 is parked in cooldown, k2 is sticky now.
        assertThat(keyManager.acquire()).isEqualTo(1);
    }

    @Test
    @DisplayName("Auth failure burns the key permanently and the next key answers")
    void authFailure_burnsKeyAndFailOver() {
        props.setApiKey1("k1");
        props.setApiKey2("k2");
        keyManager = new AiKeyManager(props);
        transport = new StubTransport();
        transport.script.add(new LlmResult(null, new AiHttpException(401, "invalid api key")));
        transport.script.add(jsonDecision("OWN_ACTION", "INCOME", null, null, List.of(), null));
        provider = new LLMBotDecisionProvider(keyManager, transport, props, fallback, mapper);

        BotDecision d = provider.decide(ownAction());

        assertThat(d.action()).isEqualTo(BotActionType.INCOME);
        assertThat(transport.usedKeys).containsExactly("k1", "k2");
        // k1 is INVALID for good: every further acquisition must answer k2.
        for (int i = 0; i < 5; i++) {
            assertThat(keyManager.acquire()).isEqualTo(1);
        }
    }

    @Test
    @DisplayName("Everything down -> the heuristic fallback guarantees a decision")
    void allFailures_fallBack() {
        props.setApiKey1("k1");
        props.setKeyCooldownSeconds(1);
        props.setMaxKeyRetries(2);
        keyManager = new AiKeyManager(props);
        transport = new StubTransport();
        transport.script.add(new LlmResult(null, new IOException("timeout")));
        provider = new LLMBotDecisionProvider(keyManager, transport, props, fallback, mapper);

        BotDecision d = provider.decide(ownAction());

        assertThat(transport.calls).isEqualTo(1); // second acquire hits cooldown -> -1
        assertThat(d.action()).isEqualTo(BotActionType.TAX);
    }

    @Test
    @DisplayName("Legal-but-declined LLM action is rejected and the fallback steps in")
    void illegalAction_isRejectedAndFallbackDecides() {
        props.setApiKey1("k1");
        keyManager = new AiKeyManager(props);
        transport = new StubTransport();
        // COUP is illegal for a 2-coin bot with a valid target? target is OPP — legal target,
        // but the bot cannot afford a coup (needs 7).
        transport.script.add(jsonDecision("OWN_ACTION", "COUP", OPP.toString(), null, List.of(), null));
        provider = new LLMBotDecisionProvider(keyManager, transport, props, fallback, mapper);

        BotDecision d = provider.decide(ownAction());

        assertThat(d.action()).isEqualTo(BotActionType.TAX);
        assertThat(transport.calls).isEqualTo(3); // every attempt was discarded
    }

    @Test
    @DisplayName("No keys configured -> immediate fallback, zero network calls")
    void noKeys_immediateFallback() {
        keyManager = new AiKeyManager(props);
        transport = new StubTransport();
        provider = new LLMBotDecisionProvider(keyManager, transport, props, fallback, mapper);

        BotDecision d = provider.decide(ownAction());

        assertThat(transport.calls).isZero();
        assertThat(d.action()).isEqualTo(BotActionType.TAX);
    }
}