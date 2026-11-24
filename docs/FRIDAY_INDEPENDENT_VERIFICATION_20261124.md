# FRIDAY 3.14.23 — Vérification indépendante des réponses (module n°2)

## Développement
Auto-Critique+ est conservé. Un **deuxième chemin de contrôle** complète
la relecture locale : il récupère des **sources Internet publiques** réellement
consultables, sur action explicite dans le Terminal Matrix.

Le bouton **« Vérifier sources Web »** :
1. Vérifie que l'Internet Jarvis est autorisé.
2. Contrôle que la question est publique et factuelle. Les demandes contenant
   des formulations de données personnelles, mots de passe, historique, GPS,
   coordonnées, secrets, URLs directes ou longs identifiants sont refusées.
3. Envoie uniquement la **question publique** à l'indexeur HTTPS déjà intégré ;
   la réponse à évaluer, l'historique, les profils et la mémoire chiffrée ne
   sont **jamais** joints à cette requête.
4. Recherche des références pertinentes sur des domaines distincts ; peut
   lire une page via le lecteur Web existant si disponible.
5. Affiche au plus quatre références : titre, URL HTTPS, domaine,
   extrait réel, statut « page lue » ou « extrait indexé/RSS », date RSS
   quand présente, et éventuel caractère institutionnel du domaine.
6. Pour les demandes portant sur une fenêtre RSS explicite, écarte les
   éléments qui ne disposent pas d'un horodatage récent compatible.
7. Affiche « SOURCES À COMPARER » ou « SOURCES INSUFFISANTES ».

## Niveau de certitude
- Un résultat d'un moteur de recherche ne prouve **jamais** à lui seul
  l'exactitude d'une réponse ; même un domaine institutionnel ne suffit pas.
- **Aucun faux label « réponse vérifiée »** n'est émis sur la seule base
  de la présence d'URLs, d'extraits, ou d'une deuxième génération IA.
- Le calculateur Java hors ligne (Auto-Critique+) peut toujours confirmer
  certaines opérations arithmétiques de manière indépendante.
- Cette version ne propose pas encore de vérification automatique
  affirmation-par-affirmation avec extraction structurée et correspondance
  aux citations : cela nécessiterait des contrôles supplémentaires.
- Si les sites sont indisponibles (403/429/JS), la vérification l'indique,
  sans inventer de références.

## Confidentialité et limites
- La vérification des sources est **manuelle**, pas déclenchée
  automatiquement par une relecture ou une réponse de Matrix.
- Les résultats sont éphémères et ne sont pas ajoutés aux corrections
  chiffrées tant qu'une correction n'a pas été volontairement approuvée.
- Le moteur ne peut pas accéder à des données privées par le bouton sources.
- Il n'y a ni modification des poids IA, ni téléchargement automatique de
  modèle, ni envoi de conversations vers GitHub.
- Les restrictions de confidentialité sont volontairement prudentes :
  certaines questions publiques peuvent nécessiter une reformulation.
- Les réseaux HTTPS ne sont pas toujours accessibles, et la qualité des
  sources varie. La vérification humaine reste nécessaire.

## Interface Android
Terminal Matrix → **AUTO-ÉVOLUTION** → **Vérifier sources Web**.
Cette fonction ne remplace pas **IA locale : analyser**. Les deux contrôles
ont des origines différentes :
- **IA locale : analyser** = diagnostic de cohérence et propositions non
  vérifiées par le modèle, avec contrôle arithmétique hors ligne quand possible.
- **Vérifier sources Web** = recherche publique indépendante, liens et
  extraits réellement obtenus, sans prétention de confirmation automatique.

Version DEBUG : \`3.14.23-source-verification-preview\`.
L'identifiant DEBUG est conservé : \`fr.friday.assistant.v31421matrix\`.
La version Google Play de release et sa signature ne sont pas modifiées.
