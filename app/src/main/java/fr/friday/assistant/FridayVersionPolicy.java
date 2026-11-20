package fr.friday.assistant;

/** Identifiants publiés par les anciennes éditions Android de JARVIS */
final class FridayVersionPolicy {
    static final String[] KNOWN_PACKAGES = {
        "fr.friday.assistant",
        "fr.friday.assistant.preview",
        "fr.friday.assistant.smartpreview"
    };
    private FridayVersionPolicy() { }

    static boolean isOtherVersion(String ownPackage, String candidate) {
        if (ownPackage == null || candidate == null || ownPackage.equals(candidate)) return false;
        for (String known : KNOWN_PACKAGES) if (known.equals(candidate)) return true;
        return false;
    }

    static String warning(int count) {
        if (count <= 0) return "Aucune autre édition connue de JARVIS détectée.";
        return count + " autre(s) édition(s) JARVIS installée(s). " +
            "Android ne permet pas de confirmer ici si leur microphone écoute. " +
            "Arrêtez leur écoute avant de démarrer celle-ci.";
    }
}
