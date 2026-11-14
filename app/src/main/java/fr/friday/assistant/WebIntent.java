package fr.friday.assistant;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/** Ne lancer un agent Web payant que pour une requête nécessitant de l'actualité. */
public final class WebIntent {
    private WebIntent() {}
    private static final Pattern CURRENT = Pattern.compile(
        "\\b(?:actualit[eé]s?|infos? du jour|derni[eè]res? nouvelles?|aujourd.hui|en ce moment|"
      + "maintenant|cette semaine|ce mois|r[eé]cent(?:e|es|s)?|"
      + "m[eé]t[eé]o|quel temps fait|temp[eé]rature|pr[eé]visions?|trafic|cours de|bourse|"
      + "prix (?:actuel|du jour)|taux de change|r[eé]sultats? (?:du|de|des)|"
      + "classements?|horaires? (?:actuels?|d.ouverture)|"
      + "cherche(?:r)? (?:sur )?(?:internet|le web)|recherche (?:sur )?(?:internet|le web)|"
      + "v[eé]rifie (?:en ligne|sur internet)|qui est (?:l.actuel|le nouveau)|"
      + "quelle est la derni[eè]re)\\b", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    public static boolean needsWeb(String question) {
        if (question == null || question.trim().isEmpty()) return false;
        String normalized = Normalizer.normalize(question, Normalizer.Form.NFC).toLowerCase(Locale.FRENCH);
        return CURRENT.matcher(normalized).find();
    }
}
