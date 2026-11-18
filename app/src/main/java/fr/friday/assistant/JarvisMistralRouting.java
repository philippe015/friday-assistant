package fr.friday.assistant;

import java.text.Normalizer;
import java.util.Locale;

/** Deux moteurs uniquement : IA locale pour demandes ordinaires, Mistral pour le Web. */
public final class JarvisMistralRouting {
    private JarvisMistralRouting() {}

    public static boolean needsOnline(String question) {
        if (question == null) return false;
        String text = Normalizer.normalize(question, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").toLowerCase(Locale.FRENCH);
        if (RecentNewsPolicy.requiresRecentNews(question)) return true;
        return text.matches("(?s).*(?:https?://|www\\.|\\bactualites?\\b|"
                + "\bmeteo\\b|\bprevisions? meteorologiques?\\b|\binternet\\b|"
                + "\bsur le web\\b|\bsur internet\\b|\brecherche (?:en ligne|sur le web)\\b|"
                + "\btrafic routier\\b|\bcours de la bourse\\b|\btaux de change\\b).*");
    }

    public static boolean useLocal(String question, boolean enabled, boolean allGeneral) {
        if (!LocalJarvisPolicy.shouldRoute(question, enabled, allGeneral)) return false;
        // Un ordre explicite « Jarvis local... » reste hors réseau même s'il
        // mentionne un sujet d'actualité. Aucune remontée cachée vers Mistral.
        return LocalJarvisPolicy.explicit(question) || !needsOnline(question);
    }
}
