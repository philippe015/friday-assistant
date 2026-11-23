# JARVIS Android — protection vocale des références RSS

## Rapport source
Rapport de test local du 23 novembre 2026, 05:53 : le modèle Vosk a entendu
« arctique numero de en entier » après un bulletin d'actualités. Le routeur
a ouvert une recherche Web sur l'Arctique. À la tentative suivante, la
transcription « article numéro de » a bien été traitée localement.

Le rapport original **n'est pas intégré au dépôt** (dialogues en clair).

## Correctif

- `RssArticleIntent` détecte cette variante très ciblée : « arctique numéro de en entier », et les variantes « article entier numéro de ».
- L'absence de numéro empêche toute lecture et **ne doit jamais être corrigée automatiquement vers « deux »**.
- Le circuit vocal demande un numéro explicite avant le réseau ; le circuit
  écrit applique désormais la même règle.
- Les vraies questions « Quel est le climat de l'Arctique ? » continuent normalement.
- Tests unitaires Java et compilation APK sur GitHub Actions.

## Version de test

Identifiant Android : `fr.friday.assistant.v31418voicefix`
Nom affiché : **JARVIS · Vocal Fix**
Version interne : 3.14.18 (Preview)

Cette variante s'installe à côté de `v31418logfix`, car les APK debug
GitHub peuvent être signés avec des clés différentes. Les anciens modèles
et conversations ne sont pas transférés automatiquement.

## Limites

Ce patch corrige le **routage** de certaines erreurs Vosk, pas la transcription
acoustique elle-même. Il n'est pas possible de reconstituer avec certitude le
numéro prononcé lorsque Vosk enregistre « de ». L'utilisateur doit dire par
exemple « article deux » après la demande de précision.
