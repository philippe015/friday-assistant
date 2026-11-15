package fr.friday.assistant;

/**
 * Détection ponctuelle des demandes qui concernent réellement la position de l'utilisateur.
 * Aucun appel GPS pour un lieu distant (« météo à Paris »).
 */
public final class LocationIntent {
    private LocationIntent() { }

    private static String fold(String question) {
        return VoiceText.normalize(question).replaceAll("[^a-z0-9]+", " ")
                .replaceAll("\\s+", " ").trim();
    }

    public static boolean direct(String question) {
        String q = fold(question);
        return q.contains("ou suis je") || q.contains("ou je suis")
                || q.contains("ou est ce que je suis") || q.contains("ou est ce que je me trouve")
                || q.contains("ou je me trouve") || q.contains("ma position")
                || q.contains("mon emplacement") || q.contains("ma localisation")
                || q.contains("position gps") || q.contains("localise moi")
                || q.contains("localisation actuelle") || q.contains("mes coordonnees")
                || q.contains("coordonnees gps")
                || q.contains("dans quelle ville suis je") || q.contains("dans quelle rue suis je")
                || q.contains("dans quel pays suis je") || q.contains("dans quelle commune suis je")
                || q.contains("a quel endroit suis je") || q.contains("ou je suis exactement")
                || q.contains("quel endroit je me trouve") || q.contains("l endroit ou je suis");
    }

    public static boolean nearby(String question) {
        String q = fold(question);
        return direct(q) || q.contains("pres de moi") || q.contains("proche de moi")
                || q.contains("a proximite de moi") || q.contains("a proximite d ici")
                || q.contains("a proximite") || q.contains("autour de moi")
                || q.contains("autour d ici") || q.contains("pres d ici")
                || q.contains("aux alentours") || q.contains("dans les environs")
                || q.contains("dans mon quartier") || q.contains("dans ma ville")
                || q.contains("dans ma region") || q.contains("ici aujourd hui")
                || q.contains("la ou je suis") || q.contains("la ou je me trouve")
                || q.contains("ou je me trouve") || q.contains("ou je suis")
                || q.contains("meteo ici") || q.contains("meteo chez moi")
                || q.contains("meteo locale") || q.contains("temps qu il fait ici")
                || q.contains("quel temps fait il ici") || q.contains("services a cote de moi");
    }
}
