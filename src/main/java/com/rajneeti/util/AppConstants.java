package com.rajneeti.util;

/**
 * Application-wide string constants.
 *
 * <p>Centralises magic strings for API paths, roles, claim keys, etc.
 * so that a rename is a single-file change.
 */
public final class AppConstants {

    private AppConstants() {
        throw new UnsupportedOperationException("Utility class – do not instantiate");
    }

    // ── API Versioning ──────────────────────────────────────────────────────
    public static final String API_BASE    = "/api";
    public static final String API_V1      = API_BASE + "/v1";

    // ── JWT Claim Keys ───────────────────────────────────────────────────────
    public static final String CLAIM_ROLES     = "roles";
    public static final String CLAIM_EMAIL     = "email";
    public static final String CLAIM_USER_ID   = "userId";

    // ── Roles ────────────────────────────────────────────────────────────────
    public static final String ROLE_PLAYER = "ROLE_PLAYER";
    public static final String ROLE_ADMIN  = "ROLE_ADMIN";

    // ── Pagination Defaults ──────────────────────────────────────────────────
    public static final int    DEFAULT_PAGE_SIZE   = 20;
    public static final int    MAX_PAGE_SIZE        = 100;
    public static final String DEFAULT_SORT_FIELD  = "createdAt";

    // ── WebSocket Topics ─────────────────────────────────────────────────────
    public static final String TOPIC_LOBBY  = "/topic/lobby";
    public static final String TOPIC_GAME   = "/topic/game/";   // append roomId
}
