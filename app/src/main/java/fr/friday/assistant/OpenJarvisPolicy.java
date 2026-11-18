package fr.friday.assistant;

import java.net.URI;
import java.util.Locale;

/**
 * FRIDAY ↔ OpenJarvis bridge policy (new Android code).
 * OpenJarvis source: https://github.com/open-jarvis/OpenJarvis
 * Apache 2.0. No upstream binaries or sources are embedded in this APK.
 */
public final class OpenJarvisPolicy {
    private OpenJarvisPolicy() { }

    /**
     * Authenticated TLS endpoint only. The desktop service MUST be behind an
     * HTTPS reverse proxy with a valid certificate (never ignore TLS errors).
     * "localhost" from Android means the phone, not the user's Linux PC.
     */
    public static String baseUrl(String entered) {
        if (entered == null || entered.trim().isEmpty())
            throw new IllegalArgumentException("Adresse HTTPS OpenJarvis manquante.");
        String value = entered.trim();
        if (value.length() > 256)
            throw new IllegalArgumentException("Adresse OpenJarvis trop longue.");
        try {
            URI uri = new URI(value);
            if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null
                    || uri.getHost().trim().isEmpty()
                    || uri.getRawUserInfo() != null || uri.getRawQuery() != null
                    || uri.getRawFragment() != null)
                throw new IllegalArgumentException("Utilisez une adresse https:// sans compte, paramètres ou fragment.");
            String host = uri.getHost().toLowerCase(Locale.ROOT);
            if ("localhost".equals(host) || "0.0.0.0".equals(host)
                    || "127.0.0.1".equals(host) || "::1".equals(host)
                    || "[::1]".equals(host) || host.endsWith(".localhost"))
                throw new IllegalArgumentException("Sur Android, localhost désigne le téléphone. Utilisez l'adresse HTTPS du serveur Manjaro.");
            String path = uri.getPath();
            if (path == null) path = "";
            if (!path.isEmpty() && !"/".equals(path) && !"/v1".equals(path) && !"/v1/".equals(path))
                throw new IllegalArgumentException("Indiquez l'adresse du serveur, pas le chemin d'une requête.");
            if (uri.getPort() == 0 || uri.getPort() > 65535)
                throw new IllegalArgumentException("Port OpenJarvis invalide.");
            String authority = uri.getRawAuthority();
            if (authority == null || authority.isEmpty())
                throw new IllegalArgumentException("Adresse de serveur invalide.");
            // A colon inside a bracketed IPv6 address is valid. No URL user-info.
            return "https://" + authority;
        } catch (java.net.URISyntaxException e) {
            throw new IllegalArgumentException("Adresse HTTPS OpenJarvis invalide.", e);
        }
    }

    public static String completionsUrl(String base) {
        return baseUrl(base) + "/v1/chat/completions";
    }

    public static String healthUrl(String base) {
        return baseUrl(base) + "/health";
    }

    public static String model(String value) {
        String v = value == null ? "" : value.trim();
        if (!v.matches("[a-zA-Z0-9_./:@-]{1,96}"))
            throw new IllegalArgumentException("Nom du modèle OpenJarvis invalide.");
        return v;
    }

    /** Off until configured; route all only with a separate explicit opt-in. */
    public static boolean shouldRoute(String question, boolean enabled, boolean routeAll) {
        if (!enabled || question == null || question.trim().isEmpty()) return false;
        if (routeAll) return true;
        String q = VoiceText.normalize(question).trim().replaceAll("\\s+", " ");
        q = q.replaceFirst("^(?:friday|frida|freiday|freday|fride)[, :;.!?-]*", "").trim();
        return q.matches("^(?:jarvis|open ?jarvis)(?:\\b.*)?")
                || q.matches("^(?:demande|pose la question) (?:a |au )?(?:open ?)?jarvis\\b.*");
    }

    public static String cleanQuestion(String question) {
        if (question == null) return "";
        // Preserve text as spoken (no rewrite of names/meaning).
        String q = question.trim();
        q = q.replaceFirst("(?iu)^(?:friday|frida|freiday|freday|fride)[, :;.!?-]*", "").trim();
        q = q.replaceFirst("(?iu)^(?:demande|pose la question) (?:à |a |au )?(?:open ?)?jarvis[, :;.!?-]*", "").trim();
        q = q.replaceFirst("(?iu)^(?:open ?jarvis|jarvis)[, :;.!?-]*", "").trim();
        return q.isEmpty() ? question.trim() : q;
    }
}
