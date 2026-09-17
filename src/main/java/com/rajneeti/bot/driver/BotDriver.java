package com.rajneeti.bot.driver;

import com.rajneeti.bot.BotDifficulty;
import com.rajneeti.bot.config.BotProperties;
import com.rajneeti.bot.decision.BotDecision;
import com.rajneeti.bot.decision.BotDecisionContext;
import com.rajneeti.bot.decision.BotDecisionProvider;
import com.rajneeti.bot.decision.BotDecisionType;
import com.rajneeti.bot.decision.BotFallbackStrategy;
import com.rajneeti.bot.decision.LLMBotDecisionProvider;
import com.rajneeti.bot.executor.BotActionExecutor;
import com.rajneeti.entity.enums.PlayerStatus;
import com.rajneeti.game.GameEngine;
import com.rajneeti.game.GamePlayerState;
import com.rajneeti.game.GameState;
import com.rajneeti.game.PendingAction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * Module 25 — drives one registered match forward, one bot action per bot per
 * poll. This is the ONLY place that decides WHEN a bot is allowed to act.
 *
 * <p>Window model (per bot, per poll, one action max):
 * <ol>
 *   <li><b>Actor windows</b> — the bot is the actor of the pending action with
 *       no challenge/block being decided right now. Waits
 *       {@code max(think, window-grace-ms)} so humans (and other bots) get a
 *       real chance to respond, then resolves or confirms the exchange.</li>
 *   <li><b>Block-challenge window</b> — a block claim is standing and open to
 *       challenge; the bot decides {@code CHALLENGE_BLOCK} or {@code PASS}.</li>
 *   <li><b>Action window</b> — an untouched pending action is open; the bot
 *       answers once with {@code CHALLENGE}, {@code BLOCK} or {@code PASS}.</li>
 *   <li><b>Own action</b> — it is the bot's turn, nothing pending; the bot
 *       decides its action.</li>
 * </ol>
 *
 * <p>Dedup keys are per match + bot: {@code "own:<turnNumber>"},
 * {@code "win:<pendingId>"} (covers both challenge and block),
 * {@code "bc:<pendingId>"} (block challenge) and {@code "res:<pendingId>"}
 * (actor resolve / exchange confirm). A new pending action gets a new id, hence
 * new keys, so stale decisions can never revisit a closed window.
 *
 * <p>Failure policy: a rejected opponent/actor decision consumes the window
 * (the bot had its one shot, matching the human model). A rejected <em>own
 * action</em> clears the observation so the bot retries next poll — otherwise a
 * single illegal LLM proposal would permanently stall the bot's turn. After
 * {@link #MAX_OWN_ACTION_ATTEMPTS} consecutive rejections the own-action window
 * is declared decided to bound the retry loop.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BotDriver {

    /** Max consecutive rejections of an own action before the bot gives up the turn. */
    private static final int MAX_OWN_ACTION_ATTEMPTS = 5;

    private final GameEngine gameEngine;
    private final BotMatchRegistry registry;
    private final BotActionExecutor executor;
    private final LLMBotDecisionProvider llmProvider;
    private final BotFallbackStrategy fallback;
    private final BotProperties botProperties;

    private final Random random = new Random();

    /**
     * Polls a registered match and lets every active bot act once (if eligible).
     */
    public synchronized void processMatch(UUID matchId) {
        if (!registry.isRegistered(matchId)) {
            return;
        }

        GameState state;
        try {
            state = gameEngine.getOrInitialize(matchId);
        } catch (RuntimeException ex) {
            log.warn("Bot driver could not load state for match {}", matchId, ex);
            return;
        }

        if (!active(state)) {
            registry.unregister(matchId);
            log.info("Bot driver unregistered finished match {}", matchId);
            return;
        }

        for (UUID botId : registry.botIds(matchId)) {
            processBot(state, botId);
        }
    }

    private void processBot(GameState state, UUID botId) {
        GamePlayerState bot = findBot(state, botId);
        if (bot == null || bot.getStatus() != PlayerStatus.ACTIVE) {
            return;
        }

        BotMatchRegistry.BotMemory memory = registry.memory(state.getMatchId(), botId);
        WindowCandidate window = resolveWindow(state, bot);
        if (window == null || memory.isDecided(window.key())) {
            return;
        }

        long now = System.currentTimeMillis();
        Long seen = memory.seenAt(window.key());
        if (seen == null) {
            memory.markSeen(window.key(), now);
            return;
        }

        long requiredDelay = window.actorWindow()
                ? Math.max(thinkDelayMs(bot), botProperties.getWindowGraceMs())
                : thinkDelayMs(bot);
        if (now - seen < requiredDelay) {
            return;
        }

        BotDecisionContext context = BotDecisionContext.create(state, bot, window.type());
        BotDecision decision = provider().decide(context);

        BotActionExecutor.ExecutionOutcome outcome =
                executor.execute(state.getMatchId(), botId, decision);

        if (outcome.accepted()) {
            memory.markDecided(window.key());
            return;
        }

        if (window.type() == BotDecisionType.OWN_ACTION) {
            int attempts = memory.incrementAttempt(window.key());
            if (attempts >= MAX_OWN_ACTION_ATTEMPTS) {
                log.warn("Bot '{}' failed its own action {} times in match {} — giving up the turn. Last: {}",
                        bot.getUsername(), attempts, state.getMatchId(), outcome.detail());
                memory.markDecided(window.key());
            } else {
                // Retry on next poll: re-observe the window (fresh think delay),
                // but NEVER consume it — a persistent stall is worse than a loop.
                memory.clearSeen(window.key());
            }
        } else {
            log.debug("Bot '{}' {:s} window consumed after rejection: {}",
                    bot.getUsername(), window.type(), outcome.detail());
            memory.markDecided(window.key());
        }
    }

    /* ------------------------------------------------------------------ */
    /*  Window resolution                                                 */
    /* ------------------------------------------------------------------ */

    private record WindowCandidate(BotDecisionType type, String key, boolean actorWindow) {
    }

    private WindowCandidate resolveWindow(GameState state, GamePlayerState bot) {
        UUID botId = bot.getUserId();
        PendingAction pending = state.getPendingAction();

        // 0. No pending action → own turn?
        if (pending == null) {
            if (botId.equals(state.getCurrentTurnPlayerId()) && !state.isActionExecuted()) {
                return new WindowCandidate(BotDecisionType.OWN_ACTION,
                        "own:" + state.getTurnNumber(), false);
            }
            return null;
        }

        boolean isActor = botId.equals(pending.getActorUserId());

        // 1. Actor window: the bot must resolve / confirm after the grace period,
        //    so opponents always get their contest window first.
        if (isActor) {
            BotDecisionType type = "EXCHANGE".equals(pending.getType())
                    ? BotDecisionType.EXCHANGE_CONFIRM
                    : BotDecisionType.ACTOR_RESOLVE;
            return new WindowCandidate(type, "res:" + pending.getId(), true);
        }

        // 2. A standing block is open to challenge before anything else.
        if (pending.getBlockerUserId() != null && pending.getBlockChallengerUserId() == null) {
            if (botId.equals(pending.getBlockerUserId())) {
                return null; // a blocker cannot challenge its own block
            }
            return new WindowCandidate(BotDecisionType.BLOCK_CHALLENGE,
                    "bc:" + pending.getId(), false);
        }

        // 3. Untouched action: one contest per opponent (challenge or block).
        if (pending.getChallengerUserId() == null && pending.getBlockerUserId() == null) {
            return new WindowCandidate(BotDecisionType.ACTION_CHALLENGE,
                    "win:" + pending.getId(), false);
        }

        // 4. Action already contested (challenged truthfully or blocked-no-challenge) —
        //    nothing left for opponents; the actor resolves.
        return null;
    }

    /* ------------------------------------------------------------------ */
    /*  Decisions / pacing                                                 */
    /* ------------------------------------------------------------------ */

    private BotDecisionProvider provider() {
        return botProperties.isUseLlm() ? llmProvider : fallback;
    }

    private long thinkDelayMs(GamePlayerState bot) {
        if (!botProperties.isThinkDelayEnabled()) {
            return 0;
        }
        BotDifficulty difficulty = BotDifficulty.fromNullable(bot.getBotDifficulty());
        long min = botProperties.minDelayFor(difficulty);
        long max = botProperties.maxDelayFor(difficulty);
        long spread = Math.max(1, max - min);
        return min + Math.abs(random.nextLong() % spread);
    }

    private GamePlayerState findBot(GameState state, UUID botId) {
        return state.getPlayers().stream()
                .filter(player -> player.getUserId().equals(botId))
                .findFirst()
                .orElse(null);
    }

    private boolean active(GameState state) {
        if (state.getStatus() == null) {
            return false;
        }
        String name = state.getStatus().name();
        return "CREATED".equals(name) || "IN_PROGRESS".equals(name);
    }
}