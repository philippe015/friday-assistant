# Changelog

Format inspiré de [Keep a Changelog](https://keepachangelog.com/fr/1.1.0/),
versionnage [SemVer](https://semver.org/lang/fr/).

## [2.2.2] — 2026-11-15

### Corrigé
- **Clé Mistral « absente » malgré la saisie** : si le Keystore de l'appareil échoue,
  la clé est désormais conservée en **repli** (et l'incident est affiché), au lieu d'être perdue.
- L'écran de réglages affiche l'état réel de la clé : `✔ enregistrée` / `✘ absente`
  avec le détail d'une éventuelle erreur Keystore.
- Message d'erreur explicite : « Réglages natifs → collez la clé → Enregistrer ».

## [2.2.1] — 2026-11-15

### Corrigé
- **Modèles Mistral par défaut** : `mistral-small-latest` renvoie `429 Rate limit exceeded`
  sur les offres actuelles. Le chat utilise désormais `ministral-8b-latest`
  (fonctionne aussi avec `ministral-14b-2512`).
- **Modèle vision** : `mistral-small-2506` n'existe plus → `ministral-14b-2512` (multimodal).
- Message d'erreur 429 explicite (limite de débit **ou** modèle hors offre).

## [2.2.0] — 2026-11-14

### Ajouté
- **Écoute interactive** : micro maintenu pendant le calcul Mistral et la lecture TTS (threads séparés)
- **Interruption vocale** : « Friday stop », « Friday attends », « Friday, j'ai une précision »,
  « Friday, explique autrement », « Friday, arrête l'écoute »
- Bouton **« Interrompre la réponse »** dans le cockpit et la notification Android
- **Clonage vocal** Mistral (Voxtral), activation enregistrée après capture
- Anti-écho : `VOICE_COMMUNICATION` + `AcousticEchoCanceler` / `NoiseSuppressor`
- Bibliothèque **Rust** `libfriday_core.so` (détection de mot-clé, JNI)

### Modifié
- Détection de fin de phrase accélérée (~2,1 s, réglable) et fenêtre de dialogue de 75 s
- Annulation des requêtes HTTP Mistral et des segments audio en cours

### Corrigé
- Une réponse annulée ne peut plus être prononcée après coup

## [2.0.2] — 2026-11-14

### Ajouté
- Publication du dépôt (sources Java/Python/Rust, scripts de build, tests)
- APK debug de référence

### Corrigé
- Analyse statique de l'APK documentée (`ANALYSE.md`)

## [2.0.0] — 2026-11-06

### Ajouté
- Cockpit HUD (WebView), mémoire personnelle chiffrée, profils de personnalité
- Mistral Chat Completions, recherche web, analyse de photos (Vision)
- Vosk hors-ligne, TTS Android, GPS et résolution de lieux
