package fr.friday.assistant;

import java.util.Locale;

/** Décide si une question justifie une recherche Web (évite les appels inutiles, facturés). */
public final class WebSearch {

    private static final String[] HINTS = {
            "actualité", "actualite", "aujourd'hui", "aujourd hui", "ce matin", "ce soir",
            "cette semaine", "cette année", "cette annee", "dernière", "derniere", "dernières", "dernieres",
            "nouveau", "nouvelle", "nouveauté", "nouveaute", "sortie", "sort ",
            "prix", "coût", "cout", "combien coûte", "combien coute", "tarif",
            "météo", "meteo", "température", "temperature",
            "news", "info", "infos", "recherche", "internet", "web", "en ligne",
            "qui est", "qui a ", "quand a ", "combien de", "classement", "match", "score",
            "population", "élection", "election", "bourse", "bitcoin", "action ",
            "2025", "2026", "2027", "récent", "recent"
    };

    private WebSearch() {}

    public static boolean needsWeb(String question) {
        if (question == null) return false;
        String s = question.toLowerCase(Locale.ROOT);
        if (s.contains("http://") || s.contains("https://")) return true;
        for (String h : HINTS) {
            if (s.contains(h)) return true;
        }
        return false;
    }
}
