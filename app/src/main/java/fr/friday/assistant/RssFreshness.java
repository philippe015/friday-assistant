package fr.friday.assistant;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Locale;

/** Horodatages RSS : ne pas confondre une page indexée avec une actualité récente. */
public final class RssFreshness {
    private RssFreshness() { }

    public static long publishedMillis(String rssDate) {
        if (rssDate == null || rssDate.trim().isEmpty()) return 0;
        try {
            return ZonedDateTime.parse(rssDate.trim(),
                    DateTimeFormatter.RFC_1123_DATE_TIME).toInstant().toEpochMilli();
        } catch (DateTimeParseException notRfc1123) {
            // Flux RDF/Atom internationaux : <dc:date> et <updated>
            // publient couramment des horodatages ISO-8601.
            try {
                return Instant.parse(rssDate.trim()).toEpochMilli();
            } catch (DateTimeParseException notInstant) {
                try {
                    return java.time.OffsetDateTime.parse(rssDate.trim())
                            .toInstant().toEpochMilli();
                } catch (DateTimeParseException invalid) {
                    return 0;
                }
            }
        }
    }

    public static boolean withinHours(long publishedAt, long now, int hours) {
        if (hours <= 0 || hours > 48 || publishedAt <= 0) return false;
        // Accepter un décalage d'horloge mineur, jamais une source du futur lointain.
        return publishedAt >= now - hours * 3600_000L
                && publishedAt <= now + 5 * 60_000L;
    }

    public static String display(long publishedAt) {
        if (publishedAt <= 0) return "date de publication non vérifiée";
        return DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm z", Locale.FRANCE)
                .withZone(ZoneId.systemDefault())
                .format(Instant.ofEpochMilli(publishedAt));
    }
}
