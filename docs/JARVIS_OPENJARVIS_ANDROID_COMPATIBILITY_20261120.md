# JARVIS Android — rapprochement avec OpenJarvis et dépôts compatibles

Date de revue : 20 novembre 2026.

## Référence et attribution

- **OpenJarvis officiel** : https://github.com/open-jarvis/OpenJarvis — licence Apache-2.0, projet de recherche et d'ingénierie locale fondé sur cinq primitives (*Intelligence, Engine, Agents, Tools & Memory, Learning*).
- **Google AI Edge LiteRT-LM** : https://github.com/google-ai-edge/LiteRT-LM — moteur d'inférence Android/Kotlin compatible sous réserve du bon format et du GPU ; application déjà reliée à `com.google.ai.edge.litertlm:litertlm-android`.
- **Google AI Edge Gallery** : https://github.com/google-ai-edge/gallery — référence d'intégration de modèles locaux, reconnaissance et synthèse vocale ; ne pas y copier des binaires sans examen.
- **Google MediaPipe Samples** : https://github.com/google-ai-edge/mediapipe-samples — exemples Android ; certains modèles, formats et API diffèrent des versions intégrées.
- **Vosk API** : https://github.com/alphacep/vosk-api — reconnaissance vocale hors ligne ; le projet JARVIS utilise sa dépendance Android.
- **Android** : https://developer.android.com/develop/background-work/services/fgs/restrictions-bg-start — limites de démarrage des services à microphone.

Aucun code ou modèle des projets externes n'est copié dans cette mise à jour. L'architecture native est **inspirée** d'OpenJarvis, sans affiliation ni prétendre intégrer son environnement Python de bureau.

## Compatibilité réelle

| Primitive OpenJarvis | Adaptation Android existante / nouvelle | Limite |
|---|---|---|
| Intelligence | `LocalJarvisEngine` (MediaPipe / LiteRT-LM) | Modèle .task ou .litertlm à importer ; une génération doit être testée |
| Engine | `FridayService` (Vosk / Android TTS) | Autorisations et restrictions Android, matériel variable |
| Agents | `JarvisToolPolicy` + `JarvisAndroidTools` | Routage déterministe, pas de boucle autonome multi-étapes |
| Tools & Memory | Calculatrice, date/heure, diagnostic, sources Web publiques, historique chiffré | Pas d'accès arbitraire aux fichiers, shell, contacts, autres applications ou MCP |
| Learning | Historique chiffré et notes explicitement enregistrées | Pas de fine-tuning ni d'entraînement automatique |

## Outils activés

- « Jarvis, quelles sont tes capacités ? » : capacités réelles et limites.
- « Jarvis, diagnostic » : état des modules installés.
- « Jarvis, diagnostic microphone » : autorisation Android, modèle vocal, service d'écoute.
- « Jarvis, quel est ton modèle IA ? » : import local présent ou absent, sans prétendre que le modèle fonctionne sans test.
- « Jarvis, niveau de batterie » : données publiques du téléphone, hors connexion.
- « Jarvis, confidentialité de Jarvis » : politique locale de données.
- « Jarvis, combien font 2 plus 2 ? » : calculateur déterministe existant.
- « Jarvis, quelle heure est-il ? » : heure du téléphone (pas une heure distante inventée).

Ces outils sont disponibles **à la voix et par écrit**, sans API distante, sans modèle IA importé, et avant toute recherche Internet générale. Les commandes non reconnues restent confiées au moteur local et aux sources Web existantes.

## Garde-fous et limites

1. L'orchestrateur n'exécute jamais des commandes shell, des scripts ou des « skills » téléchargés. Les noms d'outils et leurs actions sont fixés dans le code Java.
2. Aucune requête Web ou GPS ne sert à réaliser les diagnostics locaux ; les clés privées et le contenu de la mémoire n'apparaissent pas dans les résultats.
3. Les informations du système (`BatteryManager`, `ActivityManager`, `StatFs`) viennent d'Android et peuvent être indisponibles ; ne pas les inventer.
4. Une recherche Web activée n'est pas une preuve de connectivité ; un fichier modèle présent n'est pas la preuve d'une génération réussie.
5. Le micro ne peut pas être activé arbitrairement à la mise sous tension ou quand Android l'interdit. Le bouton Stop reste prioritaire.
6. Les dépôts externes ont leurs propres dépendances, licences et formats ; aucune promesse de compatibilité binaire « plug-and-play ».

## Prochaines étapes candidates (non implémentées ici)

- Pipeline ASR/TTS plus moderne et adapté au français selon validation sur appareils réels.
- Recherche par étapes multi-sources avec citations et vérification des erreurs.
- Mémoire conversationnelle longue durée par recherche locale, suppression et rétention contrôlée.
- Catalogue de skills signés et à liste blanche, autorisation explicite par capacité, sans exécution arbitraire.

**Validation de cette version** : tests unitaires Java et Python, compilation Android ; la qualité du micro, l'autonomie et l'inférence Gemma restent à éprouver sur le téléphone.
