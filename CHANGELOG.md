## [3.5.0] — Fiabilité Mistral et diagnostics

- Reprises HTTP bornées (429 et 500/502/503/504) avec Retry-After, backoff et arrêt lors des interruptions. Jamais de répétition des requêtes 4xx définitives.
- Liste des modèles Mistral mise en cache pendant une heure, sélection explicite, remise à zéro du test Web lors du changement de modèle.
- État Web plus détaillé : sources réelles, outil exécuté sans source ou service indisponible. Clés et coordonnées non journalisées.
- Identité du profil et autorisations GPS de la v3.4.1 préservées ; tests Java réseau supplémentaires.
- La recherche Web et les positions réelles exigent toujours des essais sur téléphone avec le compte Mistral ; APK CI ≠ preuve d'accès réel.

# Changelog

Format inspiré de [Keep a Changelog](https://keepachangelog.com/fr/1.1.0/),
versionnage [SemVer](https://semver.org/lang/fr/).

## [3.4.1] — 2026-11-15

### Correctifs principaux
- **Nom et personnalité réellement actifs** : FRIDAY se présente avec l'identité définie par son profil actif et distingue clairement son nom de celui du fournisseur IA Mistral. Les prompts systèmes de Mistral Chat et Conversations insistent sur le profil sélectionné.
- **GPS transmis à Mistral après consentement** : corrige le cas « Où suis-je ? », auparavant résolu localement même lorsque le partage avec Mistral était autorisé. Les coordonnées et leur précision sont maintenant incluses dans la requête Mistral pour les demandes de position et d'alentours autorisées.
- **Respect de la vie privée** : activer simplement le GPS ne l'autorise pas à transmettre la localisation à Mistral. Il faut autoriser explicitement le partage ponctuel. Aucune position n'est envoyée pour les questions non locales, ni stockée dans l'historique.
- **Cockpit transparent** : nouveau statut « GPS vers Mistral » et bouton d'autorisation, avec indication de l'heure du dernier envoi API réussi (sans conserver les coordonnées).
- **Android** : indique lorsqu'un redémarrage de l'écoute vocale est requis après l'autorisation GPS, pour activer le type de service de localisation en premier plan.
- **Tests de régression** : identité du profil, non-équivalence GPS local / partage Mistral, reconnaissance de la question « as-tu accès à mon GPS ? » et versionnage.

### À vérifier sur le téléphone
- Enregistrer le profil voulu, demander « Friday, comment tu t'appelles ? »
- Dans le cockpit, choisir « Autoriser le GPS à Mistral » puis autoriser la permission Android ; arrêter et redémarrer l'écoute si demandé.
- Poser « Friday, où suis-je ? » puis « Quel temps fait-il autour de moi ? ». Le second test demande le Web Mistral et peut être facturé.
- Le GPS doit fournir une position récente ; l'adresse exacte n'est pas garantie. Les résultats Mistral ne peuvent pas être certifiés sans essais avec la clé personnelle.

## [3.4.0] — 2026-11-15

### Correctifs et améliorations
- **Profil actif Mistral** : instructions de la personnalité réellement sélectionnée renforcées en fin de prompt système, pour les conversations classiques et la recherche Web ; affichage du nom du profil actif en télémétrie et vérification de l'enregistrement.
- **Télémétrie Web** : suppression de la valeur codée en dur « Désactivé ». Indication réelle : désactivé, clé manquante, activé non testé, activé testé avec URL source.
- **GPS Android** : meilleure détection des questions « Où suis-je ? », « Où est-ce que je me trouve ? », « Quels services autour de moi ? ». Diagnostic séparé des permissions, des services GPS, de l'absence de position et de la précision mesurée.
- **Consentement localisation** : choix « GPS local uniquement » ou partage ponctuel autorisé avec Mistral pour des questions sur les alentours ; aucun envoi permanent de coordonnées.
- **Interruption intelligente** : la commande « Friday, j'ai une précision » et ses variantes arrêtent la réponse en cours sans perdre le sujet ; le contexte de la demande interrompue est réutilisé dans la précision suivante.
- **Cockpit** : état GPS, recherche Web, profil actif ; suppression des requêtes GPS en double lorsque le bouton GPS est utilisé.
- **Tests** : cas de régression Java pour les formulations GPS et l'interruption ; contrôles de câblage de l'interface et de Mistral.

### Limites
- Recherche Web Mistral soumise aux permissions, au quota et à la facturation de la clé API.
- Le GPS précis dépend du matériel, de l'autorisation Android et de la couverture ; 3 mètres ne sont pas garantis.
- Les tests automatisés ne remplacent pas les vérifications audio/GPS/Mistral sur un appareil Android réel.

## [3.3.0] — 2026-11-15

### Fournisseur unique : Mistral AI
- **Suppression de DeepSeek** des appels réseau, de l'écran des réglages, du cockpit et du service vocal.
- **Mistral Chat Completions** pour les conversations ordinaires (modèle par défaut `mistral-small-latest`).
- **Mistral Conversations** et son outil intégré `web_search` pour les questions nécessitant des informations en ligne. Les références Web réellement retournées sont affichées ; en l'absence de références, FRIDAY ne prétend pas avoir consulté une source.
- **Compréhension contextuelle** des questions posées oralement et des relances courtes avec historique de dialogue.
- **Migration des préférences** : suppression locale de l'ancienne clé DeepSeek ; les clés Mistral antérieurement présentes sont préservées, mais l'ancienne version peut les avoir effacées.
- **Réglages Mistral** : clé API Mistral, détection des modèles autorisés par le compte et test explicite de la recherche Web.
- **Compatibilité Android** : même application et signature release, versionCode 54 pour mise à jour de la v3.2.2.

### Limites
- La nouvelle version nécessite une **clé API Mistral personnelle** ; la clé DeepSeek ne fonctionne pas avec Mistral.
- L'outil Web dépend des droits, du quota et de la facturation du compte Mistral. Si une recherche échoue, la réponse est signalée comme non vérifiée sur Internet.
- Tests de code et de compilation automatisés ; fonctionnement vocal et Web réel à valider sur un appareil Android.

## [3.2.2] — 2026-11-15

### Amélioré
- **Recherche Web intelligente** : les questions factuelles, les demandes de recherche explicites et les relances contextuelles déclenchent l'API de recherche DeepSeek, si elle est activée.
- **Lecture des liens** : un lien HTTP(S) fourni par l'utilisateur peut être lu grâce à l'API officielle DeepSeek Fetch (lecture ponctuelle).
- **Meilleure compréhension des relances** : « Et à Lyon ? » reprend le sujet de la question précédente pour la recherche, avec contexte conversationnel conservé pour le modèle.
- **Fiabilité des réponses** : lorsque la recherche échoue ou ne retourne aucune source, FRIDAY l'indique au lieu de prétendre avoir consulté le Web.
- **Réglages natifs** : bouton « Activer et tester la recherche Web », test réel de l'API, avertissement de facturation éventuelle.
- **Performances** : les modèles disponibles ne sont plus recherchés systématiquement à chaque question, mais avec un cache de 24 h.
- **Tests** : couverture Java des requêtes factuelles, recherches explicites, liens et relances, et validation du build Android en CI.

### Limites
- Les recherches et les lectures de pages requièrent une **clé DeepSeek valide**, du réseau et un compte autorisé à utiliser les API Search/Fetch. Les appels peuvent être facturés.
- FRIDAY ne contourne pas les sites privés, comptes protégés ou politiques d'accès.
- Les résultats de recherche ne garantissent pas à eux seuls l'exactitude d'une information.

## [3.2.1] — 2026-11-15

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
