package com.rajneeti.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Authoritative window durations for the backend game engine.
 *
 * <p>Every block and challenge window is owned by the server: when an action is
 * declared the engine schedules its window to expire, and the expiry itself is
 * what resolves the action. Clients only display these durations, so the browser
 * can never be responsible for a transition.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "game.timers")
public class GameTimerProperties {

    /** How long opponents may block a blockable action (Foreign Aid, Steal, Assassinate). */
    private int blockWindowSeconds = 15;

    /** How long opponents may challenge a claim (Tax, Exchange, Assassinate). */
    private int challengeWindowSeconds = 15;
}
