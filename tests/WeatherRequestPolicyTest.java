import fr.friday.assistant.WeatherRequestPolicy;
import fr.friday.assistant.HandsFreeQuestionPolicy;

public final class WeatherRequestPolicyTest {
    private static void eq(Object actual, Object expected, String note) {
        if (!expected.equals(actual)) throw new AssertionError(
                note + " : expected " + expected + " actual " + actual);
    }
    private static void ok(boolean value, String note) {
        if (!value) throw new AssertionError(note);
    }
    public static void main(String[] args) {
        ok(WeatherRequestPolicy.isWeatherRequest("Jarvis, la météo ?"), "wake weather");
        ok(WeatherRequestPolicy.isWeatherRequest("Quel temps fera-t-il à Lyon demain ?"),
                "tomorrow forecast");
        ok(WeatherRequestPolicy.isWeatherRequest("Quelle température à Paris ?"),
                "temperature intent");
        ok(!WeatherRequestPolicy.isWeatherRequest("Bonjour à Lyon"), "not weather");
        eq(WeatherRequestPolicy.dayOffset("la météo aujourd'hui"), 0, "today");
        eq(WeatherRequestPolicy.dayOffset("météo demain"), 1, "tomorrow");
        eq(WeatherRequestPolicy.dayOffset("météo après-demain"), 2, "day after");
        eq(WeatherRequestPolicy.cityFromWeatherQuestion("météo demain"), "",
                "tomorrow not a city");
        eq(WeatherRequestPolicy.cityFromWeatherQuestion("la météo"), "",
                "needs city");
        eq(WeatherRequestPolicy.cityFromWeatherQuestion("la météo à Paris"), "Paris",
                "weather city");
        eq(WeatherRequestPolicy.cityFromWeatherQuestion("météo pour Lyon demain"), "Lyon",
                "weather destination");
        eq(WeatherRequestPolicy.cityFromWeatherQuestion("météo de demain à Marseille"),
                "Marseille", "day is not a city");
        eq(WeatherRequestPolicy.cityFromWeatherQuestion("Météo Paris, France"),
                "Paris, France", "city with country");
        eq(WeatherRequestPolicy.cityReply("Paris"), "Paris", "one city word");
        eq(WeatherRequestPolicy.cityReply("Jarvis Lyon"), "Lyon", "wake city reply");
        eq(WeatherRequestPolicy.cityReply("pour Saint-Étienne"), "Saint-Étienne",
                "compound city");
        eq(WeatherRequestPolicy.cityReply("Non merci"), "", "not city");
        eq(WeatherRequestPolicy.cityReply("quelle heure est il"), "", "not city question");
        eq(WeatherRequestPolicy.cityReply("demain"), "", "not date as city");
        ok(HandsFreeQuestionPolicy.acceptsDirectQuestion(
                "la météo", true, true), "no-button weather");
        ok(HandsFreeQuestionPolicy.acceptsDirectQuestion(
                "météo", true, true), "short weather");
        ok(!HandsFreeQuestionPolicy.acceptsDirectQuestion(
                "météo", false, true), "handsfree disabled");
        ok(!HandsFreeQuestionPolicy.acceptsDirectQuestion(
                "météo", true, false), "TTS echo blocked");
        System.out.println("WeatherRequestPolicyTest OK");
    }
}
