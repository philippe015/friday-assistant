import fr.friday.assistant.VoiceText;
public class VoiceTextTest {
    private static void eq(String a,String b) { if(!a.equals(b)) throw new AssertionError("Attendu: " + b + " / obtenu: " + a); }
    public static void main(String[] args) {
        eq(VoiceText.join("quelle est", "est la date"), "quelle est la date");
        eq(VoiceText.join("bonjour", "bonjour"), "bonjour");
        eq(VoiceText.join("", "bonjour"), "bonjour");
        if (!VoiceText.hasWakeWord("Jarvis, quelle est la météo ?")) throw new AssertionError("Jarvis manquant");
        if (VoiceText.hasWakeWord("Vendredissime")) throw new AssertionError("Mot-clé non borné");
        eq(VoiceText.withoutWakeWord("Jarvis, bonjour"), "bonjour");
        eq(VoiceText.withoutWakeWord("Friday, quelle heure est-il ?"), "quelle heure est-il ?");
        eq(VoiceText.withoutWakeWord("Jar vis, donne la date"), "donne la date");
        eq(VoiceText.withoutWakeWord("Qui est Friday ?"), "Qui est Friday ?");
        if (!VoiceText.startsWithWakeWord("Jar vis bonjour")) throw new AssertionError("Jar vis");
        if (!VoiceText.startsWithWakeWord("Friday bonjour")) throw new AssertionError("Friday");
        if (VoiceText.startsWithWakeWord("Bonjour Friday")) throw new AssertionError("Friday derrière");
        if (VoiceText.startsWithWakeWord("j'arrive demain")) throw new AssertionError("j'arrive faux positif");

        String x = "Une phrase particulièrement longue pour vérifier la segmentation des mots à l'oral.";
        eq(String.join(" ",VoiceText.ttsChunks(x, 32)),x);
        System.out.println("VoiceTextTest: OK (6 tests)");
    }
}
