package fr.friday.assistant;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Demande de clarification des listes électorales ambiguës. Une liste de
 * candidats dépend du pays, de l'année et du stade de validation officielle.
 * Ne renvoie aucun nom supposé et n'utilise pas Internet.
 */
public final class ElectionQuestionPolicy {
    private ElectionQuestionPolicy() {}

    private static final Pattern CANDIDATES = Pattern.compile(
            "\\b(?:candidats?|candidates?|candidatures?)\\b");
    private static final Pattern PRESIDENTIAL = Pattern.compile(
            "\\b(?:presidentielle?s?|election presidentielle|elections presidentielles)\\b");
    private static final Pattern YEAR = Pattern.compile("\\b(?:19|20)\\d{2}\\b");
    private static final Pattern COUNTRY = Pattern.compile(
            "\\b(?:france|francais|francaise|etats unis|americain|americaine|"
            + "russie|russe|ukraine|ukrainien|ukrainienne|"
            + "pologne|polonais|portugal|portugais|italie|italien|"
            + "allemagne|allemand|bresil|bresilien|mexique|mexicain|"
            + "tunisie|tunisien|algerie|algerien|maroc|marocain|"
            + "senegal|senegalais|cameroun|camerounais|gabon|gabonais|"
            + "cote d ivoire|ivoirien|roumanie|roumain|"
            + "inde|indien|argentine|argentin|turquie|turc)\\b");

    private static String fold(String text) {
        return Normalizer.normalize(text == null ? "" : text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").toLowerCase(Locale.FRENCH)
                .replaceAll("[^a-z0-9]+", " ").replaceAll("\\s+", " ").trim();
    }

    public static String clarification(String question) {
        String q = fold(question);
        if (q.length() < 16 || q.length() > 220
                || !CANDIDATES.matcher(q).find()
                || !PRESIDENTIAL.matcher(q).find()) return null;
        boolean hasYear = YEAR.matcher(q).find();
        boolean hasCountry = COUNTRY.matcher(q).find();
        if (hasCountry && hasYear) return null;
        if (!hasCountry && !hasYear)
            return "Pour quelle élection présidentielle souhaitez-vous la liste "
                    + "des candidats ? Précisez le pays et l'année, par exemple "
                    + "« candidats à la présidentielle française de 2027 ». "
                    + "Je distinguerai les personnes déclarées des candidatures "
                    + "officiellement validées.";
        return hasCountry
                ? "Pour quelle année souhaitez-vous la liste des candidats "
                  + "à cette élection présidentielle ?"
                : "Dans quel pays se déroule cette élection présidentielle ? "
                  + "Précisez le pays afin de vérifier les candidats.";
    }
}
