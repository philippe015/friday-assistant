package fr.friday.assistant;

import java.util.ArrayList;

/**
 * A true 18h–23h local evening forecast instead of daily extrema.
 * Date/hour indexes are selected from Open-Meteo's location-local timestamps.
 * No GPS, HTTP or private conversation is required by this policy.
 */
public final class WeatherEveningPolicy {
    private WeatherEveningPolicy() {}

    /** Invocation is only meaningful after a city is explicitly named. */
    public static boolean requested(String question) {
        String q = WeatherQueryPolicy.fold(question);
        return q.matches(".*\\b(?:ce soir|demain soir|pour ce soir|pour demain soir)\\b.*");
    }

    /**
     * Selects 18:00–23:00 for a named date. For the current date, earliestHour
     * is rounded up by the caller so no past hour is presented as future data.
     * Returns empty when the requested evening has already passed.
     */
    public static int[] indices(String[] localTimes, String date, int earliestHour) {
        if (localTimes == null || date == null || !date.matches("\\d{4}-\\d{2}-\\d{2}"))
            return new int[0];
        String prefix = date + "T";
        int from = Math.max(18, Math.min(24, earliestHour));
        ArrayList<Integer> indexes = new ArrayList<>(6);
        for (int i = 0; i < localTimes.length; i++) {
            String time = localTimes[i];
            if (time == null || time.length() < 16 || !time.startsWith(prefix))
                continue;
            String hourValue = time.substring(11, 13);
            if (!hourValue.matches("\\d{2}") || !":00".equals(time.substring(13, 16)))
                continue;
            int hour = Integer.parseInt(hourValue);
            if (hour >= from && hour <= 23) indexes.add(i);
        }
        int[] values = new int[indexes.size()];
        for (int i = 0; i < values.length; i++) values[i] = indexes.get(i);
        return values;
    }
}
