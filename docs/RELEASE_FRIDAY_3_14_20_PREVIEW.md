# FRIDAY · Intelligence+ 3.14.20 — Android Preview

**Statut : version de test (prérelease), non publiée sur Google Play.**

Cette édition Android reprend les évolutions de la [PR #114](https://github.com/philippe015/friday-assistant/pull/114) :
- Météo horaire du soir (18–23 h, fuseau horaire de la ville) et « demain soir » ;
- Nom et personnalité FRIDAY mieux préservés quand le modèle local veut se présenter comme Gemma ou Mistral ;
- Réponse vocale directe de 50 secondes après une demande explicite de ville météo ;
- Correctif antérieur du contexte météo Vitry-sur-Seine et du diagnostic `VOSK_NO_WAKE`.

**Installation sans remplacer les anciennes éditions :** package
`fr.friday.assistant.v31420preview`, application affichée
`FRIDAY · 3.14.20 Preview`. Ses réglages, permissions, modèles locaux
et conversations sont indépendants des autres APK FRIDAY installés.

## Utilisation

1. Télécharger le **fichier .apk** dans les pièces jointes de la Release
   GitHub, pas le ZIP des sources.
2. L'ouvrir dans les téléchargements Android et autoriser l'installation
   depuis cette source si Android le demande.
3. Autoriser le micro, puis installer/configurer les modèles de
   reconnaissance Vosk et IA locale compatibles.
4. Dire « Friday, donne-moi la météo de ce soir », puis répondre
   « Vitry-sur-Seine ce soir » lorsque FRIDAY demande la ville.
5. Vérifier que la réponse utilise des horaires et que FRIDAY reste prêt
   à répondre après la synthèse vocale.

## Limites

- Binaire Android ARM64, minimum SDK 26.
- Moteur IA complet **non inclus** : il faut importer un modèle pris en
  charge pour les réponses IA locales.
- Les conflits matériels du microphone restent à tester sur téléphone.
- La recherche météo demande une connexion Internet et peut échouer si
  le service Open-Meteo est inaccessible.
- La signature de débogage GitHub varie entre environnements : ne pas
  chercher à remplacer un ancien APK au même package signé différemment.
- Pour de nouvelles régressions, exporter volontairement les diagnostics
  et les partager **en privé** ; ne jamais publier de conversation brute
  sur un dépôt GitHub public.

## Vérification de la Release

L'automatisation doit publier le fichier APK et son SHA-256 après
compilation, tests, vérification de la signature Android et contrôle de
l'identifiant du package. La publication reste marquée **Pre-release** ;
la piste Google Play et l'application `jarvis.myapp` ne sont pas modifiées.
