package fr.friday.assistant;

public final class WebPageReaderTest {
    private static void ok(boolean valid, String message) {
        if (!valid) throw new AssertionError(message);
    }
    private static void rejected(String url) {
        try {
            WebPageReader.checkedUri(url);
            throw new AssertionError("URL risquée acceptée : " + url);
        } catch (IllegalArgumentException expected) {
            // valide : rejet au niveau URL, sans accès DNS
        } catch (Exception expected) {
            // URL malformée également refusée
        }
    }
    public static void main(String[] args) throws Exception {
        ok("www.example.com".equals(WebPageReader.checkedUri(
                "https://www.example.com/articles?x=1").getHost()), "public HTTPS URL");
        ok(WebPageReader.checkedUri("https://www.service-public.fr/").getPort() == -1,
                "official public website");
        rejected("http://www.example.com/");
        rejected("file:///etc/passwd");
        rejected("https://localhost:443/private");
        rejected("https://127.0.0.1/admin");
        rejected("https://192.168.0.1/");
        rejected("https://router.local/");
        rejected("https://metadata.google.internal/");
        rejected("https://user:password@www.example.com/");
        rejected("https://www.example.com:8080/");
        rejected("https://www.example.com/#secret");
        ok("https://www.lci.fr/".equals(
                WebPageReader.checkedUri("https//www.lci.fr/").toString()),
                "https// spoken prefix is fixed");
        ok("https://www.lci.fr/".equals(
                WebPageReader.checkedUri("https:/www.lci.fr/").toString()),
                "single slash spoken prefix is fixed");
        ok("https://www.lci.fr/".equals(
                WebPageReader.checkedUri("www.lci.fr/").toString()),
                "www domain gains HTTPS scheme");
        rejected("https//192.168.0.1/");
        rejected("https//localhost/");
        ok(WebPageReader.explainHttpStatus(202).contains("pas fourni"),
                "HTTP 202 ne prouve pas une panne Internet");
        ok(WebPageReader.explainHttpStatus(403).contains("Accès refusé"),
                "403 clearly identifies blocked websites");
        ok(WebPageReader.explainHttpStatus(404).contains("introuvable"),
                "404 clearly identifies missing content");
        ok(WebPageReader.explainHttpStatus(429).contains("limite"),
                "429 identifies rate limits");
        ok(!WebPageReader.explainHttpStatus(403).contains("contourn"),
                "403 never suggests bypassing website access restrictions");
        String txt = WebPageReader.plainText(
                "<html><title>FRIDAY</title><script>dangerous();</script>"
                + "<style>body{secret:42}</style><p>Bonjour &amp; bienvenue</p></html>");
        ok(txt.contains("Bonjour & bienvenue"), "entity and text");
        ok(!txt.contains("dangerous") && !txt.contains("secret:42"), "strip script/style");
        ok(WebPageReader.MAX_TEXT <= 12_000, "bounded page text");
        System.out.println("WebPageReaderTest OK (24 tests)");
    }
}
