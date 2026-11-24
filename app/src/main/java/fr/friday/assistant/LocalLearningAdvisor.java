package fr.friday.assistant;

import android.content.Context;
import java.io.IOException;
import org.json.JSONObject;

/**
 * Local-only self-critique. Deterministic checks take precedence over
 * another LLM sample. All model judgments remain unverified proposals.
 * No remote API, background code update, silent corrections or training.
 */
public final class LocalLearningAdvisor {
    private LocalLearningAdvisor() { }

    private static String report(LocalCritiquePolicy.Assessment a) throws Exception {
        return new JSONObject()
                .put("verdict", a.verdict)
                .put("motif", a.reason)
                .put("proposition", a.correction)
                .put("verificationOutil", a.deterministic)
                .toString();
    }

    public static String propose(Context context, SecurePrefs prefs,
                                 String question, String answer) throws Exception {
        if (!LocalLearningMemory.enabled(prefs))
            throw new IOException("Activez d'abord l'apprentissage local dans Matrix.");
        String prompt = LocalLearningPolicy.reviewPrompt(question, answer);
        if (prompt == null)
            throw new IOException("Analyse refusée : question ou réponse trop longue ou "
                    + "incomplète. Aucun texte n'est tronqué.");

        // An exact on-device calculation can be verified without an LLM,
        // even if no language model has been imported.
        LocalCritiquePolicy.Assessment math =
                LocalCritiquePolicy.verifyArithmetic(question, answer);
        if (math != null) return report(math);

        if (!LocalJarvisEngine.ready(context)
                || !"1".equals(prefs.get("local_jarvis_enabled", "0")))
            throw new IOException("Aucun modèle IA local actif et compatible. "
                    + "Importez puis activez le modèle .task ou .litertlm.");

        // The raw review path has no profile, private history, Web sources,
        // approved memories or language-model identity rewriting.
        String raw = LocalJarvisEngine.reviewRaw(
                context, prefs, prompt, new NativeAI.CancelToken());
        boolean current = WebSearch.requiresCurrentEvidence(question);
        LocalCritiquePolicy.Assessment assessment =
                LocalCritiquePolicy.parseModel(raw, current);
        return report(assessment);
    }
}
