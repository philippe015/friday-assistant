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
    private static final String PENDING_COUNTRY = "jarvis_weather_country_requested";
    private static final String PENDING_CITY = "jarvis_weather_country_city";
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

    private static boolean awaitingCountry(SecurePrefs prefs) {
        return awaitingCity(prefs) && "1".equals(prefs.get(PENDING_COUNTRY, "0"));
    }

    /** Autorise un nom de ville court, mais seulement après une demande explicite. */
    public static boolean cityFollowup(SecurePrefs prefs, String heard) {
        return awaitingCity(prefs)
                && !WeatherRequestPolicy.cityReply(heard).isEmpty();
    }

    private static void clear(SecurePrefs prefs) {
        prefs.put(PENDING_UNTIL, "0");
        prefs.put(PENDING_DAY, "0");
        prefs.put(PENDING_COUNTRY, "0");
        prefs.put(PENDING_CITY, "");
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
            clear(prefs);
            city = WeatherRequestPolicy.cityFromWeatherQuestion(heard);
            day = WeatherRequestPolicy.dayOffset(heard);
            if (city.isEmpty()) {
                prefs.put(PENDING_DAY, String.valueOf(day));
                prefs.put(PENDING_UNTIL,
                        String.valueOf(System.currentTimeMillis() + VALID_FOR_MS));
                String prompt = "Pour quelle ville souhaitez-vous la météo "
                        + WeatherRequestPolicy.dayLabel(day)
                        + " ? Vous pouvez préciser le pays.";
                return new Answer(prompt, prompt, false);
            }
        } else {
            String reply = WeatherRequestPolicy.cityReply(heard);
            if (reply.isEmpty()) return null;
            if (awaitingCountry(prefs)) {
                String requestedCity = prefs.get(PENDING_CITY, "");
                if (requestedCity.isEmpty()) {
                    clear(prefs);
                    return null;
                }
                city = requestedCity + ", " + reply;
            } else {
                city = reply;
            }
            try { day = Integer.parseInt(prefs.get(PENDING_DAY, "0")); }
            catch (Exception ignored) { day = 0; }
            day = Math.max(0, Math.min(2, day));
        }
        if (!JarvisInternet.enabled(prefs)) {
            clear(prefs);
            String notice = "La consultation météo nécessite Internet. "
                    + "Activez « Autoriser Jarvis à rechercher des informations "
                    + "sur Internet » dans les réglages.";
            return new Answer(notice, notice, false);
        }
        try {
            WeatherForecast.Result result = WeatherForecast.lookup(city, day, cancellation);
            clear(prefs);
            return new Answer(result.text, result.speech, true);
        } catch (WeatherForecast.NeedCountryException ambiguous) {
            // Aucune prévision d'un pays arbitraire : la ville est conservée
            // uniquement le temps de demander « France ou États-Unis ? ».
            prefs.put(PENDING_DAY, String.valueOf(day));
            prefs.put(PENDING_COUNTRY, "1");
            prefs.put(PENDING_CITY, ambiguous.city);
            prefs.put(PENDING_UNTIL,
                    String.valueOf(System.currentTimeMillis() + VALID_FOR_MS));
            return new Answer(ambiguous.getMessage(), ambiguous.getMessage(), false);
        } catch (Exception unavailable) {
            clear(prefs);
            String reason = unavailable.getMessage() == null ? "service indisponible"
                    : unavailable.getMessage();
            if (reason.length() > 150) reason = reason.substring(0, 150);
            String notice = "Impossible de vérifier la météo de " + city
                    + " auprès du service météorologique : " + reason
                    + ". Réessayez en précisant le pays, par exemple "
                    + "« Paris, France ». Je ne vais pas inventer des températures.";
            return new Answer(notice, notice, false);
        }
    }
}
