# FRIDAY 3.14.24 — Memory Evolution+ (module 3)

## But et fonctionnement réel

Extension de la mémoire d'apprentissage chiffrée de FRIDAY/JARVIS :
- 100 entrées maximum (au lieu de 40), toujours AES-GCM Android Keystore
  et dans le même secret privé \`jarvis_learning_v1\`.
- Conservation et migration des corrections et évaluations existantes,
  y compris les anciennes évaluations sans correction.
- Reconnaissance **locale** de reformulations prudentes en français :
  normalisation des accents et de la ponctuation, quelques expressions
  synonymes courantes, comparaison de mots significatifs.
- Vérification explicite des nombres et marqueurs temporels/négation.
  Des questions sur Paris/Lyon, aujourd'hui/demain, 2026/2027 ou
  « avec »/« sans » ne doivent pas recevoir la même correction.
- Seuil strict de rapprochement et refus d'appliquer une note lorsqu'il
  existe plusieurs correspondances proches et ambiguës.
- Une correction est une information **saisie et approuvée par l'utilisateur**,
  jamais une affirmation vérifiée ni une instruction système.

## Conflits : aucun remplacement silencieux

Lorsque l'utilisateur approuve une nouvelle correction et qu'une correction
différente est déjà enregistrée pour une question correspondante :
1. L'ancienne correction reste chiffrée.
2. La nouvelle est placée dans \`pending_correction\` (chiffrée aussi).
3. La mémoire **suspend leur réutilisation dans le prompt** jusqu'à la
   résolution explicite.
4. Matrix affiche les deux corrections pour un choix humain :
   **Garder l'ancienne** ou **Approuver la nouvelle**.
5. L'utilisateur peut supprimer une seule entrée avec une double confirmation.

La comparaison « différente » compare le texte normalisé des corrections,
pas une vérification de vérité. Cela peut produire un conflit pour une simple
réécriture ; la validation humaine reste nécessaire.

## Interface Android

Terminal Matrix → AUTO-ÉVOLUTION → **Memory Evolution+ · voir les corrections**.
Les anciennes fonctions (Auto-Critique+, vérification sources publiques, corrections
approuvées, effacement général) sont conservées.

Les corrections ne sont pas injectées dans les recherches Web ou les traductions
RSS publiques. Les résultats de recherche ne reçoivent jamais les entrées
de Memory Evolution+.

## Limites explicites

- **Pas d'embeddings**, de base vectorielle, ni d'entraînement LoRA/fine-tuning.
  Il s'agit d'un rapprochement lexical-sémantique conservateur et hors ligne.
- Si la question change trop, la mémoire ne tente pas de deviner : elle n'ajoute
  aucune correction à la génération.
- La résolution de conflits compare des notes, pas leur véracité.
- Les 100 entrées sont une limite de sécurité et de performance, non une
  promesse de mémoire illimitée.
- L'application ne téléverse ni conversation ni correction sur GitHub.
- Un APK compilé et ses tests automatiques ne garantissent pas
  le fonctionnement de l'IA installée sur chaque téléphone.
- L'apprentissage doit être activé dans Matrix. Les propositions de l'IA
  restent soumises à approbation manuelle.

## Tests

\`tests/test_friday_memory_evolution_20261124.py\` vérifie avec JVM
les reformulations, les dates et villes distinctes, les nombres,
les négations, les conflits et l'intégration privée Android.
Les autres tests existants couvrent Auto-Critique+ et Internet.

## Version

Préversion Android DEBUG : \`3.14.24-memory-evolution-preview\`.
Identifiant DEBUG conservé : \`fr.friday.assistant.v31421matrix\`.
La configuration Google Play RELEASE n'est pas changée.
La signature de test peut être différente d'un APK déjà installé :
sauvegarder les données avant une désinstallation.
