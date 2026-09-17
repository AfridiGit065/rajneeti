package com.rajneeti.bot.decision;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rajneeti.bot.ai.AiHttpException;
import com.rajneeti.bot.ai.AiHttpTransport;
import com.rajneeti.bot.ai.AiKeyManager;
import com.rajneeti.bot.config.AiProperties;
import com.rajneeti.game.GameEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Module 25 — LLM-backed decision provider.
 *
 * <p>Sends the serialized {@link BotDecisionContext} (a strictly public / own-
 * cards view of the game) to an OpenAI-compatible chat-completions endpoint and
 * asks for one strict-JSON decision matching the {@link BotDecision} contract.
 * Robustness guarantees:
 *
 * <ul>
 *   <li><b>Key failover</b> — the {@link AiKeyManager} rotates through up to
 *       three keys; 401/403 permanently invalidate a key, other failures put it
 *       in cooldown, and the next key is tried immediately (bounded by
 *       {@code ai.max-key-retries}).</li>
 *   <li><b>Strict validation</b> — the returned JSON is validated against the
 *       legal options of the decision window. Malformed, illegal or empty
 *       responses are discarded.</li>
 *   <li><b>Guaranteed decision</b> — when every attempt fails (or no key is
 *       configured) the provider delegates to {@link BotFallbackStrategy}, so a
 *       bot game never stalls on a dead key. When {@code bot.use-llm} is false
 *       the driver bypasses this class entirely.</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LLMBotDecisionProvider implements BotDecisionProvider {

    private static final String SYSTEM_PROMPT = """
            You play influence cards in the party game RAJNEETI (an Indian-inspired Coup variant).
            You receive a JSON snapshot of your OWN hand and PUBLIC information about opponents.
            You must answer with STRICT JSON only, no prose, exactly this shape:
            {"decisionType":"OWN_ACTION","action":"STEAL","targetPlayerId":"<uuid or null>","claimedCharacter":"minister",
             "keepCardIds":[],"loserCardId":null,"reason":"short rationale"}
            Rules:
            - action must be one of: INCOME FOREIGN_AID TAX STEAL ASSASSINATE EXCHANGE COUP CHALLENGE CHALLENGE_BLOCK BLOCK RESOLVE CONFIRM_EXCHANGE PASS
            - "decisionType" MUST match the window you are answering (OWN_ACTION, ACTION_CHALLENGE, ACTION_BLOCK, BLOCK_CHALLENGE, ACTOR_RESOLVE, EXCHANGE_CONFIRM).
            - OWN_ACTION: choose exactly one action. COUP costs 7, ASSASSINATE costs 3, STEAL is blocked by dalal/amla and challenged as dalal,
              TAX claims minister (+3), EXCHANGE claims amla, FOREIGN_AID (+2, blockable by minister), INCOME (+1, safest).
              A mandatoryCoup=true forces COUP. Never pick an action you cannot afford.
            - ACTION_CHALLENGE: an opponent claimed a character; you may CHALLENGE (stake loserCardId from your own cards) or PASS.
            - ACTION_BLOCK: you may BLOCK with a legal claimedCharacter for the action (foreign_aid->minister, steal->dalal/amla, assassinate->goyenda) or PASS.
            - BLOCK_CHALLENGE: challenge the standing block (CHALLENGE_BLOCK with loserCardId) or PASS.
            - ACTOR_RESOLVE / EXCHANGE_CONFIRM: answer RESOLVE, or CONFIRM_EXCHANGE with exactly 2 keepCardIds from your exchangePool.
            """;

    private final AiKeyManager keyManager;
    private final AiHttpTransport transport;
    private final AiProperties aiProperties;
    private final BotFallbackStrategy fallback;
    private final ObjectMapper objectMapper;

    @Override
    public BotDecision decide(BotDecisionContext context) {
        int attempts = aiProperties.getMaxKeyRetries();
        for (int attempt = 0; attempt < attempts; attempt++) {
            int keyIndex = keyManager.acquire();
            if (keyIndex < 0) {
                break;
            }
            try {
                String raw = requestDecision(context, aiProperties.getApiKeys().get(keyIndex));
                BotDecision decision = parseAndValidate(context, raw);
                keyManager.reportSuccess(keyIndex);
                if (decision == null) {
                    continue;
                }
                return decision;
            } catch (AiHttpException ex) {
                keyManager.reportFailure(keyIndex, ex.isAuthFailure());
                log.warn("AI decision attempt {} failed for {}: {}", attempt + 1,
                        context.decisionType(), ex.getMessage());
            } catch (IOException ex) {
                keyManager.reportFailure(keyIndex, false);
                log.warn("AI transport failure on attempt {} for {}: {}", attempt + 1,
                        context.decisionType(), ex.getMessage());
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                log.warn("AI decision interrupted for {}", context.decisionType());
                break;
            }
        }

        log.debug("AI decision unavailable for {}, delegating to heuristic fallback.",
                context.decisionType());
        return fallback.decide(context);
    }

    private String requestDecision(BotDecisionContext context, String apiKey)
            throws AiHttpException, IOException, InterruptedException {
        String url = aiProperties.getBaseUrl()
                + (aiProperties.getBaseUrl().endsWith("/") ? "" : "/")
                + "chat/completions";

        String systemText = SYSTEM_PROMPT
                + "\nThe claimed blocked/claimed characters use lower-case ids: minister, ghatok, dalal, amla, goyenda."
                + "\nExposed snapshot (your private cards may include cardId; opponent records contain NO cards):";

        String body = objectMapper.writeValueAsString(java.util.Map.of(
                "model", aiProperties.getModel(),
                "temperature", 0.4,
                "max_tokens", 300,
                "response_format", java.util.Map.of("type", "json_object"),
                "messages", List.of(
                        java.util.Map.of("role", "system", "content", systemText),
                        java.util.Map.of("role", "user", "content",
                                objectMapper.writeValueAsString(context)))));

        return transport.postChat(url, apiKey, body, aiProperties.getRequestTimeoutMs());
    }

    /**
     * Validates the raw LLM JSON against the legal options of the decision
     * window. Returns null (→ try next key / fallback) when the response is
     * malformed or chooses an illegal option.
     */
    private BotDecision parseAndValidate(BotDecisionContext context, String raw) {
        try {
            JsonNode node = objectMapper.readTree(raw);
            String decisionType = node.path("decisionType").asText();
            if (!context.decisionType().name().equals(decisionType)) {
                log.debug("AI returned decisionType '{}' for a '{}' window.",
                        decisionType, context.decisionType());
                return null;
            }

            String action = node.path("action").asText();
            UUID targetId = parseUuid(node.path("targetPlayerId").asText(null));
            String claimed = nullSafe(node.path("claimedCharacter").asText(null));
            List<UUID> keep = parseUuidList(node.path("keepCardIds"));
            UUID loserCardId = parseUuid(node.path("loserCardId").asText(null));
            String reason = node.path("reason").asText(null);

            if (!isLegal(context, action, targetId, claimed, keep, loserCardId)) {
                log.debug("AI proposed an illegal '{}' for window {} — ignoring.", action,
                        context.decisionType());
                return null;
            }

            return new BotDecision(context.decisionType(), BotActionType.valueOf(action),
                    targetId, claimed, keep, loserCardId, reason);
        } catch (IOException | IllegalArgumentException ex) {
            log.debug("AI returned garbage JSON for {}: {}", context.decisionType(),
                    ex.getMessage());
            return null;
        }
    }

    private boolean isLegal(BotDecisionContext ctx, String action, UUID targetId,
                            String claimed, List<UUID> keep, UUID loserCardId) {
        try {
            BotActionType type = BotActionType.valueOf(action);
            return switch (ctx.decisionType()) {
                case OWN_ACTION -> isLegalOwnAction(ctx, type, targetId);
                case ACTION_CHALLENGE -> (type == BotActionType.CHALLENGE && isChallengeable(ctx)
                        && ownsCard(ctx, loserCardId))
                        || type == BotActionType.PASS;
                case ACTION_BLOCK -> (type == BotActionType.BLOCK && isLegalBlocker(ctx, claimed))
                        || type == BotActionType.PASS;
                case BLOCK_CHALLENGE -> (type == BotActionType.CHALLENGE_BLOCK
                        && ownsCard(ctx, loserCardId))
                        || type == BotActionType.PASS;
                case ACTOR_RESOLVE -> type == BotActionType.RESOLVE;
                case EXCHANGE_CONFIRM -> type == BotActionType.CONFIRM_EXCHANGE
                        && keep != null && keep.size() == 2 && keep.stream().distinct().count() == 2
                        && keep.stream().allMatch(id -> ctx.exchangePool() != null
                        && ctx.exchangePool().stream().anyMatch(c -> c.cardId().equals(id)));
            };
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    private boolean isLegalOwnAction(BotDecisionContext ctx, BotActionType type, UUID targetId) {
        if (ctx.mandatoryCoup()) {
            return type == BotActionType.COUP && isValidTarget(ctx, targetId);
        }
        return switch (type) {
            case INCOME, TAX, FOREIGN_AID -> true;
            case EXCHANGE -> ctx.deckCount() >= GameEngine.EXCHANGE_DRAW
                    && ctx.ownInfluenceCount() >= GameEngine.STARTING_INFLUENCE;
            case STEAL, COUP -> ctx.ownCoins() >= costOf(type)
                    && isValidTarget(ctx, targetId);
            case ASSASSINATE -> ctx.ownCoins() >= costOf(type)
                    && ctx.deckCount() >= 1
                    && isValidTarget(ctx, targetId);
            default -> false;
        };
    }

    private int costOf(BotActionType type) {
        return switch (type) {
            case STEAL -> 0;
            case ASSASSINATE -> 3;
            case COUP -> 7;
            default -> 0;
        };
    }

    private boolean isValidTarget(BotDecisionContext ctx, UUID targetId) {
        return targetId != null && ctx.legalTargets().contains(targetId);
    }

    private boolean isChallengeable(BotDecisionContext ctx) {
        return ctx.pendingAction() != null
                && ctx.pendingAction().claimedCharacter() != null
                && isChallengeableType(ctx.pendingAction().type());
    }

    private boolean isChallengeableType(String actionType) {
        return switch (actionType) {
            case "TAX", "STEAL", "EXCHANGE", "ASSASSINATE" -> true;
            default -> false;
        };
    }

    private boolean isLegalBlocker(BotDecisionContext ctx, String claimed) {
        if (ctx.pendingAction() == null || claimed == null) {
            return false;
        }
        return switch (ctx.pendingAction().type()) {
            case "FOREIGN_AID" -> claimed.equals("minister");
            case "STEAL" -> claimed.equals("dalal") || claimed.equals("amla");
            case "ASSASSINATE" -> claimed.equals("goyenda");
            default -> false;
        };
    }

    private boolean ownsCard(BotDecisionContext ctx, UUID cardId) {
        return cardId != null && ctx.ownCards().stream()
                .anyMatch(c -> c.cardId().equals(cardId));
    }

    private UUID parseUuid(String value) {
        try {
            return value == null || value.isBlank() ? null : UUID.fromString(value);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private List<UUID> parseUuidList(JsonNode node) {
        List<UUID> ids = new ArrayList<>();
        if (node == null || !node.isArray()) {
            return ids;
        }
        for (JsonNode child : node) {
            UUID id = parseUuid(child.asText(null));
            if (id != null) {
                ids.add(id);
            }
        }
        return ids;
    }

    private String nullSafe(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }
}