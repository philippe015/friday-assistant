> Historique v2.1.8. Pour la version actuelle voir README_VOIX_CLONE_V219.md.

# F.R.I.D.A.Y. 2.1.8 — Clonage de voix Mistral (Voxtral TTS)

Cette version part du projet Android **2.1.7** et conserve Mistral comme **seule API externe d'IA**. Clonage vocal via l'API officielle `POST https://api.mistral.ai/v1/audio/speech`, modèle `voxtral-mini-tts-3-0`, champ `ref_audio` et réponse WAV `audio_data` en base64. Documentation Mistral : https://docs.mistral.ai/studio/audio/text-to-speech/voice-cloning

## Installation / Manjaro

```bash
cd ~/Téléchargements
unzip -o FRIDAY_v2_1_8_Android_Voix_Clonee_Mistral_Manjaro.zip
cd FRIDAY_v2_1_8_ANDROID_VOIX_CLONEE
bash scripts/tester_sources.sh
bash scripts/compiler_manjaro.sh
adb install -r dist/FRIDAY_v2.1.8_Android_Manjaro_Debug.apk
```

Le script Gradle et Rust nécessite Java 17/21, Android SDK 35, NDK 27/28, Python 3.14. La version de développement conserve le package `.smartpreview` : la mise à jour nécessite la **même signature** que l'application précédente. Ne désinstallez pas une application fonctionnelle sans sauvegarde.

## Utiliser sa voix

1. Ouvrir **Réglages**. La clé API Mistral doit déjà être enregistrée.
2. Sous **Voix clonée**, cocher la confirmation « ma voix ou voix autorisée ».
3. Arrêter l'écoute native de F.R.I.D.A.Y. et les autres éditions qui utilisent le microphone.
4. Appuyer sur **Enregistrer mon échantillon vocal (12 s)**. Parler distinctement et naturellement pendant toute l'acquisition, dans une pièce calme. Le fichier WAV mono 16 kHz est conservé dans l'espace **privé** de l'application Android.
5. Activer **Utiliser la voix clonée pour les réponses**, puis **Enregistrer les réglages**.
6. Appuyer sur **Tester ma voix clonée** (l'écoute doit être arrêtée). Après le test, redémarrer le service vocal.
7. Demander « Friday, bonjour » : les réponses du service passeront par la synthèse Voxtral si l'API est disponible.

Les réponses longues sont découpées en segments. La reconnaissance vocale Vosk demeure **locale**. Le mode cloné requiert le réseau et peut ajouter un délai et des **frais d'API**. Si l'API vocale refuse le modèle, le quota ou la clé, les réponses vocales basculent vers la voix Android (avec message d'erreur visible).

**Confidentialité :** l'échantillon n'est pas envoyé lors de l'enregistrement. Quand la voix clonée est activée, **chaque génération vocale transmet l'échantillon de référence et le texte à Mistral**. Aucun appel n'est fait à Google, Agnès, ElevenLabs ou à un moteur distant supplémentaire. La version utilise le mode *zero-shot* `ref_audio` et **ne crée pas de voix enregistrée** de manière permanente sur Mistral. Un bouton permet de supprimer le fichier local. Vérifier les conditions de conservation de données de son compte Mistral.

**Consentement :** enregistrer uniquement sa propre voix ou une voix avec l'accord explicite de son titulaire. Pas d'import de voix tierce ni de clonage à l'insu d'une personne.

## Ce qui est et n'est pas testé

Tests de sources Python / Java indépendant d'Android / Rust / JavaScript : `bash scripts/tester_sources.sh`. Il faut **compiler sur Manjaro** et tester **sur Samsung**, y compris permissions microphone, disponibilité réelle de l'API vocale sur la clé Mistral, volume audio et comportement avec écran verrouillé. Impossible de garantir la qualité ni la ressemblance de la voix à distance.
