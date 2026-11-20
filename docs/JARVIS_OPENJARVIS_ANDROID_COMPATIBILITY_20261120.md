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

## Extension proposée : rappels d'anciens échanges chiffrés

Sur cette branche, l'assistant ne se limite plus aux dix
derniers messages lorsque l'utilisateur demande explicitement :
« Jarvis, tu te souviens de notre conversation sur le chauffage ? ».

- DialogueRecallPolicy sélectionne **au plus deux** anciens tours par
  correspondance de mots normalisés (accents et casse ignorés).
- DialogueContext injecte un extrait limité des tours retrouvés dans le
  prompt du **moteur IA local Android**. Aucun nouveau fichier personnel
  ni base de données n'est créé : on relit l'historique déjà chiffré.
- Une question générale ou Web ne déclenche **pas** cette récupération.
  Les recherches Internet reçoivent uniquement la question de recherche,
  jamais le rappel privé.
- Sans correspondance, Jarvis ne doit pas inventer d'anciens messages.
  Le système n'est **pas** une mémoire sémantique : synonymes, homonymes,
  messages effacés ou au-delà de la limite de 80 événements peuvent manquer.
- L'interface de suppression et la gestion actuelle de la mémoire restent
  inchangées. L'appareil physique doit valider une question avec reprise du
  sujet, puis un STOP et une nouvelle question.

Cette extension suit l'idée **Tools & Memory** d'OpenJarvis, sans embarquer
son runtime Python ni ses mécanismes autonomes. Ce n'est pas une synchronisation
des conversations avec OpenJarvis, GitHub ou un autre service.

## Dépôts candidats pour les prochaines intégrations Android

| Dépôt source | Fonction | Décision de compatibilité |
| --- | --- | --- |
| https://github.com/k2-fsa/sherpa-onnx | VAD, reconnaissance et mots d'activation locaux | **Candidat prioritaire à tester** en parallèle de Vosk, seulement après validation du français, de l'ABI arm64, des modèles et de l'empreinte mémoire |
| https://github.com/ggml-org/whisper.cpp | Transcription vocale multilingue | Exemples Android existants ; **alternative**, pas une deuxième capture micro simultanée |
| https://github.com/google-ai-edge/LiteRT-LM | Moteur IA Android, outils et inférence | **Déjà intégré** via dépendance Android ; vérifier la compatibilité API avant tout appel d'outil nouveau |
| https://github.com/ggml-org/llama.cpp | Inférence GGUF native | Support Android/NDK, mais **non intégré** à l'APK ; GGUF incompatible avec l'import .task/.litertlm existant |
| https://github.com/open-jarvis/OpenJarvis | Architecture en primitives, agents et mémoire | **Référence conceptuelle** ; le runtime Python/Rust et les fonctions desktop ne sont pas des dépendances Android directement intégrables |

La licence et les conditions de chaque dépôt et de chaque modèle doivent être
revues au moment de l'intégration. Ne pas changer l'API, le moteur vocal ou le
format de modèle de la version stable sans tests sur téléphone.

### Validation de cette proposition

1. Exécuter bash scripts/tester_sources.sh : les tests Java et Python
   contrôlent le rappel et les protections existantes.
2. Compiler l'APK avec la CI Android du dépôt. Le rappel local n'ajoute
   **aucune dépendance Gradle native**.
3. Sur le téléphone : aborder un sujet, en poser au moins dix autres,
   puis demander « Jarvis, tu te souviens de notre conversation sur le chauffage ? ».
4. Vérifier les mêmes étapes après redémarrage, le silence en cas de mémoire
   absente, l'absence de requête Web non demandée et la suppression de l'historique.
5. Tester le micro et le STOP : cette modification ne change ni FridayService
   ni le pipeline audio.

## Prochaines étapes candidates (non implémentées ici)

- Pipeline ASR/TTS plus moderne et adapté au français selon validation sur appareils réels.
- Recherche par étapes multi-sources avec citations et vérification des erreurs.
- Mémoire conversationnelle longue durée par recherche locale, suppression et rétention contrôlée.
- Catalogue de skills signés et à liste blanche, autorisation explicite par capacité, sans exécution arbitraire.

**Validation de cette version** : tests unitaires Java et Python, compilation Android ; la qualité du micro, l'autonomie et l'inférence Gemma restent à éprouver sur le téléphone.
