package com.rajneeti.bot.driver;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Module 25 — schedules the single bot-driving loop. Processing is
 * single-threaded by design (Spring's {@code @Scheduled} runs each task on one
 * thread), which serializes all bot-vs-bot decisions within one match.
 *
 * <p>Each poll unregisters matches that ended so the loop stops touching them.
 * A failed match is skipped (and left registered) so a transient engine error
 * cannot silently stop driving a live game.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BotScheduler {

    private final BotMatchRegistry registry;
    private final BotDriver botDriver;

    @Scheduled(fixedDelayString = "${bot.scheduler-interval-ms:500}")
    public void driveBots() {
        for (UUID matchId : registry.registeredMatches()) {
            try {
                botDriver.processMatch(matchId);
            } catch (RuntimeException ex) {
                log.warn("Bot driver failed to poll match {}", matchId, ex);
            }
        }
    }
}