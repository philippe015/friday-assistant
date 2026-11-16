import fr.friday.assistant.WebToolPolicy;

public final class WebToolPolicyTest {
    private static void check(boolean good, String message) {
        if (!good) throw new AssertionError(message);
    }
    public static void main(String[] args) {
        String model = "custom-vision-model", fallback = "mistral-small-latest";
        check(WebToolPolicy.retryWithoutExecution(model, fallback, false, 0),
                "recherche non déclenchée : repli requis");
        check(!WebToolPolicy.retryWithoutExecution(model, fallback, true, 0),
                "outil exécuté, aucune deuxième requête inutile");
        check(!WebToolPolicy.retryWithoutExecution(model, fallback, false, 2),
                "références déjà présentes, ne pas perdre les sources");
        check(!WebToolPolicy.retryWithoutExecution(fallback, fallback, false, 0),
                "pas de boucle de repli vers le même modèle");
        for (int code : new int[] {400, 404, 422})
            check(WebToolPolicy.retryRejectedModel(model, fallback, code),
                    "code modèle incompatible : " + code);
        for (int code : new int[] {401, 403, 429, 500})
            check(!WebToolPolicy.retryRejectedModel(model, fallback, code),
                    "ne jamais masquer clé, autorisation, quota ou panne : " + code);
        check(WebToolPolicy.maySearchPublicAlternatives(
                "Accès refusé par le site (HTTP 403). Lecture interdite."),
                "source publique bloquée : recherche de sources alternatives");
        check(WebToolPolicy.maySearchPublicAlternatives(
                "Accès refusé par le site (HTTP 401)."),
                "401 doit autoriser les sources alternatives");
        check(!WebToolPolicy.maySearchPublicAlternatives(
                "Accès à une adresse réseau privée interdit."),
                "ne jamais rechercher une adresse locale");
        check(!WebToolPolicy.maySearchPublicAlternatives(
                "Page introuvable sur le site (HTTP 404)."),
                "ne jamais feindre une page supprimée");
        System.out.println("WebToolPolicyTest OK");
    }
}
