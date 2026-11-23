# FRIDAY · Terminal Matrix (onglet de conversation)

**Évolution en développement** de FRIDAY Intelligence+ pour Android.
La version APK actuelle 3.14.20 n'est pas modifiée automatiquement.
Aucun accès au shell, à Termux ni aux commandes Android n'est donné.

## Interface

Un quatrième onglet **▦ Terminal Matrix** apparaît dans la barre de navigation
du cockpit, à côté de **Cockpit**, **Dialogue** et **Personnalité**. Sur écran
Android étroit, les quatre onglets s'organisent sur **deux lignes** pour
éviter les textes coupés.

Fond noir / vert Matrix, lignes de balayage discrètes, texte vert
monospacé et défilement. Les échanges affichés sont ceux de l'historique
de conversation Android : pas de base de données parallèle non chiffrée.
Les textes des questions et réponses sont insérés avec `textContent`,
jamais interprétés comme du HTML ni des commandes JavaScript.

## Poser des questions

1. Ouvrir **▦ Terminal Matrix**.
2. Saisir une question dans `vous@friday:~$`.
3. Appuyer sur **ENVOYER** ou sur **Entrée** (Maj+Entrée : nouvelle ligne).
4. FRIDAY traite la question via le même moteur local et les mêmes outils
   Android que le cockpit, puis affiche la réponse écrite.
5. La réponse est également prononcée par la **synthèse vocale Android**
   lorsque la sortie vocale est activée dans les réglages.
6. **▸ Lire la réponse** permet de faire relire la dernière réponse
   avec la voix Android ; **■ Stop voix** interrompt cette lecture.
7. **◉ Écoute vocale** démarre ou relance la reconnaissance native Vosk ;
   les paroles reconnues alimentent l'historique commun à l'application.

Le terminal n'exécute aucune commande Linux, Android, JavaScript ou shell :
une commande saisie est traitée exclusivement comme **une question** par
FRIDAY. Les réponses Internet suivent les paramètres Web et consentements
existants, sans envoyer automatiquement la mémoire privée aux services
de recherche.

## Comportement et confidentialité

- Le journal des messages affichés se reconstruit depuis l'historique
  de conversation chiffré ; le terminal n'utilise pas `localStorage`.
- Les réponses apparaissent en tant que texte brut, même si elles
  contiennent des caractères HTML, pour éviter l'injection.
- La voix dépend du réglage `voice_enabled` et du moteur TTS installé ;
  le bouton **Lire la réponse** permet une lecture à la demande.
- Les questions sont limitées à 5 000 caractères.
- Un appel en cours empêche d'envoyer une deuxième question.
- Le microphone reste soumis aux permissions Android et à la logique
  d'anti-écho déjà présente.
- Les réponses provenant du micro peuvent apparaître dans le terminal
  grâce à l'historique commun, sans nouvelle capture audio.
- **La mémoire/les modèles des différentes éditions Android** ne sont
  pas synchronisés automatiquement.

## Points à valider sur téléphone

- Saisie AZERTY et clavier virtuel à l'ouverture de l'onglet.
- Lisibilité du terminal sur écran 360 px, y compris en mode sombre.
- Sortie de la voix française, interruption et reprise du micro.
- Reprise après une réponse du modèle local et après une question Web.
- Aucun déclenchement du micro pendant une lecture TTS.
- Comportement si aucune IA locale compatible n'est importée.

Fichiers techniques :
`app/src/main/assets/index.html` et
`app/src/main/java/fr/friday/assistant/MainActivity.java`.
Tests : `tests/test_friday_matrix_terminal_20261123.py`.

Code source : [dépôt FRIDAY/JARVIS](https://github.com/philippe015/friday-assistant).
