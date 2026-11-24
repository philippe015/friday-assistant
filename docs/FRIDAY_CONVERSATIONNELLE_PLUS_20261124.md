# FRIDAY 3.14.25 — Intelligence Conversationnelle+ (phase corrective)

Date : 24 novembre 2026. Branche de développement issue de 3.14.24.

## Anomalies réellement traitées

1. **« Sais-tu coder ? »** est reconnue comme une question sur
   les capacités de l'assistant. Réponse locale honnête et courte ;
   aucune recherche sur le mot « SAIS » ni d'affirmation sur des
   outils de compilation absents.
2. **« Donne-moi la météo, le temps qu'il fait »** n'est plus traitée
   comme la recherche d'une commune nommée « le temps que fait-il ».
   L'assistant demande la ville. Les variantes Vosk
   « la métaux sur l'Île-de-France » sont normalisées de façon prudente
   uniquement avec un contexte géographique explicite.
3. **« Météo dans le Val-de-Marne »** distingue le département
   d'une ville : la météo de Créteil est donnée comme **point
   indicatif**, et non comme prévision exacte pour tout le département.
   Même logique pour l'Île-de-France avec Paris.
4. **« Doyle à Vitry-sur-Seine »** après une demande de ville
   peut être interprétée comme « à Vitry-sur-Seine », seulement dans
   ce contexte météo direct : le préfixe Vosk erroné n'est plus
   envoyé à un moteur de recherche général.
5. **« Lis-moi les articles en entier »** ou
   « trouve-moi des articles en entier » n'ouvre plus une
   nouvelle recherche sur les mots « entier » et « article ».
   JARVIS présente le dernier bulletin RSS public **déjà conservé**
   et demande un numéro précis avant de consulter l'article.
   Aucune page d'éditeur n'est inventée si la recherche échoue.

## Conservé

- Jarvis local et la reconnaissance vocale Vosk ;
- Terminal Matrix plein écran, Auto-Critique+, vérification manuelle
  des sources publiques et Memory Evolution+ AES-GCM ;
- mémoire privée non transmise aux moteurs Web, aucune synchronisation
  automatique de diagnostics ou de conversations ;
- pas de nouvelle autorisation Android ni de modèle distant.

## Ce qui n'est PAS encore fait

Ce correctif cible les anomalies identifiées. Il n'implémente pas
encore un routeur universel à modèle d'intention neuronal, un système
complet de récupération sémantique par embeddings, une correction
générale des erreurs Vosk, ni un limiteur réseau complet garantissant
un temps de réponse maximal. Il ne vérifie pas sur un téléphone réel
la vitesse de génération d'un modèle .task/.litertlm chargé.

Le modèle local n'est pas agrandi, entraîné ni remplacé :
les gains attendus viennent de la bonne sélection des outils et du
suivi de contexte RSS existant.

## Validation

Tests JVM/Python de non-régression :
- question météo sans ville ;
- requête région/département ;
- nom de ville présent et reconnaissance d'un ASR déformé ;
- préservation du routeur de lecture RSS numérotée ;
- relance plurielle d'articles sans recherche générique ;
- capacités de programmation traitées localement ;
- identité et numéro DEBUG, release Google Play inchangée.

Version DEBUG : 3.14.25-conversation-plus-preview,
code 119, identifiant fr.friday.assistant.v31421matrix.
Une différence de signature peut bloquer la mise à jour ;
sauvegarder les données avant une éventuelle désinstallation.
