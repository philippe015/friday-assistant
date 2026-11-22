package fr.friday.assistant;

/** Diagnostic redaction policy; never persist private speech or raw error text. */
public final class DiagnosticLogPolicyTest {
    private static void same(String expected, String actual, String reason) {
        if (!expected.equals(actual))
            throw new AssertionError(reason + ": expected " + expected + ", got " + actual);
    }
    private static void yes(boolean condition, String reason) {
        if (!condition) throw new AssertionError(reason);
    }
    public static void main(String[] args) {
        String privateQuestion = "Analyse l'article numéro deux et voici ma "
                + "clé API = secret456, latitude 48.81386 longitude 2.46740";
        same("", DiagnosticLogPolicy.safeCode("user", privateQuestion),
                "Never log questions");
        same("", DiagnosticLogPolicy.safeCode("assistant", "https://news.google.com/"
                + privateQuestion), "Never log RSS URLs or answers");
        same("", DiagnosticLogPolicy.safeCode("tts", privateQuestion),
                "Never log synthesized words");
        same("", DiagnosticLogPolicy.safeCode("asr_diag",
                "partial : le mot de passe secret"), "No raw Vosk partials");
        same("VOSK_FINAL", DiagnosticLogPolicy.safeCode(
                "asr_diag", "Question finalisée par Vosk : « " + privateQuestion + " »"),
                "Finalized Vosk event but never content");
        same("VOSK_NOISE_IGNORED", DiagnosticLogPolicy.safeCode(
                "asr_diag", "Mot isolé reconnu par Vosk, ignoré : secret456"),
                "Noise classification");
        same("LITERT_TOKEN_OVERFLOW", DiagnosticLogPolicy.safeCode(
                "error", "INVALID_ARGUMENT: Input token ids are too long. 2151 >= 2048 "
                        + privateQuestion), "Token overflow only as code");
        same("LITERT_INFERENCE_FAILED", DiagnosticLogPolicy.safeCode(
                "error", "LiteRtLmJniException: Failed to call nativeSendMessage"),
                "Native LiteRT failure code");
        same("GPS_FIX_REPORTED", DiagnosticLogPolicy.safeCode(
                "location", "Latitude 48.81386 Longitude 2.46740 source network"),
                "No raw coordinates");
        same("WEB_SEARCH_START", DiagnosticLogPolicy.safeCode(
                "status", "Jarvis recherche sur plusieurs sources HTTPS publiques (Bing)"),
                "Web phase recognized");
        same("MIC_READY", DiagnosticLogPolicy.safeCode(
                "voice_state", "Micro prêt : dialogue continu, posez une question"),
                "Micro phase recognized");
        same("RESPONSE_PREPARED", DiagnosticLogPolicy.safeCode(
                "latency", "Réponse préparée en 2230 ms · recherche Web non aboutie"),
                "Latency status recognized");
        yes(DiagnosticLogPolicy.safeMetricMs(
                "latency", "Réponse préparée en 2230 ms · secret") == 2230,
                "Safe response duration");
        yes(DiagnosticLogPolicy.safeMetricMs(
                "latency", "Réponse préparée en 999999999 ms") == -1,
                "Reject implausible durations");
        same("", DiagnosticLogPolicy.safeCode(
                "status", "Mon adresse et ma clé sont secret456"),
                "Non-diagnostic status discarded");
        yes(DiagnosticLogPolicy.allowedCode("RSS_LOCAL_ANALYSIS_FAILED"),
                "Compiler constants accepted");
        yes(!DiagnosticLogPolicy.allowedCode(
                "USER_SECRET: https://example.com/abc"), "Arbitrary details rejected");
        same("TTS_FIRST_SPEECH", DiagnosticLogPolicy.safeCode(
                "latency", "Première parole après 2344 ms (mesuré par Android)"),
                "First speech is NOT response preparation");
        yes(DiagnosticLogPolicy.safeMetricMs(
                "latency", "Première parole après 2344 ms (mesuré par Android)") == 2344,
                "First speech gets its actual duration");
        same("", DiagnosticLogPolicy.safeCode("asr_diag",
                "Réponse terminée ; après 2,5 s, question directe acceptée "
                + "pendant 20 s sans mot-clé"),
                "The words sans mot-cle must NOT produce a wake event");
        same("VOSK_WAKE", DiagnosticLogPolicy.safeCode("asr_diag",
                "Mot-clé Jarvis reconnu, question envoyée à Jarvis"),
                "Actual Vosk wake recognized");
        same("VOSK_DIRECT_QUESTION", DiagnosticLogPolicy.safeCode("asr_diag",
                "Question directe reconnue, envoyée à Jarvis"),
                "Hands-free question distinguished from wake word");
        same("ECHO_DICTATION_PAUSED", DiagnosticLogPolicy.safeCode("barge_diag",
                "Protection anti-écho : dictée suspendue · STOP vocal désactivé"),
                "TTS echo pause");
        same("ECHO_RESUMED", DiagnosticLogPolicy.safeCode("barge_diag",
                "Protection anti-écho terminée · questions autorisées"),
                "Echo guard released");
        same("ECHO_COOLDOWN", DiagnosticLogPolicy.safeCode("barge_diag",
                "Réponse clôturée · anti-écho temporaire · dictée bientôt active"),
                "Echo cooldown phase");
        same("", DiagnosticLogPolicy.safeCode("latency",
                "Une phrase technique sans temps identifiable"),
                "Do not save unclassified latency status");
        System.out.println("DiagnosticLogPolicyTest OK");
    }
}
