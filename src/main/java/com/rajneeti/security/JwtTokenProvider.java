package com.rajneeti.security;

import com.rajneeti.config.JwtProperties;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Map;

/**
 * Utility class for creating, parsing, and validating JWT tokens.
 *
 * <p>Uses HMAC-SHA-256 (HS256) by default.  The signing key is derived from
 * the {@code jwt.secret} property which must be Base64-encoded and at least
 * 256 bits (32 bytes) long in production.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private final JwtProperties jwtProperties;

    // ──────────────────────────────────────────────────────────────────────────
    // Token Generation
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Generates a signed access token for the given subject (username / user-id).
     *
     * @param subject    typically the username or user UUID
     * @param extraClaims additional claims to embed (roles, email, etc.)
     * @return signed JWT string
     */
    public String generateAccessToken(String subject, Map<String, Object> extraClaims) {
        return buildToken(subject, extraClaims, jwtProperties.getExpirationMs());
    }

    /**
     * Generates a signed refresh token.
     */
    public String generateRefreshToken(String subject) {
        return buildToken(subject, Map.of(), jwtProperties.getRefreshExpirationMs());
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Token Parsing
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Extracts the subject (username) from a token without verifying expiry.
     * Always call {@link #validateToken(String)} first in authentication flows.
     */
    public String extractSubject(String token) {
        return parseClaims(token).getSubject();
    }

    /**
     * Returns true if the token signature is valid and the token has not expired.
     */
    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (ExpiredJwtException ex) {
            log.warn("JWT token is expired: {}", ex.getMessage());
        } catch (UnsupportedJwtException ex) {
            log.warn("JWT token is unsupported: {}", ex.getMessage());
        } catch (MalformedJwtException ex) {
            log.warn("JWT token is malformed: {}", ex.getMessage());
        } catch (SignatureException ex) {
            log.warn("JWT signature is invalid: {}", ex.getMessage());
        } catch (IllegalArgumentException ex) {
            log.warn("JWT claims string is empty: {}", ex.getMessage());
        }
        return false;
    }

    /**
     * Returns the expiry date embedded in the token.
     */
    public Date extractExpiration(String token) {
        return parseClaims(token).getExpiration();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Private Helpers
    // ──────────────────────────────────────────────────────────────────────────

    private String buildToken(String subject, Map<String, Object> extraClaims, long ttlMs) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .claims(extraClaims)
                .subject(subject)
                .issuedAt(new Date(now))
                .expiration(new Date(now + ttlMs))
                .signWith(signingKey(), Jwts.SIG.HS256)
                .compact();
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey signingKey() {
        // If the secret already looks like Base64, decode it; otherwise treat as raw UTF-8 bytes
        try {
            byte[] keyBytes = Decoders.BASE64.decode(jwtProperties.getSecret());
            return Keys.hmacShaKeyFor(keyBytes);
        } catch (Exception ex) {
            // Fallback – raw string (dev only; production must use Base64 encoded ≥256-bit secret)
            return Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes());
        }
    }
}
