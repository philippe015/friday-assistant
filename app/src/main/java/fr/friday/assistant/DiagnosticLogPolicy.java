package fr.friday.assistant;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Privacy-first local diagnostics: never persist the content of a question,
 * spoken words, response, profile, RSS title/URL, GPS coordinate, IP or key.
 * Only predetermined codes + bounded, non-identifying timings are retained.
 * Pure Java so the privacy contract can be regression-tested off Android.
 */
public final class DiagnosticLogPolicy {
    private DiagnosticLogPolicy() { }

    private static final Pattern LATENCY_MS = Pattern.compile(
            "Réponse préparée en\\s+(\\d{1,7})\\s+ms", Pattern.CASE_INSENSITIVE);
    private static final Pattern TTS_MS = Pattern.compile(
            "Première parole après\\s+(\\d{1,7})\\s+ms", Pattern.CASE_INSENSITIVE);

    /** Empty means "do not record": especially raw text/questions and GPS. */
    public static String safeCode(String kind, String raw) {
        if (kind == null || raw == null) return "";
        String v = raw.toLowerCase(Locale.ROOT);
        switch (kind) {
            case "user": case "assistant": case "tts": case "partial":
                // Includes full questions, articles, AI answers, and quotes.
                return "";
            case "latency":
                return "RESPONSE_PREPARED";
            case "asr_diag":
                if (v.contains("question finalisée") || v.contains("question finalisee"))
                    return "VOSK_FINAL";
                if (v.contains("mot isolé") || v.contains("mot isole"))
                    return "VOSK_NOISE_IGNORED";
                if (v.contains("pas envoyée") || v.contains("pas envoyee"))
                    return "VOSK_NO_WAKE";
                if (v.contains("mot-clé") || v.contains("mot-cle"))
                    return "VOSK_WAKE";
                return "";
            case "voice_state":
                if (v.contains("micro prêt") || v.contains("micro pret"))
                    return "MIC_READY";
                if (v.contains("lecture tts") || v.contains("dictée suspendue")
                        || v.contains("dictee suspendue"))
                    return "TTS_ECHO_BLOCKED";
                if (v.contains("fin de la voix"))
                    return "TTS_FINISHED";
                if (v.contains("démarrage du microphone") || v.contains("demarrage du microphone"))
                    return "MIC_STARTING";
                return "";
            case "location":
                return v.contains("aucun") || v.contains("indisponible")
                        ? "GPS_FIX_MISSING" : "GPS_FIX_REPORTED";
            case "gps_api":
                return "GPS_STATUS";
            case "mic_conflict":
                return "MIC_DEVICE_CONFLICT";
            case "barge_diag":
                if (v.contains("anti-écho") || v.contains("anti-echo"))
                    return "ECHO_PROTECTION";
                return "";
            case "status":
                if (v.contains("recherche sur plusieurs sources")
                        || v.contains("recherche web")
                        || v.contains("recherche internet"))
                    return "WEB_SEARCH_START";
                if (v.contains("acquisition gps")) return "GPS_ACQUIRING";
                if (v.contains("écoute native active") || v.contains("ecoute native active"))
                    return "VOICE_SERVICE_ACTIVE";
                if (v.contains("réponse terminée") || v.contains("reponse terminee"))
                    return "VOICE_RESPONSE_FINISHED";
                if (v.contains("interrompue") || v.contains("interrompu"))
                    return "RESPONSE_INTERRUPTED";
                return "";
            case "error":
                if (v.contains("input token ids are too long")
                        || v.contains("tokens allowed") || v.contains("entrée trop longue"))
                    return "LITERT_TOKEN_OVERFLOW";
                if (v.contains("litert") || v.contains("inférence") || v.contains("inference"))
                    return "LITERT_INFERENCE_FAILED";
                if (v.contains("vosk") || v.contains("reconnaiss"))
                    return "VOSK_ERROR";
                if (v.contains("micro") || v.contains("audiorecord"))
                    return "MIC_ERROR";
                if (v.contains("gps") || v.contains("localisation"))
                    return "GPS_ERROR";
                if (v.contains("http") || v.contains("rss") || v.contains("web")
                        || v.contains("internet"))
                    return "WEB_ERROR";
                if (v.contains("tts") || v.contains("voix"))
                    return "TTS_ERROR";
                return "OTHER_ERROR";
            default:
                return "";
        }
    }

    /** Only two known timings, parsed without any neighboring raw text. */
    public static int safeMetricMs(String kind, String raw) {
        if (kind == null || raw == null) return -1;
        Pattern pattern = "latency".equals(kind) ? LATENCY_MS
                : "tts_latency".equals(kind) ? TTS_MS : null;
        if (pattern == null) return -1;
        Matcher m = pattern.matcher(raw);
        if (!m.find()) return -1;
        try {
            long milliseconds = Long.parseLong(m.group(1));
            return milliseconds >= 0 && milliseconds <= 600000 ? (int) milliseconds : -1;
        } catch (NumberFormatException ex) {
            return -1;
        }
    }

    /** Only internal, fixed diagnostic codes are permitted. */
    public static boolean allowedCode(String code) {
        return code != null && code.matches("[A-Z][A-Z0-9_]{2,55}");
    }
}
