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

    /** Snapshot returns only fixed diagnostic codes. Call from background thread. */
    public static String exportJson(Context context) throws Exception {
        final Context app = context.getApplicationContext();
        Future<String> future = IO.submit(() -> {
            JSONArray entries = read(new SecurePrefs(app));
            JSONObject report = new JSONObject();
            report.put("format", "jarvis-diagnostics");
            report.put("schema", 1);
            report.put("generated_utc", utc(System.currentTimeMillis()));
            report.put("app_version", FridayVersionGuard.thisVersion(app));
            report.put("android_sdk", Build.VERSION.SDK_INT);
            report.put("retention_days", 14);
            report.put("privacy", "Événements prédéfinis et durées seulement. "
                    + "Aucune transcription, conversation, coordonnée GPS, URL, "
                    + "clé API, identité ou prompt. Export manuel non chiffré.");
            report.put("events", entries);
            return report.toString(2);
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
