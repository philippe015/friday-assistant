package fr.friday.assistant;

/**
 * Deux tours de dialogue : « météo » -> « Pour quelle ville ? » -> « Lyon ».
 * Une intention et sa date d'expiration sont locales aux préférences de l'APK ;
 * ni GPS, ni historique privé, ni ville enregistrée durablement.
 */
public final class WeatherDialogue {
    private WeatherDialogue() { }
    private static final String PENDING_UNTIL = "jarvis_weather_city_until";
    private static final String PENDING_DAY = "jarvis_weather_city_day";
    private static final long VALID_FOR_MS = 120_000L;

    public static final class Answer {
        public final String text;
        public final String speech;
        public final boolean usedInternet;
        Answer(String text, String speech, boolean usedInternet) {
            this.text = text; this.speech = speech; this.usedInternet = usedInternet;
        }
    }

    public static boolean awaitingCity(SecurePrefs prefs) {
        try {
            long until = Long.parseLong(prefs.get(PENDING_UNTIL, "0"));
            return until > System.currentTimeMillis();
        } catch (Exception invalid) {
            return false;
        }
    }

    /** Autorise un nom de ville court, mais seulement après une demande explicite. */
    public static boolean cityFollowup(SecurePrefs prefs, String heard) {
        return awaitingCity(prefs)
                && !WeatherRequestPolicy.cityReply(heard).isEmpty();
    }

    private static void clear(SecurePrefs prefs) {
        prefs.put(PENDING_UNTIL, "0");
        prefs.put(PENDING_DAY, "0");
    }

    /**
     * Retourne null si l'entrée ne concerne pas la météo.
     * Interroge un service de météorologie public seulement après obtention
     * du nom de ville et si l'accès Internet a été autorisé par l'utilisateur.
     */
    public static Answer answer(SecurePrefs prefs, String heard,
                                NativeAI.CancelToken cancellation) {
        boolean freshRequest = WeatherRequestPolicy.isWeatherRequest(heard);
        boolean waiting = awaitingCity(prefs);
        if (!freshRequest && !waiting) return null;

        String city;
        int day;
        if (freshRequest) {
            city = WeatherRequestPolicy.cityFromWeatherQuestion(heard);
            day = WeatherRequestPolicy.dayOffset(heard);
            if (city.isEmpty()) {
                prefs.put(PENDING_DAY, String.valueOf(day));
                prefs.put(PENDING_UNTIL,
                        String.valueOf(System.currentTimeMillis() + VALID_FOR_MS));
                String prompt = "Pour quelle ville souhaitez-vous la météo "
                        + WeatherRequestPolicy.dayLabel(day) + " ?";
                return new Answer(prompt, prompt, false);
            }
        } else {
            city = WeatherRequestPolicy.cityReply(heard);
            if (city.isEmpty()) return null;
            try { day = Integer.parseInt(prefs.get(PENDING_DAY, "0")); }
            catch (Exception ignored) { day = 0; }
            day = Math.max(0, Math.min(2, day));
        }
        clear(prefs);
        if (!JarvisInternet.enabled(prefs)) {
            String notice = "La consultation météo nécessite Internet. "
                    + "Activez « Autoriser Jarvis à rechercher des informations "
                    + "sur Internet » dans les réglages.";
            return new Answer(notice, notice, false);
        }
        try {
            WeatherForecast.Result result = WeatherForecast.lookup(city, day, cancellation);
            return new Answer(result.text, result.speech, true);
        } catch (Exception unavailable) {
            String reason = unavailable.getMessage() == null ? "service indisponible"
                    : unavailable.getMessage();
            if (reason.length() > 150) reason = reason.substring(0, 150);
            String notice = "Impossible de vérifier la météo de " + city
                    + " auprès du service météorologique : " + reason
                    + ". Réessayez avec une ville plus précise, par exemple "
                    + "« Paris, France ». Je ne vais pas inventer des températures.";
            return new Answer(notice, notice, false);
        }
    }
}
