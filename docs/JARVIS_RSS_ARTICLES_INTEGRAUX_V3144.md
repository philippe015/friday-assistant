# FRIDAY/JARVIS v3.14.4 — lecture du contenu des articles RSS

## Objectif
Après une recherche d'actualités RSS, dire **« Jarvis, lis l'article 1 en entier »**,
ou choisir le bouton **« Lis l’article RSS 1 »**. Le lien conservé provient du dernier
bulletin d'actualités affiché, est stocké chiffré (Android Keystore), et n'est
jamais inventé par Gemma.

## Comportement
- Le téléphone lit la page HTTPS publique derrière le lien RSS (pas le seul
  titre ou résumé). Extraction des balises article / main / contenu d'article
  avant recours au texte de la page visible, sans exécution JavaScript.
- Texte nettoyé limité à 12 000 caractères et à 450 Ko de HTML; si tronqué,
  une notice explicite est affichée et prononcée. La synthèse vocale Android
  lit le texte en segments et **STOP** doit interrompre la lecture.
- Pour les contenus non français, traduction **sur le téléphone** avec
  Gemma 4 E2B GPU importé, par sections, sans cloud ni données personnelles;
  si la traduction échoue, l'original est clairement identifié.
- Pages payantes, HTTP 403/429 ou liens Google News non résolus :
  JARVIS indique la limitation, ne contourne aucun accès.
- Les sources, dates RSS et URL d'origine restent disponibles dans le dialogue.
- Sans bulletin précédent, la commande explique de lancer d'abord une recherche
  d'actualités. « Article 2 » désigne le deuxième article du dernier bulletin.
- Le profil actif s'affiche dans le cockpit; un bouton de vérification lit
  à nouveau le profil chiffré après redémarrage.

## Installation
Version de test isolée (.readerpreview), préserve v3.14.3 et son modèle
importé. **Réimporter Gemma et les modèles vocaux dans la préversion**. Pour
transférer la personnalité personnalisée : Exporter JSON depuis v3.14.3,
Importer JSON dans v3.14.4. Ne pas désinstaller la version validée.

## Limites
Une page Web disponible n'est pas nécessairement complète (paywall, scripts,
images, vidéos, pagination). L'outil ne garantit jamais le texte intégral du
site. La date RSS n'atteste pas l'heure exacte de l'événement. Aucun test sur
téléphone réel n'est considéré réussi sans retour de l'utilisateur.
