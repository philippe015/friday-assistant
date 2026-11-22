package fr.friday.assistant;

import android.content.Context;
import android.os.Build;
import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Persisted, rotated and encrypted diagnostic journal (AES-GCM Android Keystore).
 *
 * Records ONLY fixed internal codes, timestamps and an optional timing. Never
 * records raw user/AI utterances, URL, GPS coordinates, secrets or prompts.
 * No network access, no permissions, no automatic upload. Export is explicit.
 * All IO is serialized on a worker, so voice/recognition threads never wait
 * for Keystore or JSON serialization; read/export drains queued writes.
 */
public final class DiagnosticLog {
    private static final String SECRET = "jarvis_diagnostics_v31412";
    private static final int MAX_EVENTS = 420;
    private static final int MAX_JSON_CHARS = 68000;
    private static final long RETENTION_MS = 14L * 24 * 3600 * 1000;
    private static final ExecutorService IO = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "jarvis-diagnostic-encrypted-io");
        t.setDaemon(true);
        return t;
    });
    private static volatile String lastFailure = "";
    private DiagnosticLog() {}

    /** Message is never saved. Recognized codes only are passed to the writer. */
    public static void fromEvent(Context context, String kind, String message) {
        String code = DiagnosticLogPolicy.safeCode(kind, message);
        if (code.isEmpty()) return;
        int ms = DiagnosticLogPolicy.safeMetricMs(kind, message);
        append(context, code, ms);
        // The latency status is derived from an already verified local result.
        if ("RESPONSE_PREPARED".equals(code)) {
            String text = message == null ? "" : message.toLowerCase(Locale.ROOT);
            append(context, text.contains("recherche web non aboutie")
                    ? "WEB_RESULT_UNAVAILABLE"
                    : text.contains("sources web consultées")
                    ? "WEB_RESULT_USED" : "LOCAL_RESULT", -1);
        }
    }

    /** Call only with compile-time codes and numeric metrics (no arbitrary data). */
    public static void record(Context context, String code) {
        append(context, code, -1);
    }

    private static void append(Context context, String code, int ms) {
        if (context == null || !DiagnosticLogPolicy.allowedCode(code)) return;
        final Context app = context.getApplicationContext();
        final long when = System.currentTimeMillis();
        IO.execute(() -> {
            try {
                SecurePrefs prefs = new SecurePrefs(app);
                JSONArray existing = read(prefs);
                JSONArray next = new JSONArray();
                long oldest = when - RETENTION_MS;
                // Retain at most MAX_EVENTS-1 most recent events, discarding
                // older data or malformed entries. Never log private payload.
                int start = Math.max(0, existing.length() - MAX_EVENTS + 1);
                for (int i = start; i < existing.length(); i++) {
                    JSONObject event = existing.optJSONObject(i);
                    if (event == null || event.optLong("time_ms", 0) < oldest) continue;
                    next.put(event);
                }
                JSONObject entry = new JSONObject();
                entry.put("time_ms", when);
                entry.put("code", code);
                if (ms >= 0 && ms <= 600000) entry.put("duration_ms", ms);
                next.put(entry);
                // Avoid oversize encrypted secrets and bound storage even if
                // Android's wall-clock jumps backwards.
                while (next.length() > MAX_EVENTS || next.toString().length() > MAX_JSON_CHARS)
                    next.remove(0);
                prefs.putPrivateSecret(SECRET, next.toString());
                lastFailure = "";
            } catch (Exception unavailable) {
                // Fail closed: no plaintext cache, logfile or error detail
                // containing secrets or call parameters is written.
                lastFailure = "Stockage chiffré du journal indisponible";
            }
        });
    }

    private static JSONArray read(SecurePrefs prefs) throws Exception {
        String raw = prefs.privateSecret(SECRET);
        return raw.isEmpty() ? new JSONArray() : new JSONArray(raw);
    }

    /** Default remains code-only. No conversation history read without consent. */
    public static String exportJson(Context context) throws Exception {
        return exportJson(context, false);
    }

    /**
     * User-selected export: assemble the encrypted diagnostic events with the
     * already-encrypted conversation history. Never duplicate dialogues into
     * the technical log, preferences or any temporary plaintext file.
     * The JSON result stays in memory until Android's selected document is
     * explicitly opened by MainActivity.
     */
    public static String exportJson(Context context, boolean includeConversation)
            throws Exception {
        final Context app = context.getApplicationContext();
        Future<String> future = IO.submit(() -> {
            SecurePrefs prefs = new SecurePrefs(app);
            JSONArray entries = read(prefs);
            JSONObject report = new JSONObject();
            report.put("format", "jarvis-diagnostics");
            report.put("schema", 2);
            report.put("generated_utc", utc(System.currentTimeMillis()));
            report.put("app_version", FridayVersionGuard.thisVersion(app));
            report.put("android_sdk", Build.VERSION.SDK_INT);
            report.put("retention_days", 14);
            report.put("conversation_included", includeConversation);
            report.put("privacy", includeConversation
                    ? "Export EXPLICITE incluant le texte des échanges locaux. "
                      + "Le fichier JSON n'est PAS chiffré ; il peut révéler "
                      + "des données personnelles, des URL et des secrets saisis. "
                      + "Ne l'envoyez qu'après vérification manuelle."
                    : "Codes de diagnostic et durées uniquement : aucune "
                      + "transcription ou réponse n'est incluse.");
            report.put("events", entries);

            if (includeConversation) {
                // Do not record dialogue a second time. Read the encrypted
                // 80-message ring ONLY when the user checks the option.
                JSONArray history = new JSONArray(
                        PrivateConversationHistory.read(app, prefs));
                JSONArray conversation = new JSONArray();
                for (int i = Math.max(0, history.length() - 80);
                        i < history.length(); i++) {
                    JSONObject message = history.optJSONObject(i);
                    if (message == null) continue;
                    String kind = message.optString("kind", "");
                    if (!("user".equals(kind) || "assistant".equals(kind))) continue;
                    JSONObject item = new JSONObject();
                    item.put("seq", message.optLong("seq", i + 1));
                    item.put("role", kind);
                    item.put("channel", message.optString("channel", "unknown"));
                    // Full text of the retained exchange (no clipping).
                    item.put("text", message.optString("text", ""));
                    long timestamp = message.optLong("time_ms", 0);
                    if (timestamp > 0) {
                        item.put("time_ms", timestamp);
                        item.put("time_utc", utc(timestamp));
                    } else {
                        // Do not fabricate dates for conversations saved before
                        // v3.14.14. Their original order is still preserved.
                        item.put("time_unknown", true);
                    }
                    conversation.put(item);
                }
                report.put("conversation", conversation);
                report.put("conversation_count", conversation.length());
                report.put("conversation_note",
                        "80 derniers messages conservés au maximum dans "
                        + "l'historique privé. Les messages antérieurs à "
                        + "v3.14.14 n'ont pas d'horodatage fiable.");
                report.put("timeline", timeline(entries, conversation));
            }
            return report.toString(2);
        });
        return future.get(12, TimeUnit.SECONDS);
    }

    /** Sort non-private diagnostics + references to dated dialogue messages. */
    private static JSONArray timeline(JSONArray events, JSONArray conversation)
            throws Exception {
        java.util.ArrayList<JSONObject> merged = new java.util.ArrayList<>();
        for (int i = 0; i < events.length(); i++) {
            JSONObject entry = events.optJSONObject(i);
            if (entry == null) continue;
            JSONObject point = new JSONObject();
            point.put("time_ms", entry.optLong("time_ms", 0));
            point.put("type", "diagnostic");
            point.put("code", entry.optString("code", ""));
            if (entry.has("duration_ms"))
                point.put("duration_ms", entry.optInt("duration_ms"));
            merged.add(point);
        }
        for (int i = 0; i < conversation.length(); i++) {
            JSONObject message = conversation.optJSONObject(i);
            if (message == null || !message.has("time_ms")) continue;
            JSONObject point = new JSONObject();
            point.put("time_ms", message.optLong("time_ms"));
            point.put("type", "conversation_reference");
            point.put("seq", message.optLong("seq"));
            point.put("role", message.optString("role"));
            // Text is available exactly once in conversation[].
            merged.add(point);
        }
        merged.sort(java.util.Comparator.comparingLong(
                e -> e.optLong("time_ms", 0)));
        JSONArray ordered = new JSONArray();
        for (JSONObject point : merged) ordered.put(point);
        return ordered;
    }

    /**
     * Minimal, privacy-preserving report for a PUBLIC GitHub Issue.
     * No full diagnostic JSON, transcripts, URLs, raw errors, keys,
     * precise dates/times or conversation history can enter this export.
     * The Android UI asks permission before opening the browser.
     */
    public static String githubIssueBody(Context context) throws Exception {
        final Context app = context.getApplicationContext();
        Future<String> future = IO.submit(() -> {
            JSONArray entries = read(new SecurePrefs(app));
            StringBuilder body = new StringBuilder(2700);
            body.append("## Anomalie observée\n")
                    .append("Décrivez ce qui ne fonctionne pas.\n\n")
                    .append("## Étapes pour reproduire\n")
                    .append("1. Ouvrir JARVIS\n2. ...\n3. ...\n\n")
                    .append("## Résultat attendu\n")
                    .append("Décrivez le comportement souhaité.\n\n")
                    .append("## Environnement (non identifiant)\n")
                    .append("- Version JARVIS : ")
                    .append(FridayVersionGuard.thisVersion(app)
                            .replaceAll("[^a-zA-Z0-9. _-]", " ").trim())
                    .append("\n- Android SDK : ")
                    .append(Build.VERSION.SDK_INT)
                    .append("\n\n## Derniers événements techniques anonymisés\n")
                    .append("Codes fixes + délais relatifs seulement. ")
                    .append("Pas de paroles, conversations, URL, GPS précis, ")
                    .append("identifiants ou clés.\n\n")
                    .append("```text\n");
            int start = Math.max(0, entries.length() - 28);
            long base = -1;
            int included = 0;
            for (int i = start; i < entries.length(); i++) {
                JSONObject entry = entries.optJSONObject(i);
                if (entry == null) continue;
                String code = entry.optString("code", "");
                // Only strictly predetermined uppercase codes are published.
                if (!DiagnosticLogPolicy.allowedCode(code)) continue;
                long timestamp = entry.optLong("time_ms", 0);
                if (timestamp <= 0) continue;
                if (base < 0) base = timestamp;
                long elapsed = Math.max(0, (timestamp - base) / 1000);
                body.append("+").append(elapsed).append("s  ").append(code);
                if (entry.has("duration_ms")) {
                    int duration = entry.optInt("duration_ms", -1);
                    if (duration >= 0 && duration <= 600000)
                        body.append("  ").append(duration).append("ms");
                }
                body.append("\n");
                included++;
            }
            if (included == 0) body.append("(Aucun événement technique disponible)\n");
            body.append("```\n\n")
                    .append("> Rapport créé volontairement depuis JARVIS. ")
                    .append("Il est PUBLIC une fois publié sur GitHub. ")
                    .append("Vérifiez et modifiez le brouillon avant validation.\n");
            return body.toString();
        });
        return future.get(12, TimeUnit.SECONDS);
    }

    /** Local preview, read only after queued writes complete. */
    public static String preview(Context context) throws Exception {
        final Context app = context.getApplicationContext();
        Future<String> future = IO.submit(() -> {
            JSONArray events = read(new SecurePrefs(app));
            StringBuilder out = new StringBuilder();
            out.append("JOURNAL CHIFFRÉ JARVIS\n")
                    .append(events.length()).append(" événements récents · 14 jours maximum\n")
                    .append("Codes et durées uniquement, sans paroles ni GPS.\n");
            if (!lastFailure.isEmpty()) out.append(lastFailure).append("\n");
            int start = Math.max(0, events.length() - 45);
            for (int i = start; i < events.length(); i++) {
                JSONObject item = events.optJSONObject(i);
                if (item == null) continue;
                out.append(utc(item.optLong("time_ms", 0))).append("  ")
                        .append(item.optString("code", "UNKNOWN"));
                if (item.has("duration_ms"))
                    out.append(" · ").append(item.optInt("duration_ms")).append(" ms");
                out.append("\n");
            }
            if (events.length() == 0) out.append("Aucun événement enregistré pour l'instant.");
            return out.toString();
        });
        return future.get(12, TimeUnit.SECONDS);
    }

    /** Explicit user confirmation is required before calling. */
    public static void clear(Context context) throws Exception {
        final Context app = context.getApplicationContext();
        Future<?> f = IO.submit(() -> {
            new SecurePrefs(app).removePrivateSecret(SECRET);
            lastFailure = "";
            return null;
        });
        f.get(12, TimeUnit.SECONDS);
    }

    private static String utc(long timestamp) {
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ROOT);
        f.setTimeZone(TimeZone.getTimeZone("UTC"));
        return f.format(new Date(timestamp));
    }
}
