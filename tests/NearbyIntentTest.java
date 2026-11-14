import fr.friday.assistant.NearbyIntent;
public class NearbyIntentTest {
 public static void main(String[] args) {
  check(NearbyIntent.wantsPlaces("Friday trouve les pharmacies autour de moi"));
  check(NearbyIntent.wantsPlaces("Quels restaurants près de moi ?"));
  check(NearbyIntent.wantsPlaces("Analyse les alentours"));
  check(!NearbyIntent.wantsPlaces("Bonjour comment vas-tu ?"));
  check(!NearbyIntent.wantsPlaces("Quelle météo près de moi ?"));
  check(!NearbyIntent.wantsPlaces("Où suis-je ?"));
  check("pharmacy".equals(NearbyIntent.type("Pharmacie près de moi")));
  check("restaurant".equals(NearbyIntent.type("Restaurants à proximité")));
  System.out.println("OK : intentions de recherche des alentours");
 }
 private static void check(boolean b) {if (!b) throw new AssertionError("NearbyIntent");}
}
