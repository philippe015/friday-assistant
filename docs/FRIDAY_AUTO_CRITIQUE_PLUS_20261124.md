# FRIDAY / JARVIS — Auto-Critique+ (préversion Android 3.14.22)

## Ce qui est réellement implémenté
- Analyse à la demande ou automatique (opt-in) des réponses écrites du Terminal Matrix.
- **Chemin d'inférence dédié** \`LocalJarvisEngine.reviewRaw\` : l'auto-critique
  ne reçoit ni profil utilisateur, ni historique privé, ni mémoire
  d'apprentissage, ni extrait Web en supplément de la question et de la réponse
  expressément sélectionnées. Elle ne va pas sur Internet.
- **Contrôle mathématique indépendant** : lorsqu'une expression arithmétique
  entière est acceptée par le calculateur Java local, Auto-Critique+ compare
  le résultat numérique de la réponse à ce calcul, sans demander au modèle IA
  de valider son propre raisonnement.
- Pour les autres demandes, le modèle local renvoie trois champs :
  \`VERDICT\`, \`MOTIF\` et \`CORRECTION\`. Le modèle peut se tromper.
- Les faits évolutifs (actualité, météo, prix, etc.) ne sont **jamais**
  déclarés vérifiés par une simple seconde génération du modèle.
- Réponses de revue structurées pour Matrix : statut, motif, proposition
  facultative, indicateur de vérification par outil.
- Anti-mélange : une analyse asynchrone est rattachée à la paire exacte
  question/réponse d'origine. Si une nouvelle réponse arrive, la précédente
  ne peut pas être validée par erreur pour cette autre réponse.
- Validation humaine explicite et sauvegarde chiffrée AES-GCM uniquement sur
  Android pour les corrections approuvées ; aucune sauvegarde automatique des
  propositions de l'IA.

## Ce qui reste limité
- Il ne s'agit **pas** d'un second modèle spécialisé ni d'un entraînement
  autonome. Le même modèle local effectue la relecture.
- Les tests CI vérifient les règles et la compilation, pas la qualité du
  langage généré par un vrai modèle sur le téléphone.
- Audit intégral limité à 260 caractères de question et 1 500 caractères
  de réponse : un contenu plus long est refusé explicitement, **jamais**
  tronqué silencieusement.
- Les calculs décimaux, les affirmations scientifiques et les actualités ne
  bénéficient pas encore d'une vérification indépendante générale.
- Le diagnostic IA peut signaler une erreur ou n'en voir aucune, sans
  constituer une preuve d'exactitude.
- Réexécuter le même modèle après chaque réponse consomme de la RAM et de la
  batterie ; l'auto-analyse reste facultative.

## Tester sur Android
1. Installer l'APK de test avec signature compatible et vérifier les données
   de l'ancienne version avant toute désinstallation.
2. Ouvrir Matrix puis la section « AUTO-ÉVOLUTION ».
3. Activer Apprentissage (ON). Vérifier que le modèle local fonctionne.
4. Poser une question puis cliquer « IA locale : analyser ».
5. Pour tester le contrôle déterministe : demander une multiplication et
   comparer le diagnostic au calculateur Java.
6. Vérifier l'affichage « DIAGNOSTIC » et « MOTIF », puis l'absence de
   mémorisation tant que « Approuver et enregistrer chiffré » n'a pas été touché.
7. Poser une autre question pendant une relecture et vérifier que l'ancienne
   proposition n'est pas applicable à cette nouvelle réponse.

La version de la préversion est affichée comme
\`3.14.22-auto-critique-preview\`. L'identifiant Android DEBUG existant
\`fr.friday.assistant.v31421matrix\` est conservé afin d'éviter un changement
de stockage privé entre préversions *lorsque la signature de l'installation
est compatible*. Ceci n'est pas une version Google Play.
