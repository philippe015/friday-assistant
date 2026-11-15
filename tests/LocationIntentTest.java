package fr.friday.assistant;
public final class LocationIntentTest {
 public static void main(String[] args) {
  check(LocationIntent.direct("Friday, où suis-je ?"));
  check(LocationIntent.direct("Ma localisation"));
  check(LocationIntent.nearby("Quel temps fait-il près de moi ?"));
  check(LocationIntent.nearby("Restaurants à proximité"));
  check(!LocationIntent.nearby("Bonjour comment vas-tu ?"));
  check(!LocationIntent.nearby("Météo à Paris"));
  check(LocationIntent.direct("Friday, où est-ce que je me trouve ?"));
  check(LocationIntent.direct("Quel est mon emplacement actuel ?"));
  check(LocationIntent.direct("Dans quel pays suis-je ?"));
  check(LocationIntent.direct("Mistral, as-tu ma position ?"));
  check(LocationIntent.direct("Friday, as-tu accès à mon GPS ?"));
  check(LocationIntent.nearby("Quels lieux sont autour d'ici ?"));
  check(LocationIntent.nearby("Quel temps fait-il ici ?"));
  check(LocationIntent.nearby("Où se trouvent les restaurants aux alentours ?"));
  check(!LocationIntent.nearby("Quelle est la météo à Lyon demain ?"));
  check(!LocationIntent.nearby("Quelle est la population de Paris ?"));
  System.out.println("OK : intentions de localisation");
 }
 private static void check(boolean ok) { if(!ok) throw new AssertionError("Échec test localisation"); }
}
