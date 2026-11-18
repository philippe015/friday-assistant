import fr.friday.assistant.OpenJarvisPolicy;

public final class OpenJarvisPolicyTest {
    private static void check(boolean ok, String reason) {
        if (!ok) throw new AssertionError(reason);
    }
    private static void reject(String base) {
        try {
            OpenJarvisPolicy.baseUrl(base);
            throw new AssertionError("Adresse dangereuse acceptée : " + base);
        } catch (IllegalArgumentException expected) { }
    }
    public static void main(String[] args) {
        check(OpenJarvisPolicy.baseUrl("https://jarvis.maison.example:8443/v1/")
                .equals("https://jarvis.maison.example:8443"), "normalise /v1");
        check(OpenJarvisPolicy.completionsUrl("https://jarvis.example/")
                .equals("https://jarvis.example/v1/chat/completions"), "route API officielle");
        check(OpenJarvisPolicy.healthUrl("https://jarvis.example").endsWith("/health"), "health");
        for (String url : new String[]{"http://jarvis.example", "https://localhost:8000",
                "https://127.0.0.1", "https://u:p@jarvis.example",
                "https://jarvis.example/?token=abc", "https://jarvis.example/#key",
                "https://jarvis.example/private", "file:///etc/passwd", ""}) reject(url);
        check(!OpenJarvisPolicy.shouldRoute("Jarvis, aide-moi", false, true), "opt-in requis");
        check(OpenJarvisPolicy.shouldRoute("Jarvis, aide-moi", true, false), "mot-cle");
        check(OpenJarvisPolicy.shouldRoute("Friday, demande à Jarvis de réfléchir", true, false), "délégation FR");
        check(!OpenJarvisPolicy.shouldRoute("Friday, actualités", true, false), "Mistral par défaut");
        check(OpenJarvisPolicy.shouldRoute("Question générale", true, true), "mode route-all");
        check(OpenJarvisPolicy.cleanQuestion("Jarvis, explique les étoiles")
                .equals("explique les étoiles"), "retirer wake");
        check(OpenJarvisPolicy.cleanQuestion("Friday, demande à Jarvis de vérifier")
                .equals("de vérifier"), "retirer prefixe de délégation");
        check(OpenJarvisPolicy.model("qwen3.5:2b").equals("qwen3.5:2b"), "identifiant modèle");
        try { OpenJarvisPolicy.model("../secret model"); throw new AssertionError("Invalid model"); }
        catch (IllegalArgumentException expected) { }
        System.out.println("OpenJarvisPolicyTest OK");
    }
}
