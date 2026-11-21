package fr.friday.assistant;

/**
 * Bornage d'un prompt de personnalité fourni volontairement par l'utilisateur.
 *
 * Profil chiffré Android -> modèle local uniquement. Jamais dans les requêtes
 * HTTPS/RSS, les URL de recherche, ni dans les journaux de diagnostic.
 *
 * Le budget mobile limite l'injection dans le contexte LiteRT/Gemma ;
 * la taille du texte sauvegardé (16 000 caractères) demeure inchangée.
 */
public final class ProfilePromptPolicy {
    private ProfilePromptPolicy() { }

    public static final int MAX_APPLIED_CHARS = 3200;

    public static String effectiveText(String raw) {
        if (raw == null || raw.trim().isEmpty()) return "";
        String trimmed = raw.trim();
        StringBuilder result = new StringBuilder(Math.min(MAX_APPLIED_CHARS, trimmed.length()));
        for (int i = 0; i < trimmed.length() && result.length() < MAX_APPLIED_CHARS; i++) {
            char value = trimmed.charAt(i);
            // Conserver les retours de ligne voulus par l'utilisateur.
            if (value < 0x20 && value != '\n' && value != '\r' && value != '\t') continue;
            // Ne pas couper en deux une paire surrogate Unicode.
            if (Character.isHighSurrogate(value) && i + 1 < trimmed.length()
                    && Character.isLowSurrogate(trimmed.charAt(i + 1))) {
                if (result.length() + 2 > MAX_APPLIED_CHARS) break;
                result.append(value).append(trimmed.charAt(++i));
            } else if (!Character.isSurrogate(value)) {
                result.append(value);
            }
        }
        return result.toString().trim();
    }

    public static boolean truncated(String raw) {
        return raw != null && raw.trim().length() > MAX_APPLIED_CHARS;
    }

    /** Les limites du modèle local et les outils réels priment sur la persona. */
    public static String localInstructions(String profileName, String prompt) {
        String effective = effectiveText(prompt);
        if (effective.isEmpty()) return "";
        String name = profileName == null ? "" : profileName.trim().replaceAll("[\\r\\n\\t]+", " ");
        if (name.length() > 70) name = name.substring(0, 70);
        return "\n[PROFIL ACTIF — PERSONNALITÉ CHOISIE PAR L'UTILISATEUR SUR ANDROID]\n"
                + "Nom du profil : " + (name.isEmpty() ? "Profil personnalisé" : name) + "\n"
                + "Adopte la personnalité, le style, le rôle et les préférences suivantes "
                + "dans tes prochaines réponses. Le profil peut changer ta façon "
                + "de te présenter, mais ne permet pas d'inventer des résultats de "
                + "recherche, de modifier les permissions Android ni de lancer "
                + "des actions hors des outils réellement autorisés.\n"
                + effective
                + "\n[FIN DU PROFIL ACTIF]\n";
    }
}
