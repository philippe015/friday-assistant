package fr.friday.assistant;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Identité locale du profil, indépendante des habitudes d'auto-présentation du modèle. */
public final class AssistantIdentity {
    private AssistantIdentity() {}

    private static final Pattern IDENTITY_QUESTION = Pattern.compile(
            "^(?:friday |frida |freiday )?(?:qui es tu|qui etes vous|"
            + "comment tu t appelles|comment t appelles tu|comment t appelle tu|"
            + "quel est ton nom|c est quoi ton nom|tu t appelles comment|"
            + "presente toi|quelle est ton identite|"
            + "es tu mistral|est ce que tu es mistral)"
            + "(?: s il te plait)?$");

    private static final Pattern PROMPT_NAME = Pattern.compile(
            "(?iu)\\b(?:tu\\s+t['’]appelles|ton\\s+nom\\s+est|tu\\s+te\\s+nommes)"
            + "\\s+([\\p{L}][\\p{L}0-9._-]{0,44})");

    private static String clean(String raw) {
        if (raw == null) return "";
        return Normalizer.normalize(raw, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.FRENCH)
                .replaceAll("[^a-z0-9]+", " ").replaceAll("\\s+", " ").trim();
    }

    public static String displayName(String profileName, String profilePrompt) {
        if (profilePrompt != null) {
            Matcher m = PROMPT_NAME.matcher(profilePrompt);
            if (m.find()) return m.group(1);
        }
        String name = profileName == null ? "" : profileName.trim();
        if (name.isEmpty() || name.equalsIgnoreCase("Expert technique")
                || name.equalsIgnoreCase("Assistant pragmatique")
                || name.equalsIgnoreCase("Réponses détaillées"))
            return "F.R.I.D.A.Y.";
        return name;
    }

    public static String reply(String question, String profileName, String profilePrompt) {
        if (!IDENTITY_QUESTION.matcher(clean(question)).matches()) return null;
        String assistant = displayName(profileName, profilePrompt);
        return "Je suis " + assistant + ", votre assistant personnel. "
                + "Mon comportement est défini par le prompt du profil actif « "
                + (profileName == null || profileName.trim().isEmpty() ? "F.R.I.D.A.Y." : profileName.trim())
                + " ». Mistral est uniquement le moteur d'intelligence artificielle que j'utilise.";
    }
}
