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
                val isGpu = file.name.contains("-gpu", ignoreCase = true)
                val config = EngineConfig(
                    modelPath = file.absolutePath,
                    backend = if (isGpu) Backend.GPU() else Backend.CPU(),
                    cacheDir = context.cacheDir.absolutePath
                )
                val candidate = Engine(config)
                try {
                    candidate.initialize()
                } catch (failure: Exception) {
                    // The documentation explicitly forbids closing an engine
                    // that failed initialize(), so it is not cached or closed.
                    throw IOException(
                        "Impossible d'initialiser LiteRT-LM. Vérifiez la RAM, le modèle et " +
                            (if (isGpu) "le pilote GPU/OpenCL du téléphone." else "la compatibilité CPU."),
                        failure
                    )
                }
                activeEngine = candidate
                activePath = file.absolutePath
            }
            cancellation?.check()
            val engine = activeEngine ?: throw IOException("Moteur LiteRT-LM indisponible.")
            val answer = engine.createConversation().use { conversation ->
                conversation.sendMessage(prompt).toString().trim()
            }
            cancellation?.check()
            if (answer.isEmpty()) throw IOException("LiteRT-LM n'a généré aucun texte.")
            return answer
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
        } catch (failure: IOException) {
            throw failure
        } catch (failure: Exception) {
            throw IOException(
                "Échec d'inférence LiteRT-LM : " + failure.javaClass.simpleName +
                    ". Vérifiez le modèle et les capacités GPU du téléphone.", failure
            )
        } finally {
            lock.unlock()
        }
    }

    private fun closeLocked() {
        val old = activeEngine
        activeEngine = null
        activePath = null
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
