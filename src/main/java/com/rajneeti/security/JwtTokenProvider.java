package com.rajneeti.security;

import com.rajneeti.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Utility component for generating, signing, and validating JSON Web Tokens (JWT).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private final JwtProperties jwtProperties;

    /**
     * Generates an access token for an authenticated {@link UserPrincipal}.
     */
    public String generateAccessToken(UserPrincipal userPrincipal) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userPrincipal.getId().toString());
        claims.put("email", userPrincipal.getEmail());
        return buildToken(userPrincipal.getUsername(), claims, jwtProperties.getExpirationMs());
    }

    /**
     * Generates an access token with custom subject and claims.
     */
    public String generateAccessToken(String subject, Map<String, Object> extraClaims) {
        return buildToken(subject, extraClaims, jwtProperties.getExpirationMs());
    }

    /**
     * Generates a signed refresh token string with extended TTL.
     */
    public String generateRefreshToken(String subject) {
        return buildToken(subject, Map.of(), jwtProperties.getRefreshExpirationMs());
    }

    /**
     * Extracts the subject (username) from the token.
     */
    public String extractSubject(String token) {
        return parseClaims(token).getSubject();
    }

    /**
     * Extracts the user UUID claim from the token.
     */
    public UUID extractUserId(String token) {
        String userIdStr = parseClaims(token).get("userId", String.class);
        return userIdStr != null ? UUID.fromString(userIdStr) : null;
    }

    /**
     * Validates whether the token signature is correct and not expired.
     * Note: Never logs token payload or token string.
     */
    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (ExpiredJwtException ex) {
            log.warn("JWT token has expired");
        } catch (UnsupportedJwtException ex) {
            log.warn("JWT token format is unsupported");
        } catch (MalformedJwtException ex) {
            log.warn("JWT token is malformed");
        } catch (SignatureException ex) {
            log.warn("JWT signature validation failed");
        } catch (IllegalArgumentException ex) {
            log.warn("JWT claims string is empty or invalid");
        }
        return false;
    }

    /**
     * Extracts token expiration timestamp.
     */
    public Date extractExpiration(String token) {
        return parseClaims(token).getExpiration();
    }

    /**
     * Returns the access token expiration in seconds.
     */
    public long getExpirationInSeconds() {
        return jwtProperties.getExpirationMs() / 1000L;
    }

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
        try {
            byte[] keyBytes = Decoders.BASE64.decode(jwtProperties.getSecret());
            return Keys.hmacShaKeyFor(keyBytes);
        } catch (Exception ex) {
            return Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes());
        }
    }
}