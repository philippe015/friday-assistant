package fr.friday.assistant;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

/** Politique réseau testable hors Android. Pas de retry automatique des erreurs non transitoires. */
public final class HttpRetryPolicy {
    public static final int MAX_ATTEMPTS = 3;
    private HttpRetryPolicy() {}

    public static boolean retryable(int status) {
        return status == 429 || status == 500 || status == 502 ||
                status == 503 || status == 504;
    }
    /** Retry-After en secondes ou date HTTP. Borne à 5 s pour le dialogue vocal. */
    public static long retryAfterMs(String value, long nowMillis) {
        if (value == null || value.trim().isEmpty()) return -1L;
        try { return Math.min(5000L, Math.max(0L, Long.parseLong(value.trim()) * 1000L)); }
        catch (NumberFormatException ignored) {}
        try {
            long expiry = ZonedDateTime.parse(value.trim(), DateTimeFormatter.RFC_1123_DATE_TIME)
                    .toInstant().toEpochMilli();
            return Math.min(5000L, Math.max(0L, expiry - nowMillis));
        } catch (Exception ignored) { return -1L; }
    }
    public static long delayMs(int failedAttempt, String retryAfter, long nowMillis) {
        long instructed = retryAfterMs(retryAfter, nowMillis);
        if (instructed >= 0) return instructed;
        long base = failedAttempt <= 1 ? 450L : 1050L;
        return base + ThreadLocalRandom.current().nextLong(0, 250L);
    }
}
