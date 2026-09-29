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

    /**
     * How long the actor has to finish an Exchange by choosing which 2 of the 4
     * pooled cards to keep.
     *
     * <p>An Exchange is the one window whose outcome depends on an input only the
     * actor can supply, so it is never resolved generically: when this deadline
     * passes without a confirmation the Exchange is <em>cancelled</em> — the two
     * drawn cards go back to the deck, the hand is restored and the turn advances.
     * Nothing is ever chosen automatically on the actor's behalf.
     */
    private int exchangeDecisionWindowSeconds = 60;
}
