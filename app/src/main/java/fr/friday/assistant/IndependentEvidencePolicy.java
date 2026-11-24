package fr.friday.assistant;

import java.net.URI;
import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * No model can authorize an Internet lookup of private dialogue.
 *
 * This conservative, testable policy permits only an explicit user's
 * source-check request for a public, factual and non-sensitive question.
 * It does not infer consent from an "always Web" preference.
 */
public final class IndependentEvidencePolicy {
    private IndependentEvidencePolicy() { }

    private static final Pattern SENSITIVE = Pattern.compile(
            "(?:^| )(?:je|j|moi|me|mon|ma|mes|ton|ta|tes|notre|nos|votre|vos|"
            + "personnel|prive|privee|confidentiel|confidentielle|"
            + "memoire|conversations|historique|souvenirs|"
            + "identifiant|identifiants|mot de passe|secret|"
            + "adresse|coordonnees|telephone|numero de|"
            + "compte|bancaire|dossier|medical|medicale|"
            + "patient|traitement|sante|gps|localisation|position)(?: |$)");
    private static final Pattern PUBLIC_FACT = Pattern.compile(
            "^(?:qui|quoi|que|quel|quelle|quels|quelles|"
            + "quand|comment|pourquoi|ou|combien|est ce que|"
            + "explique|decris|compare|recherche|cherche|"
            + "verifie|donne moi des informations|actualites|"
            + "dernieres actualites|meteo)\\b.*");
    private static final Pattern OFFLINE = Pattern.compile(
            ".*(?:sans internet|sans web|hors ligne|"
            + "sans connexion|ne recherche pas|n utilise pas internet|"
            + "uniquement en local|reste en local).*");
    private static final Pattern LONG_NUMBER = Pattern.compile(".*\\b\\d{7,}\\b.*");

    private static String fold(String value) {
        return Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").toLowerCase(Locale.FRENCH)
                .replaceAll("[^a-z0-9]+", " ").replaceAll("\\s+", " ").trim()
                .replaceFirst("^(?:jarvis|friday|fraday|jervis)\\s+", "");
    }

    public static boolean eligible(String question, boolean internetEnabled,
                                   boolean explicitlyRequested) {
        if (!internetEnabled || !explicitlyRequested || question == null
                || question.length() < 12 || question.length() > 260)
            return false;
        String folded = fold(question);
        if (folded.length() < 12 || SENSITIVE.matcher(folded).find()
                || OFFLINE.matcher(folded).matches()
                || LONG_NUMBER.matcher(folded).matches()
                || question.contains("@") || question.contains("://")
                || question.toLowerCase(Locale.ROOT).contains("www."))
            return false;
        return PUBLIC_FACT.matcher(folded).matches();
    }

    /** Categorisation is a hint, never a certificate of factual correctness. */
    public static boolean institutionalCandidate(String url) {
        try {
            URI uri = URI.create(url);
            if (!"https".equalsIgnoreCase(uri.getScheme())
                    || uri.getHost() == null || uri.getUserInfo() != null)
                return false;
            String h = uri.getHost().toLowerCase(Locale.ROOT);
            return h.endsWith(".gouv.fr") || h.endsWith(".gov")
                    || h.endsWith(".gov.uk") || h.endsWith(".europa.eu")
                    || h.equals("europa.eu") || h.equals("who.int")
                    || h.endsWith(".who.int") || h.equals("nasa.gov")
                    || h.endsWith(".nasa.gov") || h.equals("insee.fr")
                    || h.endsWith(".insee.fr") || h.equals("cnil.fr")
                    || h.endsWith(".cnil.fr");
        } catch (Exception invalid) { return false; }
    }

    /** Only HTTPS public URLs, no credentials or local/IP literal hosts. */
    public static boolean safeReference(String url) {
        try {
            URI uri = URI.create(url);
            if (!"https".equalsIgnoreCase(uri.getScheme())
                    || uri.getUserInfo() != null || uri.getHost() == null
                    || uri.getPort() != -1) return false;
            String host = uri.getHost().toLowerCase(Locale.ROOT);
            return host.contains(".") && !host.equals("localhost")
                    && !host.endsWith(".local") && !host.endsWith(".internal")
                    && !host.matches("[0-9.]+")
                    && !host.contains(":");
        } catch (Exception invalid) { return false; }
    }
}
