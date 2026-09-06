package com.rajneeti;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;

import java.time.ZoneId;
import java.util.Arrays;
import java.util.TimeZone;

/**
 * RAJNEETI – Main Application Entry Point
 *
 * <p>Bootstraps the Spring Boot application. Sets the default JVM timezone to UTC
 * before any Spring infrastructure is initialised so that all date/time operations
 * (Hibernate, Jackson, etc.) are timezone-consistent.
 */
@Slf4j
@SpringBootApplication
public class RajneetiApplication {

    private final Environment environment;

    public RajneetiApplication(Environment environment) {
        this.environment = environment;
    }

    public static void main(String[] args) {
        // Force UTC timezone for the entire JVM before Spring starts
        TimeZone.setDefault(TimeZone.getTimeZone(ZoneId.of("UTC")));
        SpringApplication.run(RajneetiApplication.class, args);
    }

    /**
     * Logs a startup banner after all beans are wired and the server is ready.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        String port    = environment.getProperty("server.port", "8080");
        String profile = String.join(", ", Arrays.asList(environment.getActiveProfiles()));

        log.info("""

                ╔══════════════════════════════════════════════════════╗
                ║          R A J N E E T I  –  রাজনীতি               ║
                ║      Real-Time Multiplayer Bluff Strategy Game       ║
                ╠══════════════════════════════════════════════════════╣
                ║  Port    : {}
                ║  Profile : {}
                ║  Health  : http://localhost:{}/api/health
                ╚══════════════════════════════════════════════════════╝
                """, port, profile.isBlank() ? "default" : profile, port);
    }
}
