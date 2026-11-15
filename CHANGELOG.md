# Changelog

Format inspiré de [Keep a Changelog](https://keepachangelog.com/fr/1.1.0/),
versionnage [SemVer](https://semver.org/lang/fr/).

## [Non publié] — correctifs après v3.2.0

### Corrigé
- **Réponses locales trompeuses** : la logique Python prioritaire appliquait encore
  d'anciens tests par sous-chaîne pour l'heure/la date, annulant le correctif Java de v3.2.0.
  Les demandes contenant un lieu ou un autre sujet sont maintenant transmises à DeepSeek.
- **Confiance Vosk inefficace** : activation de `setWords(true)` sur tous les décodeurs
  et prise en compte des mots de chaque segment reconnu, pas seulement du segment final.
- **Cycle de vie audio** : libération du contrôle automatique de gain (AGC) à la fermeture
  du microphone.
- **Tests de non-régression** : scénarios français couvrant météo, villes, transports,
  dates et heure.

## [3.2.0] — 2026-11-15

### Corrigé
- **Questions mal comprises** : les réponses locales (« il est X heures », « nous sommes le … »)
  interceptaient des phrases contenant simplement le mot « heure ». Elles exigent désormais que
  **tous** les mots de la phrase appartiennent au champ « heure » ou « date », et que la question
  fasse **8 mots maximum**. « Quelle est la météo à Vitry ? » part donc bien à l'IA.
- **Transcription douteuse** : la confiance moyenne des mots Vosk est vérifiée. En dessous de
  **55 %** sur une phrase de 3 mots ou plus, FRIDAY affiche ce qu'il a entendu et demande de
  répéter, au lieu d'envoyer une question erronée au modèle.

## [3.1.1] — 2026-11-15

### Corrigé
- **La détection des modèles n'appliquait pas le résultat** : la liste était affichée/copiée, mais
  le champ « Modèle DeepSeek » conservait l'ancienne valeur (souvent obsolète).
- Désormais, si le modèle configuré n'est pas proposé par le compte, l'app **applique
  automatiquement** un modèle valide détecté (priorité à ceux contenant « flash »), met à jour
  le champ et le réglage, et affiche une confirmation.

## [3.1.0] — 2026-11-15

### Ajouté
- **Accès Web** via l'API DeepSeek : `POST /v1/tools/search` (recherche native DeepSeek).
  Les meilleurs résultats (titre, URL, résumé) sont injectés dans la requête, avec consigne
  de **citer les sources** ; les pages Web sont traitées comme des données non fiables.
- Heuristique `WebSearch.needsWeb()` : la recherche ne se déclenche que si la question le
  justifie (actualité, prix, météo, dates récentes, URL…), pour éviter les appels facturés.
- Réglage **« Recherche Web (API DeepSeek) »** activé par défaut.

### Amélioré
- **Qualité du microphone** : ajout de l'**AGC** (`AutomaticGainControl`) en plus de
  l'annulation d'écho et du réducteur de bruit, et **tampon audio ×4** (16384 octets minimum)
  pour réduire les coupures et mieux capter les voix faibles.

## [3.0.5] — 2026-11-15

### Corrigé
- **La clé DeepSeek saisie n'était pas prise en compte** tant que l'on n'appuyait pas sur
  « Enregistrer les réglages » (situé tout en bas du panneau, hors écran).
- Nouveau bouton **« 💾 Enregistrer la clé DeepSeek »** juste sous le champ, avec retour immédiat
  (`✔ enregistrée` / `✘ échec`).
- Le bouton **« 🔍 Détecter les modèles »** enregistre d'abord la clé présente dans le champ,
  puis interroge `/models` — plus besoin d'aller plus bas dans les réglages.

## [3.0.4] — 2026-11-15

### Ajouté
- **Détection automatique des modèles** : l'app interroge `GET /models` avec votre clé et
  utilise un modèle réellement proposé par votre compte (plus de nom obsolète).
- Bouton **« 🔍 Détecter les modèles disponibles »** dans les réglages : affiche la liste
  et la copie dans le presse-papiers.
- La liste détectée est mise en cache (`available_models`) et réutilisée comme ordre de repli.

## [3.0.3] — 2026-11-15

### Corrigé
- **Le champ « Modèle DeepSeek » conservait l'ancien nom** (`deepseek-chat`) enregistré dans les
  préférences, alors que la valeur par défaut était corrigée : l'écran affichait donc toujours
  l'ancien modèle.
- **Migration au démarrage** : `deepseek-chat`, `deepseek-reasoner` et `deepseek-v3*` sont
  remplacés par **`deepseek-flash`**.
- Le nom retiré n'est plus envoyé à l'API avant le repli.

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
