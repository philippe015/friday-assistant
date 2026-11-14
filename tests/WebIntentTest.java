import fr.friday.assistant.WebIntent;
public class WebIntentTest {
    public static void main(String[] args) {
        if (WebIntent.needsWeb("Bonjour Friday, explique-moi la relativité.")) throw new AssertionError("Question générale : pas de Web");
        if (!WebIntent.needsWeb("Cherche sur Internet les dernières nouvelles.")) throw new AssertionError("Recherche Web explicite");
        if (!WebIntent.needsWeb("Donne la météo aujourd'hui.")) throw new AssertionError("Météo");
        if (!WebIntent.needsWeb("Quel est le taux de change actuel ?")) throw new AssertionError("Change");
        if (WebIntent.needsWeb("")) throw new AssertionError("Vide");
        System.out.println("WebIntentTest : 5 tests OK");
    }
}
