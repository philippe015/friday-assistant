package fr.friday.assistant;

/** Identifiants publiés par les anciennes éditions Android de F.R.I.D.A.Y. */
final class FridayVersionPolicy {
    static final String[] KNOWN_PACKAGES = {
        "fr.friday.assistant",
        "fr.friday.assistant.preview",
        "fr.friday.assistant.smartpreview",
        "fr.friday.assistant.readerfixpreview",
        "fr.friday.assistant.v31418preview",
        "fr.friday.assistant.v31418logfix",
        "fr.friday.assistant.v31418voicefix",
        "fr.friday.assistant.v31418ecoute"
    };
    private FridayVersionPolicy() { }

    static boolean isOtherVersion(String ownPackage, String candidate) {
        if (ownPackage == null || candidate == null || ownPackage.equals(candidate)) return false;
        for (String known : KNOWN_PACKAGES) if (known.equals(candidate)) return true;
        return false;
    }

    static String warning(int count) {
        if (count <= 0) return "Aucune autre édition connue de F.R.I.D.A.Y. détectée.";
        return count + " autre(s) édition(s) F.R.I.D.A.Y. installée(s). " +
            "Android ne permet pas de confirmer ici si leur microphone écoute. " +
            "Arrêtez leur écoute avant de démarrer celle-ci.";
    }
}
