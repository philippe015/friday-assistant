# JARVIS Android — reconnaissance française sherpa-onnx (version bêta)

## Fonction réellement proposée
La dictée peut utiliser le moteur local **sherpa-onnx**, sur le téléphone,
avec le modèle Zipformer **français** officiel (Common Voice FR). Le moteur
vocal historique **Vosk** reste le choix par défaut et le décodeur STOP
reste fondé sur Vosk, y compris pendant la synthèse vocale.

Un **seul AudioRecord** ouvre le micro interne du téléphone et transmet ses
trames 16 kHz à un seul moteur de dictée générale. Il n'y a aucun appel
LLM distant, ni second microphone, ni accès Bluetooth ajouté. Le moteur
d'IA local et le moteur de dictée sont deux composants indépendants.

**Limites importantes :** le modèle de 2023 doit être validé sur chaque
téléphone Android, la dépendance JitPack est externe, la première
initialisation ONNX peut être plus lente que Vosk et consommer plus de RAM.
La compilation CI ne démontre pas une bonne reconnaissance de la voix.

## Installer depuis le téléphone sans ordinateur

1. Mettre à jour avec un APK construit et signé **avec la même signature**
   que la version stable, sans désinstaller JARVIS.
2. Ouvrir **Réglages > Reconnaissance française avancée**.
3. Appuyer sur **Télécharger le modèle français officiel (.tar.bz2)**.
   L'adresse officielle est :
   https://github.com/k2-fsa/sherpa-onnx/releases/download/asr-models/sherpa-onnx-streaming-zipformer-fr-2023-04-14.tar.bz2
4. Attendre le téléchargement complet puis appuyer sur
   **Importer l'archive française sur le téléphone** et choisir le
   fichier téléchargé. **Il est inutile de le décompresser.**
5. Une fois l'importation confirmée, cocher
   **Essayer sherpa-onnx français** puis arrêter et redémarrer l'écoute.
6. Dites « Jarvis, quelle heure est-il ? », puis trois autres questions.
   Testez « STOP » pendant une réponse parlée et une question après STOP.
7. En cas d'échec ou de performances insuffisantes, décochez le mode
   expérimental et redémarrez l'écoute : Vosk fonctionne comme avant.

## Sécurité de l'importation

- Seule l'archive officielle au nom exact est acceptée.
- Seuls quatre fichiers ONNX/int8 et tokens attendus sont copiés.
  Les noms d'entrées de TAR ne sont jamais réutilisés comme chemins
  de destination : protection contre les traversées de répertoires.
- Taille totale copiée bornée à 400 Mo ; mémoire de travail bornée ;
  aucune extraction des poids fp32 supplémentaires.
- Écriture dans un dossier temporaire puis permutation après validation.
  L'ancien modèle est conservé si l'import échoue avant la permutation.
- L'import refuse de se lancer pendant l'écoute. Pas de téléchargement
  silencieux ni transmission de données privées.
- L'archive officielle n'est pas vérifiée par signature cryptographique
  dans cette bêta : utiliser uniquement le lien indiqué par l'application.

## Tests GitHub et tests réels

- Tests CI Java : sélection du moteur désactivée par défaut, refus d'un
  modèle absent, secours Vosk et messages de diagnostic.
- Tests CI Python : un seul AudioRecord, STOP/Vosk indépendant, intégration
  Android, import de l'archive et absence de démarrage au boot.
- Compilation APK Android sur GitHub Actions pour confirmer les dépendances.
- Essais sur téléphone à faire avant toute fusion : permissions Android,
  modèle importé, dictée française, mémoire occupée, reprise d'écoute
  après lecture, STOP vocal, micro intégré et batterie.

**Statut : expérimental.** L'ajout des classes et de la dépendance ne vaut
pas preuve de performances sur matériel réel. Aucune mise à jour n'a été
faite sur la branche principale tant que la PR reste non fusionnée.

## Sources

- https://github.com/k2-fsa/sherpa-onnx
- https://k2-fsa.github.io/sherpa/onnx/java-api/android.html
- https://k2-fsa.github.io/sherpa/onnx/pretrained_models/online-transducer/zipformer-transducer-models.html
- https://developer.android.com/develop/background-work/services/fgs/restrictions-bg-start
