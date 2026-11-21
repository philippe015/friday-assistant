import fr.friday.assistant.WeatherPlacePolicy;

public final class WeatherPlacePolicyTest {
    private static void eq(String expected, String actual) {
        if (!expected.equals(actual))
            throw new AssertionError("expected <" + expected + "> but got <" + actual + ">");
    }
    private static void yes(boolean value, String label) {
        if (!value) throw new AssertionError(label);
    }
    public static void main(String[] args) {
        eq("Paris", WeatherPlacePolicy.city("Paris, France"));
        eq("France", WeatherPlacePolicy.country("Paris, France"));
        eq("Paris", WeatherPlacePolicy.city("Paris France"));
        eq("France", WeatherPlacePolicy.country("Paris France"));
        eq("Paris", WeatherPlacePolicy.city("Paris en France"));
        eq("France", WeatherPlacePolicy.country("Paris en France"));
        eq("Paris", WeatherPlacePolicy.city("Paris, États-Unis"));
        eq("États-Unis", WeatherPlacePolicy.country("Paris, États-Unis"));
        eq("Lyon", WeatherPlacePolicy.city("Lyon"));
        eq("", WeatherPlacePolicy.country("Lyon"));
        eq("Aix-en-Provence", WeatherPlacePolicy.city("Aix-en-Provence"));
        eq("", WeatherPlacePolicy.country("Aix-en-Provence"));
        eq("Marseille", WeatherPlacePolicy.city("Marseille au Canada"));
        eq("Canada", WeatherPlacePolicy.country("Marseille au Canada"));
        yes(WeatherPlacePolicy.sameCountry("France", "France", "FR"), "France");
        yes(WeatherPlacePolicy.sameCountry("USA", "États-Unis", "US"), "USA");
        yes(WeatherPlacePolicy.sameCountry("États-Unis", "United States", "US"), "US");
        yes(WeatherPlacePolicy.sameCountry("Canada", "Canada", "CA"), "Canada");
        yes(!WeatherPlacePolicy.sameCountry("France", "États-Unis", "US"), "wrong country");
        yes(WeatherPlacePolicy.sameCity("Saint-Étienne", "Saint Etienne"), "accent");
        yes(!WeatherPlacePolicy.sameCity("Paris", "Parisville"), "not same city");
        System.out.println("WeatherPlacePolicyTest OK");
    }
}