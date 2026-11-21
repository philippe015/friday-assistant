package fr.friday.assistant;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Extraits publics du flux RSS : compléments facultatifs, jamais une preuve
 * d'événement, jamais un résumé imaginé par le modèle.
 */
public final class NewsEvidencePolicy {
    private NewsEvidencePolicy() {}

    private static String fold(String text) {
        return Normalizer.normalize(text == null ? "" : text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").toLowerCase(Locale.FRENCH)
                .replaceAll("[^a-z0-9]+", " ").replaceAll("\\s+", " ").trim();
    }

    public static String distinctRssExcerpt(String title, String summary, String url) {
        if (summary == null || summary.trim().isEmpty()) return "";
        // Les descriptions de Google News agrègent plusieurs articles et ne
        // correspondent pas à une synthèse du texte d'un article identifiable.
        if (url != null && url.matches("(?i)^https://news\\.google\\.com/.*")) return "";
        String clean = summary.replaceAll("[\\r\\n\\t]+", " ")
                .replaceAll("(?i)^Média indiqué par le flux\\s*:\\s*[^.]{1,65}\\.\\s*", "")
                .replaceAll("\\s+", " ").trim();
        if (clean.length() < 45) return "";
        String t = fold(title);
        String s = fold(clean);
        if (s.equals(t) || s.startsWith(t) || t.startsWith(s)) return "";
        // Les annonces et ordres intégrés dans un flux ne sont ni des faits
        // vérifiés ni des instructions pour l'assistant.
        if (s.contains("ignore les instructions") || s.contains("mot de passe")
                || s.contains("cle api") || s.contains("prompt systeme"))
            return "";
        return clean.length() <= 190 ? clean : clean.substring(0, 189).trim() + "…";
    }
}
