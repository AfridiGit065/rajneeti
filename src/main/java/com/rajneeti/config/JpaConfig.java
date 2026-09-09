package com.rajneeti.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * JPA Configuration enabling Spring Data JPA auditing.
 */
@Configuration
@EnableJpaAuditing
public class JpaConfig {
}