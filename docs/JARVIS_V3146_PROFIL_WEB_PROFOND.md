# JARVIS Android v3.14.6 — profil permanent et analyse Web approfondie

## Objectif

Sur Android, JARVIS conserve le profil sélectionné dans Android Keystore et le relit
à chaque requête Gemma/LiteRT ou MediaPipe. L'identité parlée du moteur local
est fondée sur le profil actif plutôt que sur un nom codé en dur.

L'analyse complète est un **traitement du texte public effectivement récupéré**,
pas une promesse d'accès à des pages masquées ou de rendu d'un navigateur JavaScript.

## Utilisation

- « Jarvis, analyse cette page https://exemple.org/article » : JARVIS télécharge
  la page HTTPS publique, extrait le texte lisible (balise article/main si possible)
  puis analyse les segments avec le profil actif. La source réellement lue est affichée.
- « Jarvis, lis cette page https://exemple.org/article en entier » : fait lire le
  texte public extrait, avec avertissement s'il est incomplet.
- « Jarvis, analyse les pages Web sur les moteurs électriques » : analyse au plus
  deux pages accessibles parmi les résultats, puis compare les conclusions.
- Après un bulletin RSS : « Jarvis, analyse l'article 1 avec mon profil » ou
  « Jarvis, lis l'article 1 en entier ». Les liens Google News non résolus et
  les pages qui refusent l'accès sont signalés, non contournés.

## Détails techniques

- Le lecteur ordinaire conserve ses limites précédentes : 450 Ko HTML, 12 000 caractères.
- La lecture approfondie accepte au plus 1,5 Mo HTML, 36 000 caractères de texte.
- Découpage à 1 700 caractères, 24 segments maximum, regroupement des analyses
  par cinq segments et synthèse finale sur tous les groupes.
- Le modèle local peut être sollicité de nombreuses fois sur une page longue :
  prévoir une utilisation CPU/GPU plus importante ; le bouton d'interruption reste
  fonctionnel entre les étapes d'analyse.
- Les profils sont toujours récupérés localement avant une génération Gemma. Le
  nombre de caractères de profil appliqués par requête reste borné à 3 200 pour
  protéger le contexte mémoire du modèle, même si davantage peut être enregistré.
- Aucune clé API, mémoire privée, localisation GPS ou profil n'est ajoutée à la
  recherche publique HTTPS. Les extraits Web non fiables ne sont jamais traités
  comme des instructions système.
- HTTPS public seulement ; maximum trois redirections, refus des adresses locales
  et privées. HTTP 403/429, contenus payants, articles JavaScript-only et sites
  nécessitant un compte restent inaccessibles. Les pages avec trop de texte
  portent un avertissement explicite de lecture partielle.

## Non-régressions

Le patch ne change pas les classes de microphone/Vosk, l'écoute continue, le TTS,
le fonctionnement du bouton STOP, la mémoire locale chiffrée, ni le partage GPS.
Le suffixe de préversion reste `.readerfixpreview` pour la continuité avec
v3.14.5 lorsque les signatures Android correspondent.

Tests automatiques : découpage et limites de pages longues, commandes explicites,
identité du profil actif, confinement de l'inférence et des recherches.
**Les tests CI ne remplacent pas une validation vocale sur un téléphone.**

## Parcours de validation Android

1. Garder l'ancienne APK fonctionnelle et sauvegarder les données importantes.
2. Installer l'APK v3.14.6 de prévisualisation compilée sur la PR GitHub.
   Si Android refuse la mise à jour, ne pas désinstaller l'APK précédente
   uniquement pour contourner une incompatibilité de signature.
3. Vérifier le profil enregistré puis demander « Jarvis, qui es-tu ? ».
4. Tester la lecture et l'analyse d'une URL HTTPS publique en français.
5. Tester une page longue : vérifier que la réponse inclut les passages de la fin.
6. Vérifier les liens réels et les avertissements de texte partiel.
7. Essayer une page bloquée et vérifier qu'aucun contenu n'est inventé.
8. Relancer l'application et vérifier que le profil choisi reste actif.
