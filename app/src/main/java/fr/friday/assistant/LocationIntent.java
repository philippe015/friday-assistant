package fr.friday.assistant;

/** Détection explicite des questions de localisation : pas de suivi permanent. */
public final class LocationIntent {
    private LocationIntent() { }
    public static boolean direct(String question) {
        String q = VoiceText.normalize(question);
        return q.contains("ou suis je") || q.contains("ou suis-je") ||
            q.contains("ma position") || q.contains("ma localisation") ||
            q.contains("localise moi") || q.contains("localise-moi") ||
            q.contains("donne moi mes coordonnees") || q.contains("coordonnees gps") ||
            q.contains("dans quelle ville suis je") || q.contains("dans quelle rue suis je") ||
            q.contains("a quel endroit suis je") || q.contains("ou je suis exactement") ||
            q.contains("dans quel endroit je suis") || q.contains("dans quelle commune suis je");
    }
    public static boolean nearby(String question) {
        String q = VoiceText.normalize(question);
        return direct(q) || q.contains("pres de moi") || q.contains("a proximite") ||
            q.contains("autour de moi") || q.contains("pres d'ici") ||
            q.contains("dans les environs") || q.contains("dans ma ville") ||
            q.contains("ou je me trouve") || q.contains("l'endroit ou je suis") ||
            q.contains("la ou je suis") || q.contains("meteo ici") ||
            q.contains("meteo chez moi") || q.contains("meteo locale") ||
            q.contains("ici aujourd'hui") || q.contains("dans mon quartier");
    }
}
