import fr.friday.assistant.JarvisMistralRouting;

public final class JarvisMistralRoutingTest {
    private static void check(boolean ok, String label) {
        if (!ok) throw new AssertionError(label);
    }
    public static void main(String[] args) {
        check(JarvisMistralRouting.useLocal("Bonjour comment vas-tu", true, true), "general local");
        check(!JarvisMistralRouting.useLocal("Bonjour comment vas-tu", true, false), "all off Mistral");
        check(!JarvisMistralRouting.useLocal("Dernieres actualites des 7 heures", true, true), "news via Mistral");
        check(!JarvisMistralRouting.useLocal("Quelle est la meteo a Paris", true, true), "weather via Mistral");
        check(!JarvisMistralRouting.useLocal("Recherche sur Internet ce site", true, true), "web via Mistral");
        check(JarvisMistralRouting.useLocal("Jarvis local, explique les etoiles", true, false), "explicit local");
        check(JarvisMistralRouting.useLocal("Jarvis local, explique la meteo", true, true), "explicit never cloud");
        check(!JarvisMistralRouting.useLocal("Jarvis, explique le soleil", false, true), "disabled no local");
        check(!JarvisMistralRouting.useLocal("", true, true), "empty");
        System.out.println("JarvisMistralRoutingTest: OK");
    }
}
