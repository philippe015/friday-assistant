package fr.friday.assistant;

/** Requêtes locales explicites : aucun appel réseau sur une conversation ordinaire. */
public final class NearbyIntent {
    private NearbyIntent() {}
    public static boolean wantsPlaces(String question) {
        String q = VoiceText.normalize(question);
        boolean local = LocationIntent.nearby(q) || q.contains("a cote") || q.contains("pas loin") ||
            q.contains("proche de moi") || q.contains("aux alentours") || q.contains("alentours") ||
            q.contains("les plus proches") || q.contains("autour d'ici") || q.contains("dans le coin");
        boolean search = q.contains("trouve") || q.contains("cherche") || q.contains("quel") || q.contains("quels") ||
            q.contains("liste") || q.contains("montre") || q.contains("indique") || q.contains("y a t il") ||
            q.contains("analyse") || q.contains("situes") || q.contains("se trouvent") || q.contains("services");
        boolean category = type(question) != null;
        return (local && (category || search || q.contains("autour de moi") || q.contains("alentours"))) &&
            !q.contains("meteo") && !q.contains("quelle heure") && !q.contains("quel temps") &&
            !LocationIntent.direct(question);
    }
    public static String type(String question) {
        String q = VoiceText.normalize(question);
        if (q.contains("pharmaci")) return "pharmacy";
        if (q.contains("restauran")) return "restaurant";
        if (q.contains("cafes") || q.contains("cafe") || q.contains("coffee")) return "cafe";
        if (q.contains("hopital") || q.contains("urgence") || q.contains("hopitaux")) return "hospital";
        if (q.contains("supermarche") || q.contains("super marche")) return "supermarket";
        if (q.contains("parking")) return "parking";
        if (q.contains("station essence") || q.contains("essence")) return "gas_station";
        if (q.contains("distributeur") || q.contains("retrait de billets")) return "atm";
        if (q.contains("commissariat") || q.contains("police")) return "police";
        if (q.contains("gare") || q.contains("train")) return "train_station";
        return null;
    }
    public static String categoryForSearch(String question) {
        String t = type(question);
        if (t == null) return "lieux et commerces";
        switch (t) {
            case "pharmacy": return "pharmacies";
            case "restaurant": return "restaurants";
            case "cafe": return "cafés";
            case "hospital": return "hôpitaux";
            case "supermarket": return "supermarchés";
            case "parking": return "parkings";
            case "gas_station": return "stations service";
            case "atm": return "distributeurs de billets";
            case "police": return "commissariats";
            case "train_station": return "gares";
            default: return "lieux";
        }
    }
}
