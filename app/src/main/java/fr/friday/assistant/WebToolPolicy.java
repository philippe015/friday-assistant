package fr.friday.assistant;

/**
 * Choix de repli pour la recherche Mistral, sans accès réseau ni état Android.
 * Ne transforme jamais une réponse sans appel d'outil en résultat Web vérifié.
 */
public final class WebToolPolicy {
    private WebToolPolicy() {}

    /** Repli ponctuel vers le modèle de base si l'outil n'a pas été exécuté. */
    public static boolean retryWithoutExecution(String selected, String fallback,
                                                boolean executed, int references) {
        return selected != null && fallback != null && !selected.equals(fallback)
                && !executed && references == 0;
    }

    /**
     * 400/404/422 peuvent indiquer un modèle non compatible avec Conversations.
     * Ne pas réessayer sur 401/403/429 : une autre référence de modèle ne donne
     * pas de droits supplémentaires et ne contourne aucun quota.
     */
    public static boolean retryRejectedModel(String selected, String fallback, int httpCode) {
        return selected != null && fallback != null && !selected.equals(fallback)
                && (httpCode == 400 || httpCode == 404 || httpCode == 422);
    }
    /** Recherche alternative seulement pour les vrais refus HTTP du site. */
    public static boolean maySearchPublicAlternatives(String reason) {
        return reason != null && (reason.startsWith("Accès refusé par le site (HTTP 401)")
                || reason.startsWith("Accès refusé par le site (HTTP 403)"));
    }
}
