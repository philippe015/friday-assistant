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
        String txt = WebPageReader.plainText(
                "<html><title>FRIDAY</title><script>dangerous();</script>"
                + "<style>body{secret:42}</style><p>Bonjour &amp; bienvenue</p></html>");
        ok(txt.contains("Bonjour & bienvenue"), "entity and text");
        ok(!txt.contains("dangerous") && !txt.contains("secret:42"), "strip script/style");
        ok(WebPageReader.MAX_TEXT <= 12_000, "bounded page text");
        System.out.println("WebPageReaderTest OK (15 tests)");
    }
}
