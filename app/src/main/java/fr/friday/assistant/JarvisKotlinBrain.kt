package fr.friday.assistant

import java.text.Normalizer
import java.util.Locale

/**
 * Triage conversationnel déterministe, 100 % local, sans permission Android.
 * Une dictée douteuse ou un refus bref ne devient plus une recherche Web.
 * Ne lit aucune clé, n'envoie aucune requête et ne conserve aucun dialogue.
 */
object JarvisKotlinBrain {
    private val repeatedReject = setOf("non non", "non merci", "pas du tout", "deux fois non")
    private val acknowledgement = setOf("ok", "okay", "d accord", "entendu", "tres bien", "oui d accord")
    private val thanks = setOf("merci", "merci beaucoup", "merci jarvis")
    private val bareCity = Regex("^[a-z][a-z-]+(?: [a-z][a-z-]+){1,3}(?: [0-9]{2,5})?$")
    private val cityBadStart = setOf("je", "tu", "il", "elle", "nous", "vous", "et",
        "quel", "quelle", "quels", "quelles", "comment", "pourquoi", "cherche",
        "recherche", "donne", "dis", "ouvre", "arrete", "meteo", "oui", "non")

    private fun fold(value: String?): String {
        val normalized = Normalizer.normalize(value.orEmpty(), Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "").lowercase(Locale.FRENCH)
        return normalized.replace(Regex("[^a-z0-9]+"), " ")
            .replace(Regex("\\s+"), " ").trim()
            .replace(Regex("^(?:jarvis|jarvise|jervis|friday) "), "")
    }

    /** Un score Vosk absent ne doit jamais être assimilé à une erreur. */
    @JvmStatic
    fun needsVoiceClarification(heard: String?, confidence: Double, explicitWake: Boolean): Boolean {
        if (!confidence.isFinite() || confidence <= 0.0 || confidence >= 1.0) return false
        val words = fold(heard).split(' ').filter { it.isNotBlank() }
        if (words.size < 2 || words.size > 48) return false
        val threshold = if (explicitWake) 0.40 else 0.62
        return confidence < threshold
    }

    /**
     * Retourne null pour les vraies questions ; elles restent gérées par
     * les outils Android/Internet et le moteur IA existants.
     * privateDialogue provient exclusivement de l'historique local chiffré.
     */
    @JvmStatic
    fun localClarification(question: String?, privateDialogue: String?): String? {
        val q = fold(question)
        if (q.isEmpty() || q.length > 95) return null
        val assistantLines = privateDialogue.orEmpty().lineSequence()
            .filter { it.startsWith("Jarvis : ") }.toList()
        val latestAssistant = assistantLines.lastOrNull()?.let(::fold).orEmpty()
        val recentlyAskedWeather = assistantLines.takeLast(2)
            .any { fold(it).contains("pour quelle ville souhaitez vous la meteo") }

        if (q == "non" || q in repeatedReject) {
            return if (latestAssistant.contains("pour quelle ville souhaitez vous la meteo")) {
                "D'accord. Je ne lance pas de recherche météo. " +
                    "Indiquez une ville si vous souhaitez reprendre cette demande."
            } else {
                "D'accord. Que souhaitez-vous corriger ou préciser ?"
            }
        }
        if (q == "oui") return "D'accord. Quelle précision souhaitez-vous ajouter ?"
        if (q in acknowledgement) return "Je vous écoute. Quelle est votre prochaine demande ?"
        if (q in thanks) return "Avec plaisir. Je vous écoute."

        // Une formule répétée sans action ne doit pas envoyer « suite » à Wikipédia.
        if (q == "tout de suite c est tout de suite")
            return "Que souhaitez-vous que je fasse immédiatement ? " +
                "Précisez l'action ou la question."

        // « Notre article un humain » : référence ambiguë, pas un ordre de lecture.
        if (q.matches(Regex("^(?:notre|cet|ce) article (?:[1-7]|un|deux|trois|quatre|cinq|six|sept) [a-z-]+(?: [a-z-]+){0,2}$")))
            return "Vous parlez d'un article précis ? " +
                "Dites « lis l'article un en entier » pour le dernier bulletin, " +
                "ou précisez le sujet souhaité."

        // La ville seule, après une question météo récente, mérite clarification
        // si l'outil météo existant n'a pas déjà résolu la ville.
        if (recentlyAskedWeather && bareCity.matches(q)
            && q.substringBefore(' ') !in cityBadStart) {
            return "Vous indiquez « " + question.orEmpty().trim().take(70) +
                " ». Confirmez-vous que vous souhaitez la météo de cette commune ?"
        }
        return null
    }
}
