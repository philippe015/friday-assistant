# F.R.I.D.A.Y. v2.2.0 — Voix clonée et interruption interactive

**Nouveau :** le microphone reste actif pendant les réponses de Mistral/Voxtral. Dites « Friday stop » pour interrompre la voix sans arrêter le service, ou « Friday, j’ai une précision » pour compléter la conversation. Le cockpit et la notification Android ont un bouton de coupure de réponse.

**Détails et procédures :** voir `README_ECOUTE_INTERRUPTION_V220.md`. Les consignes précédentes sont conservées ci-dessous.

---

**ACTUALITÉ v2.1.9 :** activation du clonage vocal enregistrée automatiquement après la capture ; consulter [README_VOIX_CLONE_V219.md](README_VOIX_CLONE_V219.md).

**v2.1.9 :** clonage vocal Mistral avec activation automatique après capture ; voir [README_VOIX_CLONE_V219.md](README_VOIX_CLONE_V219.md).

# F.R.I.D.A.Y. 2.1.9 — Android Mistral uniquement

Cette édition ajoute une mémoire locale chiffrée sur demande et un mode d'analyse approfondie. **Lisez README_INTELLIGENCE_V216.md pour les changements et la compilation.**

# F.R.I.D.A.Y. v2.1.9 — Android · Mistral uniquement

Projet Android complet à compiler **sur Manjaro** pour installation sur un téléphone Android ARM64. À partir de la v2.1.4, cette variante retire **Agnès** et **le mode Duo** : **une seule clé d'IA, Mistral**. Aucun appel vers l'API d'Agnès. Une ancienne clé Agnès chiffrée est effacée à l'ouverture de cette version.

## Fonctions

- Conversation texte/vocale native via Mistral Chat Completions (`/v1/chat/completions`).
- Recherche Web à travers l'API Conversations Mistral (`/v1/conversations`, outil `web_search`), selon l'abonnement et le modèle.
- Analyse ponctuelle de photos avec **Mistral Vision** après prise de photo volontaire.
- Reconnaissance française Vosk hors ligne, synthèse vocale Android, service microphone au premier plan avec notification d'arrêt ; l'écoute peut se poursuivre écran verrouillé, sous réserve des restrictions Android.
- GPS Android, estimation de précision et recherche des alentours en interrogeant Mistral ; partage de position **facultatif** et ponctuel.
- Cockpit holographique, mode immersif, historique vocal local ; profils de personnalité personnalisables (10 profils, 16 000 caractères) chiffrés dans Android Keystore.
- Python (logique locale), Rust (détection de mot-clé), Java Android (service, GPS, TTS, API), cockpit HTML local dans un WebView. **Aucun serveur Termux nécessaire une fois installée.**

**Retiré :** la clé Agnès, ses modèles, le Duo et la génération d'images Agnès. La **création de nouvelles images** n'est pas incluse : pour l'ajouter avec Mistral seul, il faut intégrer son outil dédié `image_generation` et récupérer les images via ses fichiers, pas réutiliser l'ancienne route Agnès. L'analyse des photos reste présente.

## Compiler sur Manjaro

Dépendances attendues : JDK 17 ou 21 (et non 11), Python 3.14, Rust/rustup/cargo, SDK Android 35, build-tools 35.0.0 et Android NDK 27.2 ou 28.2. Gradle 8.9 et `cargo-ndk` sont installés par le script au besoin.

```bash
cd ~/Téléchargements
unzip -o FRIDAY_v2_1_9_Android_Clonage_Auto_Manjaro.zip
cd FRIDAY_v2_1_9_ANDROID_VOIX_CLONE_AUTO
bash scripts/tester_sources.sh
bash scripts/compiler_manjaro.sh
```

APK attendu après `BUILD SUCCESSFUL` :

`dist/FRIDAY_v2.1.9_Android_Manjaro_Debug.apk`

Avec le téléphone connecté et le débogage USB autorisé :

```bash
adb install -r dist/FRIDAY_v2.1.9_Android_Manjaro_Debug.apk
```

Cette variante DEBUG conserve le suffixe d'identifiant `.preview`. Si la clé de signature de débogage diffère d'une version précédente, Android refusera l'installation par-dessus : **ne désinstallez pas votre version fonctionnelle avant d'avoir sauvegardé vos réglages**. Les clés API sont chiffrées dans l'application et ne sont pas exportées avec les profils.

## Première configuration Android

1. Ouvrir **Réglages** et renseigner la seule clé API **Mistral**, le modèle texte et le modèle Vision.
2. Choisir Web étendu, automatique ou désactivé ; certains appels Web peuvent être facturés ou limités selon le compte.
3. Télécharger une fois le modèle de reconnaissance vocal français (Vosk) ; autoriser microphone et notifications, puis activer l'écoute lorsque l'application est affichée.
4. Vérifier **Tester la voix** ; utiliser une question texte après démarrage du service pour distinguer micro/API/TTS.
5. Activer la position Android et son partage ponctuel séparément, uniquement si souhaité.
6. Configurer **Personnalité** pour personnaliser le comportement de Mistral, y compris écran verrouillé.

Le GPS ne garantit pas 3 m de précision. Les lieux fournis par la recherche Web ne sont pas une base cartographique exhaustive. Le géocodeur facultatif d'Android peut consulter un service réseau du système, mais aucune clé ni API Google Places n'est utilisée dans le code.

## Validation

Contrôles statiques et tests Python/Java/JavaScript fournis dans `scripts/tester_sources.sh`. La compilation APK complète, les appels API, la caméra, le microphone avec écran verrouillé et l'installation sur Samsung **restent à valider sur le téléphone**.

### Sources techniques

- Mistral Web Search : https://docs.mistral.ai/studio/agents/agent-tools/websearch
- Mistral Vision : https://docs.mistral.ai/capabilities/vision
- Mistral Image Generation (non intégré) : https://docs.mistral.ai/studio/agents/agent-tools/image_generation


## v2.1.9 — Prévention des conflits de microphone

- Numéro de version et identifiant du paquet affichés dans le cockpit et dans le panneau Android.
- Détection **des éditions connues installées** (`fr.friday.assistant`, `.preview`, `.smartpreview`) avec liste et accès direct aux réglages Android pour arrêt manuel.
- Avertissement avant de démarrer le micro si une autre édition connue est installée, avec possibilité d'annuler ou de poursuivre.
- Sur Android 10+, surveillance de `AudioRecord.getActiveRecordingConfiguration().isClientSilenced()` et notification dans le cockpit si Android rend la capture muette (concurrence micro / restriction système).
- Reprise de la reconnaissance lorsque Android rend le micro accessible à nouveau.
- Aucun appel réseau supplémentaire. Mistral, GPS, vision, Vosk, mémoire et voix sont conservés.

**Limite :** Android n'autorise pas une application normale à savoir de manière fiable si *toutes* les autres versions de F.R.I.D.A.Y. écoutent réellement. Le panneau indique donc « installée », jamais « active » pour les autres applications, et n'arrête aucune version sans intervention de l'utilisateur. La surveillance de `isClientSilenced` détecte un symptôme réel de concurrence audio, mais ne permet pas d'identifier l'autre application responsable.

**Autre limite :** F.R.I.D.A.Y. peut continuer lorsque l'écran s'éteint si son service reste actif, mais pas si le téléphone est éteint ou si Android tue le service.
