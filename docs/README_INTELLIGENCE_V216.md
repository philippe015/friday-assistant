# F.R.I.D.A.Y. 2.1.6 — Mistral intelligent (Android)

Cette édition part de **F.R.I.D.A.Y. 2.1.5 Mistral seul** et conserve son service Vosk/TTS écran verrouillé, la caméra, la recherche Web Mistral, le GPS et les profils.

## Changements

- **Mémoire locale chiffrée** (Android Keystore), désactivée par défaut. Jusqu'à 12 notes de 300 caractères, saisissables dans Réglages ou par commande « Friday, retiens que ... ». Effacement possible. Aucune mémorisation automatique. Les notes ne sont envoyées à Mistral qu'après activation de la mémoire et pendant une question normale.
- **Mode analyse approfondie** optionnel : consignes de vérification, température abaissée et limite de réponse augmentée, sans appels API supplémentaires systématiques. Peut augmenter latence et coût de sortie.
- **Sélection rapide de Mistral Medium** pour choisir un modèle plus performant lorsque disponible sur votre compte. Enregistrer après avoir sélectionné. Tarification et accès selon votre compte.
- **Prudence factuelle** : vérification des sources, distinction entre faits et incertitudes, demandes de clarification quand nécessaire.
- **Vie privée** : commandes mémoire exclues de l'historique persistant des dialogues vocaux, mémoire désactivée par défaut. Sauvegarde Keystore des notes. Pas de mémoire des coordonnées GPS.

## Compiler sur Manjaro

```bash
cd ~/Téléchargements
unzip -o FRIDAY_v2_1_6_Android_Mistral_Intelligent_Manjaro.zip
cd FRIDAY_v2_1_6_ANDROID_MISTRAL_MEMOIRE
bash scripts/tester_sources.sh
bash scripts/compiler_manjaro.sh
```

Si la compilation réussit : `dist/FRIDAY_v2.1.6_Android_Manjaro_Debug.apk`.

**Attention** : arrêtez le service vocal de votre ancienne application avant de démarrer F.R.I.D.A.Y. Smart pour éviter deux accès simultanés au microphone. L'application debug utilise un identifiant `.smartpreview` et sa signature dépend de votre environnement. Ne supprimez pas votre ancienne application tant que la nouvelle n'est pas testée.

## Essais utiles sur le téléphone

1. Testez le micro, la parole, Mistral et la vision tels qu'en v2.1.5.
2. Dans les réglages, cochez Mémoire, notez une information non sensible et enregistrez les notes et les réglages.
3. Dites « Friday, retiens que je préfère des réponses concises » ; posez une autre question pour vérifier l'adaptation.
4. Dites « Friday, qu'as-tu mémorisé ? » puis effacez les notes et vérifiez.
5. Cochez Analyse approfondie, enregistrez et comparez les réponses sur une question difficile.
6. Vérifiez l'écoute écran verrouillé et la notification d'arrêt.

## Limites

Le modèle Mistral ne devient pas intrinsèquement plus intelligent. Le contexte, la mémoire et les consignes peuvent améliorer sa pertinence, mais pas garantir l'absence d'erreurs. Les outils Web demandent un accès et quota compatibles. APK et fonctionnement matériel à tester sur le Samsung.
