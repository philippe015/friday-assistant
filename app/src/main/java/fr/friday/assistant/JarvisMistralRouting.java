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
        return text.contains("https://") || text.contains("http://")
                || text.contains("www.") || text.contains("actualite")
                || text.contains("meteo") || text.contains("prevision meteorologique")
                || text.contains("internet") || text.contains("sur le web")
                || text.contains("recherche en ligne")
                || text.contains("trafic routier") || text.contains("cours de la bourse")
                || text.contains("taux de change");
    }

    public static boolean useLocal(String question, boolean enabled, boolean allGeneral) {
        if (!LocalJarvisPolicy.shouldRoute(question, enabled, allGeneral)) return false;
        // Un ordre explicite « Jarvis local... » reste hors réseau même s'il
        // mentionne un sujet d'actualité. Aucune remontée cachée vers Mistral.
        return LocalJarvisPolicy.explicit(question) || !needsOnline(question);
    }
}
