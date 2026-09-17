package com.rajneeti.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Module 25 — enables Spring's scheduling infrastructure needed by the
 * {@link com.rajneeti.bot.driver.BotScheduler} loop.
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}