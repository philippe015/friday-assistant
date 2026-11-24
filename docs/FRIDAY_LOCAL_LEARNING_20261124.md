# FRIDAY · Auto-évolution IA locale supervisée (préversion)

## Objectif
FRIDAY analyse ses propres réponses au moyen du modèle IA **installé sur le
téléphone** (.task MediaPipe ou .litertlm LiteRT-LM). Ce modèle propose une
amélioration facultative. **Il ne réentraîne pas les poids** et ne modifie pas
automatiquement le code, le modèle ou les autorisations Android.

## Utilisation dans Terminal Matrix
1. Ouvrir **◈ AUTO-ÉVOLUTION · IA LOCALE SUPERVISÉE**.
2. Appuyer sur **Apprentissage : OFF** pour l'activer (désactivé par défaut).
3. Après une réponse Matrix, appuyer sur **IA locale : analyser**.
4. L'IA locale génère une proposition courte, susceptible de contenir des erreurs.
5. Vérifier, modifier et appuyer sur **Approuver et enregistrer chiffré**.
6. Lors d'une **question strictement identique** (après normalisation), la
   correction peut être communiquée au moteur local dans son contexte.
7. **Auto-analyse : ON** effectue cette revue après chaque réponse écrite Matrix.
   Elle ne mémorise jamais la proposition sans validation explicite.
8. Les boutons **✓ Utile**, **✕ Insuffisant** enregistrent une évaluation sans
   prétendre que le modèle a été entraîné.
9. **Effacer les corrections** requiert deux appuis.

## Confidentialité et limites
- Stockage de 40 questions maximum avec corrections de 600 caractères maximum.
- AES-GCM / Android Keystore via SecurePrefs ; aucun repli en clair.
- Aucun envoi automatique à GitHub ou à un serveur de modèles.
- Une correction ne sert **pas** de source scientifique ou d'actualité
  vérifiée et n'est pas injectée dans les recherches Web et traductions RSS.
- Le modèle peut halluciner lors de sa propre révision : l'apprentissage
  reste supervisé. Les propositions sont affichées comme **non vérifiées**.
- L'auto-analyse sollicite la RAM et le processeur ; sur les petits
  téléphones, laisser cette option OFF et analyser à la demande.
- Le modèle doit être présent, activé et réellement capable de générer.
  Un APK compilé ne prouve pas que MediaPipe ou LiteRT fonctionne sur
  tous les téléphones.
- Les réponses vocales et écrites utilisent la même mémoire d'apprentissage
  approuvée lors des prochaines requêtes locales ; la revue automatique est
  actuellement déclenchée par les réponses du Terminal Matrix.

## Fichiers
- app/src/main/java/fr/friday/assistant/LocalLearningPolicy.java
- app/src/main/java/fr/friday/assistant/LocalLearningMemory.java
- app/src/main/java/fr/friday/assistant/LocalLearningAdvisor.java
- app/src/main/java/fr/friday/assistant/LocalJarvisEngine.java
- app/src/main/java/fr/friday/assistant/MainActivity.java
- app/src/main/assets/index.html
- tests/test_friday_local_learning_20261124.py

Ne pas présenter ce mécanisme comme un entraînement LoRA, un fine-tuning
ou une modification autonome des poids du modèle : il s'agit d'une
**amélioration par mémoire locale approuvée**.
