# Changelog

Format inspiré de [Keep a Changelog](https://keepachangelog.com/fr/1.1.0/),
versionnage [SemVer](https://semver.org/lang/fr/).

## [3.0.2] — 2026-11-15

### Corrigé
- **Modèle DeepSeek invalide** : `deepseek-chat` n'existe plus dans l'API 2026
  (modèles actuels : `deepseek-flash`, `deepseek-v4-pro`). Le modèle par défaut devient
  **`deepseek-flash`**.
- **Repli automatique** : si le modèle configuré est refusé (400/404/422), la question est
  rejouée avec `deepseek-flash` puis `deepseek-v4-pro`, et le réglage est corrigé.
- **URL de base** : `https://api.deepseek.com` (chemin documenté) au lieu de `…/v1`.

## [3.0.1] — 2026-11-15

### Ajouté
- **Diagnostic d'erreur** dans les réglages natifs : la dernière erreur du moteur IA est
  mémorisée, affichée avec son horodatage, et copiable dans le presse-papiers
  (bouton « ⧉ Copier la dernière erreur ») — avec modèle, URL et présence de la clé.
- Nettoyage : fonction `vision()` résiduelle retirée du cockpit.

## [3.0.0] — 2026-11-15

### Supprimé
- **Tout le code Mistral** : `MistralVoice` (clonage vocal Voxtral), `WebIntent` / `WebPolicy`
  (recherche Web via l'API Conversations), `NearbyIntent` (analyse des alentours),
  `VoiceClonePolicy` / `VoiceCloneActivation` / `VoiceSampleRecorder`, analyse d'images (Vision).
- Réglages associés : clé Mistral, modèle Mistral, modèle vision, mode Web, raisonnement approfondi,
  voix clonée. Le bouton « Choisir Mistral Medium » disparaît.
- Tests obsolètes liés à Mistral.

### Modifié
- **DeepSeek est le seul moteur** (endpoint `https://api.deepseek.com/v1`, modèle `deepseek-chat`),
  avec le bouton « Se connecter / créer ma clé sur platform.deepseek.com ».
- Le service annonce « Réflexion DeepSeek… » ; le cockpit et les libellés ne mentionnent plus Mistral.
- `debug` conserve le suffixe `.smartpreview` (cohabitation avec les anciennes versions).

### Notes
- L'application est plus légère et n'envoie plus rien à Mistral.
- La reconnaissance vocale (Vosk), la synthèse Android (TTS), la mémoire chiffrée, les profils de
  personnalité et la localisation restent disponibles.

## [2.4.0] — 2026-11-15

### Modifié
- **DeepSeek devient le fournisseur unique** (Mistral retiré de l'interface).
- Nouveau bouton **« Se connecter / créer ma clé sur platform.deepseek.com »** : ouvre la page
  web officielle dans le navigateur, où l'utilisateur crée/copie sa clé API.
- Le cockpit affiche désormais **MOTEUR DEEPSEEK**.
- La clé reste chiffrée localement (Android Keystore).

### Notes
- La **vision** et la **recherche Web** ne sont plus exposées : DeepSeek Chat ne propose pas
  de connecteurs équivalents (les fonctions Mistral restent dans le code mais inactives).

## [2.3.0] — 2026-11-15

### Ajouté
- **Fournisseur IA configurable** dans les réglages natifs :
  - `Mistral AI` (comportement historique)
  - **`Compatible OpenAI`** : URL de base + modèle + clé — fonctionne avec
    **DeepSeek**, **Ollama**, **LM Studio**, **vLLM**, etc.
- La clé du fournisseur OpenAI est chiffrée comme celle de Mistral (`SecurePrefs`).

### Notes
- La **vision** et la **recherche Web** restent liées à Mistral (connecteurs non pris en charge
  par les autres fournisseurs). Les questions texte utilisent le fournisseur choisi.

## [2.2.4] — 2026-11-15

### Corrigé
- **Chaque question affichait « ERREUR »** : le mode Web utilisait l'API *Conversations* avec un modèle
  `ministral-*`, qui ne prend pas en charge les connecteurs :
  `HTTP 400 — Model ministral-8b-latest currently does not support builtin connectors`.
- Les modèles `ministral-*` **n'utilisent plus** l'API Conversations : réponse directe via
  `/v1/chat/completions`, avec une note si la question nécessitait Internet.
- Le repli du mode Web couvre désormais **429 / 500 / 501** en plus de 400 / 404 / 422.

## [2.2.3] — 2026-11-15

### Corrigé
- **« MOTEUR MISTRAL ERREUR »** alors que la voix et les réponses locales fonctionnent :
  un modèle enregistré (`mistral-small-*` / `mistral-medium-*`) renvoie `429` sur les offres
  actuelles. Les modèles stockés sont désormais **migrés automatiquement** au démarrage.
- **Repli automatique** : si le modèle configuré est refusé (400/404/422/429), la question est
  rejouée une fois avec `ministral-8b-latest`, le réglage est mis à jour et une note est ajoutée
  à la réponse.
- Les erreurs Mistral précisent désormais le **modèle utilisé**.

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
