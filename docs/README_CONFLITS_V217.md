# F.R.I.D.A.Y. v2.1.7 — Détection des conflits entre installations

Ce projet est la suite du projet Android **v2.1.6 Mistral seul**. Il corrige aussi l'import manquant `AlertDialog` qui bloquait une compilation de v2.1.6.

## Changements

1. Le cockpit indique l'identité et la version de l'application en cours et le nombre d'autres éditions connues installées.
2. Si une autre édition est installée, un dialogue apparaît au démarrage de l'écoute, avec annulation, gestion des versions ou poursuite volontaire.
3. Le bouton **Versions** ouvre la liste des éditions installées puis leurs pages **Informations sur l'application** dans Android. L'arrêt forcé éventuel est réalisé par l'utilisateur dans les réglages Android.
4. La version v2.1.7 observe la configuration de son enregistrement micro via `AudioRecord` sur Android 10+. En cas de capture rendue muette par le système, elle affiche « MICRO BLOQUÉ » ; elle cesse temporairement d'alimenter Vosk avec ce silence, puis reprend si Android restaure le micro.

## Ce que l'application ne peut pas garantir

Android ne fournit pas aux applications ordinaires un moyen fiable d'identifier en toutes circonstances l'application qui possède le micro ni d'arrêter une autre application à leur place. Le simple fait qu'une autre édition soit *installée* ne prouve pas qu'elle soit active. Le dialogue avertit de cette incertitude.

## Compilation Manjaro

```bash
cd ~/Téléchargements
unzip -o FRIDAY_v2_1_7_Android_Conflits_Micro_Manjaro.zip
cd FRIDAY_v2_1_7_ANDROID_CONFLITS_MICRO
bash scripts/tester_sources.sh
bash scripts/compiler_manjaro.sh
```

APK attendue après **BUILD SUCCESSFUL** : `dist/FRIDAY_v2.1.7_Android_Manjaro_Debug.apk`. Cette APK utilise l'identifiant debug `fr.friday.assistant.smartpreview`; elle pourra remplacer une v2.1.6 de même identifiant et signature, sinon Android refusera la mise à jour. Ne désinstallez pas une version fonctionnelle simplement pour contourner un problème de signature.

## Essais sur le Samsung

- Installer la v2.1.7 à côté de la v1.x ; ouvrir le cockpit et vérifier la version affichée.
- Avec la v1.x toujours installée, activer l'écoute v2.1.7 : le dialogue d'avertissement doit s'afficher.
- Appuyer sur Versions, sélectionner l'ancienne édition, puis vérifier que les paramètres Android de cette édition s'ouvrent.
- Arrêter l'ancienne édition manuellement et démarrer l'écoute dans la v2.1.7 ; vérifier une question vocale puis verrouiller l'écran.
- Tester un véritable chevauchement de micro seulement si possible, et vérifier l'alerte **MICRO BLOQUÉ** si Android réduit au silence le flux de v2.1.7. L'absence d'alerte ne démontre pas qu'aucune autre application utilise le micro.
- Vérifier Mistral, l'analyse visuelle, la localisation et la mémoire ; ne pas partager la clé API dans un journal de débogage.

## Tests effectués avant livraison

Tests unitaires Java sans Android, Python, Rust, syntaxe JavaScript et Bash, intégrité du ZIP. La compilation Android complète et les essais matériels restent à effectuer sur Manjaro et Samsung.
