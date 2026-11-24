package fr.friday.assistant;

import android.app.ActivityManager;
import android.content.Context;
import android.os.SystemClock;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Explicit, user-started ON-DEVICE test. No HTTP, RSS, microphone, private
 * dialogue, GPS, user prompts, model downloads or server logging.
 *
 * Reports are stored only in the Android Keystore AES-GCM-backed SecurePrefs.
 * The backend cannot certify overall intelligence from these small tests.
 */
public final class LocalModelBenchmark {
    private LocalModelBenchmark() {}
    private static final String VAULT_KEY = "jarvis_local_benchmark_v1";

    public static final class Session {
        private final AtomicBoolean running = new AtomicBoolean(false);
        private volatile NativeAI.CancelToken currentToken;
        private volatile String state = "idle";
        private volatile String message = "Aucun test lancé sur ce téléphone.";
        private volatile int done;
        private volatile String currentReport = "";

        /** The WebView bridge invokes start on a non-UI Javascript thread. */
        public String start(Context context, SecurePrefs prefs) {
            if (!"1".equals(prefs.get("local_jarvis_enabled", "0")))
                return "Activez d'abord le moteur Jarvis local.";
            if (!LocalJarvisEngine.ready(context))
                return "Aucun modèle local prêt. Importez et testez un modèle .task ou .litertlm.";
            if (!running.compareAndSet(false, true))
                return "Un banc d'essai du modèle est déjà en cours.";
            done = 0;
            currentReport = "";
            currentToken = new NativeAI.CancelToken();
            state = "running";
            message = "Préparation des dix questions fixes hors ligne.";
            Context app = context.getApplicationContext();
            NativeAI.CancelToken token = currentToken;
            Thread worker = new Thread(() -> run(app, prefs, token),
                    "friday-local-benchmark");
            worker.setDaemon(true);
            worker.start();
            return "Banc d'essai local démarré. Aucune question privée n'est utilisée.";
        }

        public String stop() {
            if (!running.get()) return "Aucun test en cours.";
            NativeAI.CancelToken token = currentToken;
            if (token != null) token.cancel();
            message = "Interruption demandée : arrêt après l'inférence en cours.";
            return message;
        }

        public String status(SecurePrefs prefs) {
            String report = currentReport;
            if (report.isEmpty() && !running.get()) {
                try { report = prefs.secret(VAULT_KEY); }
                catch (Exception unavailable) { report = ""; }
            }
            try {
                JSONObject obj = new JSONObject();
                obj.put("state", state);
                obj.put("done", done);
                obj.put("total", LocalBenchmarkPolicy.count());
                obj.put("message", message);
                if (!report.isEmpty()) obj.put("report", new JSONObject(report));
                return obj.toString();
            } catch (Exception invalid) {
                return "{\"state\":\"error\",\"message\":\"Rapport non disponible.\"}";
            }
        }

        private void run(Context context, SecurePrefs prefs, NativeAI.CancelToken token) {
            JSONArray results = new JSONArray();
            long beganAt = System.currentTimeMillis();
            long ramBefore = availableRam(context);
            ArrayList<Long> timings = new ArrayList<>();
            int positive = 0;
            int unconfirmed = 0;
            int manual = 0;
            int errors = 0;
            boolean cancelled = false;
            try {
                for (int i = 0; i < LocalBenchmarkPolicy.count(); i++) {
                    token.check();
                    LocalBenchmarkPolicy.Challenge test = LocalBenchmarkPolicy.item(i);
                    message = "Question " + (i + 1) + "/" + LocalBenchmarkPolicy.count()
                            + " · " + test.category + " · génération réelle en cours";
                    long startedAt = SystemClock.elapsedRealtime();
                    String response = "";
                    String verdict = "";
                    String error = "";
                    try {
                        // Raw model output, never LocalJarvisArithmetic, history, Web or persona.
                        response = LocalJarvisEngine.benchmarkRaw(
                                context, prefs, LocalBenchmarkPolicy.prompt(i), token);
                        token.check();
                        verdict = LocalBenchmarkPolicy.evaluate(i, response);
                    } catch (OutOfMemoryError memory) {
                        error = "Mémoire insuffisante pour poursuivre.";
                        verdict = "ERREUR";
                        errors++;
                        cancelled = true;
                    } catch (Exception | LinkageError failure) {
                        if (Thread.currentThread().isInterrupted()) {
                            cancelled = true;
                            break;
                        }
                        error = failure.getClass().getSimpleName()
                                + " : inférence indisponible ou interrompue.";
                        verdict = "ERREUR";
                        errors++;
                    }
                    long duration = Math.max(0, SystemClock.elapsedRealtime() - startedAt);
                    if (!"ERREUR".equals(verdict)) {
                        timings.add(duration);
                        switch (verdict) {
                            case "INDICE_POSITIF": positive++; break;
                            case "LECTURE_MANUELLE": manual++; break;
                            default: unconfirmed++; break;
                        }
                    }
                    JSONObject entry = new JSONObject();
                    entry.put("id", test.id);
                    entry.put("categorie", test.category);
                    entry.put("question_publique", test.question);
                    entry.put("duree_ms", duration);
                    entry.put("verdict_indicatif", verdict);
                    entry.put("reponse", clip(response, 450));
                    entry.put("erreur", clip(error, 180));
                    results.put(entry);
                    done = results.length();
                    if (cancelled) break;
                }
            } catch (Exception interruption) {
                cancelled = true;
            } finally {
                boolean completed = !cancelled && done == LocalBenchmarkPolicy.count();
                String json;
                try {
                    JSONObject report = new JSONObject();
                    report.put("schema", 1);
                    report.put("version", "3.14.26-local-benchmark-preview");
                    report.put("date_utc_ms", beganAt);
                    report.put("mode", "inférence locale brute hors ligne ; questions fixes publiques");
                    report.put("moteur", LocalJarvisEngine.status(context, prefs));
                    report.put("modele_octets", LocalJarvisEngine.modelFile(context).length());
                    report.put("questions_terminees", done);
                    report.put("questions_prevues", LocalBenchmarkPolicy.count());
                    report.put("complet", completed);
                    report.put("interrompu", cancelled);
                    report.put("indices_positifs", positive);
                    report.put("non_confirmes", unconfirmed);
                    report.put("lecture_manuelle", manual);
                    report.put("erreurs_inference", errors);
                    report.put("duree_mediane_ms", median(timings));
                    report.put("ram_libre_depart_mo", ramBefore < 0 ? JSONObject.NULL : ramBefore / 1_000_000L);
                    long ramAfter = availableRam(context);
                    report.put("ram_libre_fin_mo", ramAfter < 0 ? JSONObject.NULL : ramAfter / 1_000_000L);
                    report.put("memoire_note", "RAM disponible Android, non consommation mémoire du modèle");
                    report.put("avertissement", "Indicateurs lexicaux exploratoires, pas un score de QI "
                            + "ni une validation du raisonnement. Les réponses se relisent manuellement. "
                            + "Aucun test microphone, Web ou modèle distant.");
                    report.put("details", results);
                    json = report.toString();
                } catch (Exception unexpected) {
                    json = "";
                }
                currentReport = json;
                state = completed ? "completed" : (cancelled ? "cancelled" : "partial");
                message = completed
                        ? "10 réponses analysées ; relisez les indicateurs et les réponses."
                        : "Test partiel : interruptions, erreurs ou mémoire insuffisante.";
                if (!json.isEmpty()) {
                    try {
                        prefs.setSecret(VAULT_KEY, json);
                    } catch (Exception storage) {
                        message += " Sauvegarde chiffrée indisponible : résultat temporaire uniquement.";
                    }
                }
                currentToken = null;
                running.set(false);
            }
        }
    }

    private static long availableRam(Context context) {
        try {
            ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            if (am == null) return -1;
            ActivityManager.MemoryInfo info = new ActivityManager.MemoryInfo();
            am.getMemoryInfo(info);
            return info.availMem;
        } catch (Exception unavailable) {
            return -1;
        }
    }

    private static long median(ArrayList<Long> elapsed) {
        if (elapsed.isEmpty()) return -1L;
        Collections.sort(elapsed);
        int center = elapsed.size() / 2;
        if (elapsed.size() % 2 == 1) return elapsed.get(center);
        return (elapsed.get(center - 1) + elapsed.get(center)) / 2;
    }

    private static String clip(String text, int max) {
        if (text == null) return "";
        return text.length() <= max ? text : text.substring(0, max);
    }
}
