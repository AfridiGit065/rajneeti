package com.rajneeti.util;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * Utility methods for date and time operations.
 *
 * <p>All timestamps in RAJNEETI are stored in UTC and converted to the user's
 * local timezone on the client side. Never store timezone-aware timestamps
 * in the database without a clear contract.
 */
public final class DateTimeUtil {

    private static final DateTimeFormatter ISO_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").withZone(ZoneId.of("UTC"));

    private DateTimeUtil() {
        throw new UnsupportedOperationException("Utility class – do not instantiate");
    }

    /** Returns the current UTC instant. */
    public static Instant nowUtc() {
        return Instant.now();
    }

    /** Returns the current UTC time as a {@link LocalDateTime}. */
    public static LocalDateTime nowLocalUtc() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }

    /** Formats an {@link Instant} as an ISO-8601 UTC string. */
    public static String formatUtc(Instant instant) {
        return ISO_FORMATTER.format(instant);
    }
}
