package fr.friday.assistant;

import android.content.Context;
import android.os.SystemClock;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * User-triggered synthetic 6-turn LLM continuity test. Uses Android's local
 * MediaPipe/LiteRT-LM runtime, NEVER the real history, Web or user profile.
 */
public final class LocalDialogueBenchmark {
    private LocalDialogueBenchmark() {}
    private static final String VAULT = "jarvis_local_dialogue_benchmark_v1";

    public static final class Session {
        private final AtomicBoolean running = new AtomicBoolean(false);
        private volatile NativeAI.CancelToken token;
        private volatile String state = "idle";
        private volatile String message = "Aucun test de dialogue lancé.";
        private volatile int done;
        private volatile String lastReport = "";

        public String start(Context context, SecurePrefs prefs) {
            if (!"1".equals(prefs.get("local_jarvis_enabled", "0")))
                return "Activez d'abord le modèle IA local.";
            if (!LocalJarvisEngine.ready(context))
                return "Aucun modèle IA importé pour effectuer ce test.";
            if (!running.compareAndSet(false, true))
                return "Un test de dialogue est déjà en cours.";
            done = 0;
            lastReport = "";
            state = "running";
            message = "Test fictif hors ligne en cours : aucune conversation privée lue.";
            token = new NativeAI.CancelToken();
            NativeAI.CancelToken current = token;
            Thread task = new Thread(() -> run(context.getApplicationContext(), prefs, current),
                    "friday-dialogue-benchmark");
            task.setDaemon(true);
            task.start();
            return "Six échanges fictifs démarrés sur le modèle local.";
        }

        public String stop() {
            if (!running.get()) return "Aucun test de dialogue en cours.";
            NativeAI.CancelToken current = token;
            if (current != null) current.cancel();
            return "Arrêt demandé ; attente de la fin de la génération native en cours.";
        }

        public String status(SecurePrefs prefs) {
            String report = lastReport;
            if (report.isEmpty() && !running.get()) {
                try { report = prefs.secret(VAULT); }
                catch (Exception unavailable) { report = ""; }
            }
            try {
                JSONObject snapshot = new JSONObject();
                snapshot.put("state", state);
                snapshot.put("done", done);
                snapshot.put("total", DialogueTestPolicy.count());
                snapshot.put("message", message);
                if (!report.isEmpty()) snapshot.put("report", new JSONObject(report));
                return snapshot.toString();
            } catch (Exception ignored) {
                return "{\"state\":\"error\",\"message\":\"Rapport indisponible.\"}";
            }
        }

        private void run(Context context, SecurePrefs prefs, NativeAI.CancelToken cancellation) {
            String history = "";
            JSONArray answers = new JSONArray();
            ArrayList<Long> latencies = new ArrayList<>();
            int evidence = 0;
            int manual = 0;
            int unverified = 0;
            boolean stopped = false;
            long began = System.currentTimeMillis();
            try {
                for (int i = 0; i < DialogueTestPolicy.count(); i++) {
                    cancellation.check();
                    String question = DialogueTestPolicy.question(i);
                    String prompt = DialogueTestPolicy.prompt(history, i);
                    message = "Échange fictif " + (i + 1) + "/"
                            + DialogueTestPolicy.count() + " · IA locale en cours";
                    long startTime = SystemClock.elapsedRealtime();
                    String reply;
                    try {
                        reply = LocalJarvisEngine.dialogueTestRaw(context, prefs,
                                prompt, cancellation);
                        cancellation.check();
                    } catch (Exception | LinkageError failure) {
                        stopped = true;
                        JSONObject error = new JSONObject();
                        error.put("tour", i + 1);
                        error.put("question_publique", question);
                        error.put("statut", "ERREUR_INFERENCE");
                        error.put("cause", failure.getClass().getSimpleName());
                        answers.put(error);
                        break;
                    }
                    long elapsed = Math.max(0, SystemClock.elapsedRealtime() - startTime);
                    String verdict = DialogueTestPolicy.evaluation(i, reply);
                    if ("INDICE_THEME_REPRIS".equals(verdict)
                            || "INDICE_CONTEXTE_REPRIS".equals(verdict)) evidence++;
                    else if ("LECTURE_MANUELLE".equals(verdict)) manual++;
                    else unverified++;
                    latencies.add(elapsed);
                    JSONObject item = new JSONObject();
                    item.put("tour", i + 1);
                    item.put("question_publique", question);
                    item.put("reponse_ia_locale", clip(reply, 650));
                    item.put("duree_ms", elapsed);
                    item.put("indice", verdict);
                    answers.put(item);
                    history = DialogueTestPolicy.appendTurn(history, i, reply);
                    done = i + 1;
                }
            } catch (Exception cancelled) {
                stopped = true;
            } finally {
                boolean complete = !stopped && done == DialogueTestPolicy.count();
                try {
                    JSONObject report = new JSONObject();
                    report.put("schema", 1);
                    report.put("version", "3.14.27-dialogue-pro-preview");
                    report.put("date_utc_ms", began);
                    report.put("mode", "six échanges fictifs, IA locale brute, hors ligne");
                    report.put("moteur", LocalJarvisEngine.status(context, prefs));
                    report.put("complet", complete);
                    report.put("tours_reussis", done);
                    report.put("tours_prevus", DialogueTestPolicy.count());
                    report.put("indices_contexte", evidence);
                    report.put("lecture_manuelle", manual);
                    report.put("non_confirmes", unverified);
                    report.put("duree_mediane_ms", median(latencies));
                    report.put("avertissement", "Indicateurs lexicaux exploratoires : "
                            + "la cohérence et la fidélité du dialogue doivent être "
                            + "vérifiées manuellement. Ne mesure pas le QI.");
                    report.put("details", answers);
                    lastReport = report.toString();
                    try {
                        prefs.setSecret(VAULT, lastReport);
                    } catch (Exception vault) {
                        message = "Rapport temporaire : stockage chiffré indisponible.";
                    }
                } catch (Exception ignored) {
                    lastReport = "";
                }
                state = complete ? "completed" : "partial";
                if (message.startsWith("Échange fictif") || message.startsWith("Test fictif"))
                    message = complete
                            ? "Six réponses locales reçues : vérifiez le suivi du contexte."
                            : "Test interrompu ou échec : rapport partiel disponible.";
                token = null;
                running.set(false);
            }
        }

        private static long median(ArrayList<Long> values) {
            if (values.isEmpty()) return -1;
            Collections.sort(values);
            int n = values.size();
            if (n % 2 != 0) return values.get(n / 2);
            return (values.get(n / 2 - 1) + values.get(n / 2)) / 2;
        }
        private static String clip(String value, int max) {
            if (value == null) return "";
            return value.length() <= max ? value : value.substring(0, max);
        }
    }
}
