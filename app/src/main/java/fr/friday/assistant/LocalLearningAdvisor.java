package fr.friday.assistant;

import android.content.Context;
import java.io.IOException;

/** Supervised on-device learning. Only the already installed local LLM
 * may produce a suggested improvement. No cloud call or background update.
 * A suggestion is NEVER silently accepted as a verified fact. */
public final class LocalLearningAdvisor {
    private LocalLearningAdvisor() { }

    public static String propose(Context context, SecurePrefs prefs,
                                 String question, String answer) throws Exception {
        if (!LocalLearningMemory.enabled(prefs))
            throw new IOException("Activez d'abord l'apprentissage local dans Matrix.");
        if (!LocalJarvisEngine.ready(context)
                || !"1".equals(prefs.get("local_jarvis_enabled", "0")))
            throw new IOException("Aucun modèle IA local actif et compatible. "
                    + "Importez puis activez le modèle .task ou .litertlm.");
        String prompt = LocalLearningPolicy.reviewPrompt(question, answer);
        if (prompt == null)
            throw new IOException("Question ou réponse non compatible avec une analyse ciblée.");
        // Force the actual local MediaPipe/LiteRT runtime; do not call
        // JarvisInternet, cloud APIs or the browsing tool for this review.
        String proposal = LocalJarvisEngine.answer(context, prefs, prompt,
                "", new NativeAI.CancelToken());
        String clean = LocalLearningPolicy.validCorrection(proposal);
        if (clean == null)
            throw new IOException("Le modèle local n'a pas proposé de correction valide.");
        return clean;
    }
}
