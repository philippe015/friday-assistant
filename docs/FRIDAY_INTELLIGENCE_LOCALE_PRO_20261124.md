# FRIDAY/JARVIS 3.14.26 — Intelligence Locale Pro+ (diagnostic embarqué)

Date : 24 novembre 2026. Cette préversion est basée sur la branche 3.14.25 Intelligence Conversationnelle+. PR chaînée, non fusionnée à main.

## Ce qui est livré

Un panneau Terminal Matrix → INTELLIGENCE LOCALE PRO+ · BANC D'ESSAI HORS LIGNE. Appuyez sur Tester le modèle local (10 questions).

- Vérification préalable : moteur IA local activé et fichier modèle réellement présent.
- Dix questions publiques constantes, sur calcul, logique, connaissance, traduction, français, programmation et raisonnement.
- Inférence réellement exécutée avec MediaPipe ou LiteRT-LM, sans raccourci arithmétique, recherche Web, prompt de personnalité, souvenir, correction, historique personnel, GPS, microphone ou modèle distant.
- Avancement 0 à 10 ; temps de génération de chaque réponse ; durée médiane sur les générations réussies et RAM libre Android avant/après. La RAM libre n'est pas l'empreinte mémoire utilisée par le modèle. Le débit en tokens/s n'est pas calculé faute de tokenisation observée fiable.
- Contrôles lexicaux pour huit questions : INDICE_POSITIF ou NON_CONFIRME selon les mots ou nombres présents. Deux questions demandent une lecture manuelle. Ce n'est ni une note d'intelligence ni la preuve d'un raisonnement ; relire chaque réponse brute.
- Interruption volontaire. Elle peut prendre effet seulement à la fin d'une génération native selon le runtime.
- Rapport stocké exclusivement dans SecurePrefs Android Keystore AES-GCM sous la clé jarvis_local_benchmark_v1, sans stockage en clair de secours.
- Copie volontaire vers le presse-papiers avec avertissement : le presse-papiers n'est pas chiffré. Aucun envoi automatique vers Web, GitHub ou un autre serveur.

## Limitations

Ce banc d'essai ne télécharge, n'entraîne ni ne remplace le modèle. Il n'ajoute pas un routeur neuronal universel ou un système d'embeddings. Les temps dépendent de l'appareil, de sa température, du runtime et des tâches simultanées. Comparer plusieurs exécutions dans des conditions similaires.

Les questions sont fixes et publiques. L'historique, les notes, les prompts personnalisés, les conversations et les coordonnées GPS restent exclus du test.

## Validation sur téléphone

1. Installer l'APK debug sur Android ARM64. Sauvegarder la mémoire privée avant toute désinstallation éventuellement nécessaire en cas de signature incompatible.
2. Activer et importer un modèle compatible .task ou .litertlm, puis vérifier la génération avec le test IA locale existant.
3. Ouvrir Matrix → Banc d'essai et lancer les dix questions ; si possible arrêter d'autres inférences pendant la mesure.
4. Lire le rapport et les réponses brutes. Il reste chiffré dans l'application jusqu'à une copie explicite.
5. Comparer avec d'autres modèles compatibles sans interpréter les indices lexicaux comme un score de QI.

Tests CI : JVM des dix critères, garde-fous d'absence d'Internet et de mémoire personnelle, compilation Android debug.

Identifiant debug : fr.friday.assistant.v31421matrix. Version 3.14.26-local-benchmark-preview, code 120. La version release Google Play 3.14.18 code 112 reste inchangée.
