# FRIDAY 3.14.21 — Terminal Matrix Preview (Android ARM64)

**Préversion Android pour tests, indépendante de FRIDAY 3.14.20.**
Cette publication ne met pas à jour l'application Google Play.

## Nouvel onglet : Terminal Matrix

- **Terminal noir et vert fluorescent**, lettres monospaces et animation discrète, avec navigation adaptée au smartphone.
- **Poser une question par écrit** : saisir dans `vous@friday:~$` puis appuyer sur **ENVOYER** ou la touche **Entrée**. Maj+Entrée permet de saisir plusieurs lignes.
- **Réponses affichées dans le terminal** depuis le pipeline JARVIS existant : même IA locale, mémoire conversationnelle chiffrée et recherche Web lorsque l'utilisateur l'a autorisée.
- **Réponses vocales automatiques** si la synthèse vocale Android est activée ; boutons **Lire la réponse**, **Stop voix** et **Écoute vocale** (Vosk).
- L'historique vocal et écrit du cockpit est partagé avec le terminal. Les conversations ne sont pas copiées dans un nouveau stockage non chiffré.
- Texte affiché en sécurité avec `textContent`. Ce « terminal » est **une interface de dialogue**, pas un shell Linux, Termux ou une console capable d'exécuter des commandes.
- Le bandeau du cockpit affiche désormais la vraie version Android installée, au lieu du numéro 3.14.18 resté dans l'interface.

## Installation

1. Télécharger **FRIDAY-3.14.21-Matrix-Preview.apk** dans les fichiers de cette Release GitHub.
2. Sur Android 8.0+ **ARM64**, ouvrir le fichier dans Téléchargements, autoriser l'installation depuis le navigateur lorsque Android le demande.
3. Lancer **FRIDAY · Terminal Matrix** (identifiant `fr.friday.assistant.v31421matrix`).
4. Autoriser le microphone pour le contrôle vocal et configurer un modèle français Vosk.
5. Importer votre modèle IA compatible `.task` ou `.litertlm` dans les réglages, puis tester **Jarvis IA locale sans Internet**.
6. Ouvrir le quatrième onglet **▦ Terminal Matrix** ; poser une question et vérifier l'apparition du texte puis la lecture vocale.
7. Tester la relecture de la réponse et le bouton d'arrêt de la voix.

## Précautions

Cette Preview a un **package différent** de l'ancienne version 3.14.20 : elle s'installe à côté, sans effacer les réglages ou modèles de cette dernière. En contrepartie, il faudra configurer ses propres permissions et réimporter les modèles. La signature est une signature de test, non destinée à Google Play.

Le modèle IA n'est **pas inclus** dans l'APK. La compatibilité GPU, la mémoire disponible et l'écoute permanente restent à confirmer sur téléphone réel. L'accès Web est facultatif, soumis aux réglages du moteur existant ; des termes de recherche peuvent être envoyés à des services tiers lorsque Web est activé. Les exports de conversations sont sensibles : ne pas les publier dans les issues GitHub.

## Documentation et vérification

- [Manuel Terminal Matrix](https://github.com/philippe015/friday-assistant/blob/feature/friday-matrix-terminal-20261123/docs/FRIDAY_MATRIX_TERMINAL_20261123.md)
- [Code source et corrections (PR #116)](https://github.com/philippe015/friday-assistant/pull/116)
- Tests Python/Node JS et compilation Android via GitHub Actions.
- Le fichier `SHA256SUMS.txt` fourni par la Release permet de contrôler l'intégrité du binaire.

**Aucun changement de l'application Play Console `jarvis.myapp`.**
