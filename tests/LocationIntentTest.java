package fr.friday.assistant;
public final class LocationIntentTest {
 public static void main(String[] args) {
  check(LocationIntent.direct("Friday, où suis-je ?"));
  check(LocationIntent.direct("Ma localisation"));
  check(LocationIntent.nearby("Quel temps fait-il près de moi ?"));
  check(LocationIntent.nearby("Restaurants à proximité"));
  check(!LocationIntent.nearby("Bonjour comment vas-tu ?"));
  check(!LocationIntent.nearby("Météo à Paris"));
  System.out.println("OK : intentions de localisation");
 }
 private static void check(boolean ok) { if(!ok) throw new AssertionError("Échec test localisation"); }
}
