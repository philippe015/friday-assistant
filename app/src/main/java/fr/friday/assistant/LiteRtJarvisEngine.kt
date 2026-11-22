package fr.friday.assistant

import android.content.Context
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock

/**
 * Standalone Android LiteRT-LM 0.18.0 engine for .litertlm models.
 *
 * A model is chosen by the user from Android's document picker, copied to
 * getFilesDir(), and loaded ONLY from that private path. No PC or network API.
 * This backend complements the existing MediaPipe .task backend.
 *
 * Single model owner: all initialize/conversation/close operations take LOCK.
 * Never closes an engine if initialize() failed, per LiteRT-LM guidance.
 */
object LiteRtJarvisEngine {
    private val lock = ReentrantLock()
    private var activePath: String? = null
    private var activeEngine: Engine? = null
    /** Backend du moteur initialisé : nécessaire pour un repli sûr après JNI. */
    private var activeGpu = false

    @JvmStatic
    @Throws(IOException::class)
    fun answer(
        context: Context,
        file: File,
        prompt: String,
        cancellation: NativeAI.CancelToken?
    ): String {
        cancellation?.check()
        if (!file.isFile || file.length() < 1_000_000L) {
            throw IOException("Le modèle .litertlm n'est pas installé sur ce téléphone.")
        }
        if (!lock.tryLock(3, TimeUnit.SECONDS)) {
            throw IOException("Jarvis LiteRT-LM traite déjà une question. Réessayez après la réponse.")
        }
        try {
            cancellation?.check()
            if (activeEngine == null || activePath != file.absolutePath) {
                closeLocked()
                val prefs = SecurePrefs(context)
                val wantsGpu = prefs.get("local_jarvis_gpu", "0") == "1"
                // Compatibilité : essayer GPU puis repli CPU si le pilote GPU/OpenCL
                // est absent ou si le modèle GPU ne charge pas. Ne jamais appeler
                // close() sur un moteur dont initialize() a échoué (doc LiteRT).
                val backends = if (wantsGpu) listOf(Backend.GPU(), Backend.CPU())
                               else listOf(Backend.CPU())
                var selected: Engine? = null
                var selectedGpu = false
                var lastFailure: Exception? = null
                for (backend in backends) {
                    try {
                        val config = EngineConfig(
                            modelPath = file.absolutePath,
                            backend = backend,
                            cacheDir = context.cacheDir.absolutePath,
                            maxNumTokens = 2048
                        )
                        val candidate = Engine(config)
                        candidate.initialize()
                        selected = candidate
                        selectedGpu = backend is Backend.GPU
                        prefs.put("local_jarvis_gpu", if (selectedGpu) "1" else "0")
                        break
                    } catch (failure: Exception) {
                        lastFailure = failure
                    }
                }
                if (selected == null) {
                    val detail = lastFailure?.let { failure ->
                        val reason = failure.message?.replace(Regex("[\\r\\n]+"), " ")
                            ?.take(180) ?: "cause non détaillée"
                        " Cause native : ${failure.javaClass.simpleName} : $reason."
                    } ?: ""
                    throw IOException(
                        "Impossible d'initialiser LiteRT-LM" +
                            (if (wantsGpu) " (GPU puis CPU)" else " (CPU)") +
                            ". Vérifiez la RAM, le fichier .litertlm et OpenCL." +
                            " Un modèle compilé uniquement pour GPU peut refuser le mode CPU." +
                            detail,
                        lastFailure
                    )
                }
                activeEngine = selected
                activePath = file.absolutePath
                activeGpu = selectedGpu
            }
            cancellation?.check()
            val engine = activeEngine ?: throw IOException("Moteur LiteRT-LM indisponible.")
            try {
                // 2048 is a tokenizer limit, NOT 2048 characters. Reduce
                // oversized context before JNI and reserve output headroom.
                val preparedPrompt = LiteRtPromptBudget.fit(
                    prompt, LiteRtPromptBudget.INITIAL_UTF8_BYTES)
                val response = infer(engine, preparedPrompt)
                cancellation?.check()
                return response
            } catch (cancelled: java.util.concurrent.CancellationException) {
                // Annulation de l'utilisateur : ne pas démarrer de nouveau backend.
                throw cancelled
            } catch (memory: OutOfMemoryError) {
                throw memory
            } catch (linkage: LinkageError) {
                throw linkage
            } catch (inference: Exception) {
                // Un échec JNI peut laisser un objet de conversation ou le cache
                // GPU dans un état invalide. Ne JAMAIS réutiliser ce moteur.
                val failedOnGpu = activeGpu
                val tooManyTokens = tokenOverflow(inference)
                closeLocked()
                cancellation?.check()
                if (tooManyTokens) {
                    // INVALID_ARGUMENT: input token ids are too long is a
                    // PROMPT-size error, not evidence of GPU incompatibility.
                    // Retrying unchanged input on CPU only obscures the cause.
                    val reduced = LiteRtPromptBudget.fit(
                        prompt, LiteRtPromptBudget.RETRY_UTF8_BYTES)
                    try {
                        val retried = Engine(EngineConfig(
                            modelPath = file.absolutePath,
                            backend = if (failedOnGpu) Backend.GPU() else Backend.CPU(),
                            cacheDir = context.cacheDir.absolutePath,
                            maxNumTokens = 2048
                        ))
                        retried.initialize()
                        activeEngine = retried
                        activePath = file.absolutePath
                        activeGpu = failedOnGpu
                        cancellation?.check()
                        val recovered = infer(retried, reduced)
                        cancellation?.check()
                        return recovered
                    } catch (cancelled: java.util.concurrent.CancellationException) {
                        closeLocked()
                        throw cancelled
                    } catch (memory: OutOfMemoryError) {
                        closeLocked()
                        throw memory
                    } catch (linkage: LinkageError) {
                        closeLocked()
                        throw linkage
                    } catch (retryError: Exception) {
                        closeLocked()
                        throw IOException(
                            "LiteRT-LM : entrée trop longue pour la limite du "
                                + "modèle (2048 tokens). Le contexte a été "
                                + "raccourci et réessayé sur le même backend, "
                                + "sans résultat : " + shortReason(retryError) +
                                ". Réduisez le profil actif ou l'historique. "
                                + "Il ne s'agit pas d'une preuve de panne GPU.",
                            retryError
                        )
                    }
                }
                if (failedOnGpu) {
                    // Une seule tentative CPU, sur un moteur entièrement neuf.
                    // Certains modèles .litertlm sont compilés GPU seulement :
                    // dans ce cas on dit que le repli n'est pas compatible.
                    try {
                        val cpuEngine = Engine(EngineConfig(
                            modelPath = file.absolutePath,
                            backend = Backend.CPU(),
                            cacheDir = context.cacheDir.absolutePath,
                            maxNumTokens = 2048
                        ))
                        cpuEngine.initialize()
                        activeEngine = cpuEngine
                        activePath = file.absolutePath
                        activeGpu = false
                        cancellation?.check()
                        val cpuResponse = infer(cpuEngine, prompt)
                        cancellation?.check()
                        SecurePrefs(context).put("local_jarvis_gpu", "0")
                        return cpuResponse
                    } catch (cancelled: java.util.concurrent.CancellationException) {
                        closeLocked()
                        throw cancelled
                    } catch (memory: OutOfMemoryError) {
                        closeLocked()
                        throw memory
                    } catch (linkage: LinkageError) {
                        closeLocked()
                        throw linkage
                    } catch (cpuFailure: Exception) {
                        closeLocked()
                        throw IOException(
                            "LiteRT-LM : erreur d'inférence GPU (" +
                                shortReason(inference) + "), et repli CPU impossible (" +
                                shortReason(cpuFailure) + "). Le modèle .litertlm" +
                                " peut être incompatible avec ce téléphone, notamment" +
                                " s'il est compilé uniquement pour GPU." +
                                " Essayez un modèle LiteRT-LM CPU compatible, sans" +
                                " désinstaller JARVIS ni effacer vos conversations.",
                            cpuFailure
                        )
                    }
                }
                throw IOException(
                    "LiteRT-LM : erreur d'inférence sur CPU (" +
                        shortReason(inference) + "). Moteur réinitialisé pour" +
                        " la prochaine question. Vérifiez la compatibilité du" +
                        " modèle, la RAM et la longueur du contexte.",
                    inference
                )
            }
        } catch (memory: OutOfMemoryError) {
            closeLocked()
            throw IOException(
                "Mémoire RAM insuffisante pour ce modèle. " +
                    "Choisissez un modèle LiteRT-LM plus petit.", memory
            )
        } catch (native: LinkageError) {
            closeLocked()
            throw IOException(
                "Bibliothèques natives LiteRT-LM indisponibles sur ce téléphone : " +
                    native.javaClass.simpleName, native
            )
        } catch (cancelled: java.util.concurrent.CancellationException) {
            // Le STOP bouton annule le tour sans transformer l'annulation en
            // panne native et sans déclencher de réinitialisation intempestive.
            throw cancelled
        } catch (failure: IOException) {
            throw failure
        } catch (failure: Exception) {
            closeLocked()
            throw IOException(
                "Échec LiteRT-LM : " + shortReason(failure) +
                    ". Vérifiez le modèle, la RAM et le backend Android.", failure
            )
        } finally {
            lock.unlock()
        }
    }

    /** Never switch GPU -> CPU for an oversized prompt. */
    private fun tokenOverflow(error: Throwable): Boolean {
        var problem: Throwable? = error
        for (depth in 0..4) {
            val message = problem?.message?.lowercase() ?: return false
            if (message.contains("input token ids are too long")
                    || message.contains("exceeding the maximum number of tokens")
                    || message.contains("maximum number of input tokens"))
                return true
            problem = problem.cause
        }
        return false
    }

    private fun infer(engine: Engine, prompt: String): String {
        val answer = engine.createConversation().use { conversation ->
            conversation.sendMessage(prompt).toString().trim()
        }
        if (answer.isEmpty()) throw IOException("LiteRT-LM n'a généré aucun texte.")
        return answer
    }

    /** Diagnostic local court, sans chemin privé ni contenu du profil/mémoire. */
    private fun shortReason(failure: Throwable): String {
        val message = failure.message.orEmpty()
            .replace(Regex("[\\r\\n]+"), " ")
            .replace(Regex("/(?:data|storage)/[^\\s:]+"), "[fichier privé]")
            .take(145)
        return failure.javaClass.simpleName +
            (if (message.isBlank()) "" else " : " + message)
    }

    private fun closeLocked() {
        val old = activeEngine
        activeEngine = null
        activePath = null
        activeGpu = false
        if (old != null) {
            try { old.close() } catch (_: Exception) { }
        }
    }

    /** Returns false instead of replacing weights while native inference is active. */
    @JvmStatic
    fun releaseAndWait(): Boolean {
        if (!lock.tryLock(30, TimeUnit.SECONDS)) return false
        try {
            closeLocked()
            return true
        } finally {
            lock.unlock()
        }
    }
}
