package com.rajneeti.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Strongly-typed JWT configuration properties.
 *
 * <p>Values are bound from {@code application.yml} under the {@code jwt} prefix.
 * All secrets are read from environment variables – never hardcoded.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    /** HMAC-SHA signing secret – must be at least 256 bits in production. */
    private String secret;

    /** Access token TTL in milliseconds (default 24 h). */
    private long expirationMs = 86_400_000L;

    /** Refresh token TTL in milliseconds (default 7 days). */
    private long refreshExpirationMs = 604_800_000L;
}
