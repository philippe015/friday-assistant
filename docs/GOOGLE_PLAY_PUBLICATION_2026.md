# JARVIS pour Google Play Console — préparation 23 novembre 2026

Statut : **PRÉPARATION / AUCUNE PUBLICATION DÉCLENCHÉE**.
Console privée indiquée par le propriétaire :
https://play.google.com/console/u/0/developers/5340777987600946368/app/4976446886469015692/app-dashboard

## Alerte importante visible dans Play Console

La capture du journal d'activité du 23/11/2026 affiche :
- **JARVIS** / **`jarvis.myapp`** ;
- **Signature d'application > Activer** : modification enregistrée à 07:45 ;
- un bandeau rouge : **« Compte de développeur risquant d'être clôturé pour inactivité »**.

Ouvrir **« Afficher les détails »** sur le bandeau rouge et relever la
**date butoir exacte** indiquée par Google. Cette capture n'affiche
pas l'échéance : ne pas la deviner.

D'après la documentation officielle de Google, les démarches pour conserver
un compte concerné peuvent inclure **la vérification de l'e-mail et du téléphone
de contact du compte** **et** **le téléversement/la mise à disposition d'une
application**, une piste de test interne/fermé pouvant convenir sans lancer
de production. Effectuer les actions demandées spécifiquement par le
message de Google, avant la date limite.

Source : https://support.google.com/googleplay/android-developer/answer/11605267

## 1. Compatibilité Android

- Identifiant montré sur la capture Play Console du 23 novembre 2026 :
  **`jarvis.myapp`**. Ne pas télécharger un AAB signé sous `fr.friday.assistant`
  dans cette application. L'AAB de Google Play est construit en passant
  **`-PjarvisPlayPackage=jarvis.myapp`** au workflow dédié. Le `namespace`
  Java/Kotlin reste `fr.friday.assistant` ; cela ne modifie pas les classes.
  Les APK de développement hors Google Play conservent leur identifiant actuel.
  Vérifier aussi l'écran « Intégrité de l'application » pour la signature et la clé.
- Identifiant des APK debug précédents différent : `fr.friday.assistant.v31418voicefix` ;
  **ne pas** envoyer un APK debug à Google Play.
- `compileSdk 36` et `targetSdk 36`, requis pour les nouvelles publications standard
  au 23 novembre 2026. Android Gradle Plugin 8.10.1 et SDK/API 36.
- Version interne **3.14.18**, `versionCode 112` à confirmer contre l'historique des
  versions **de la console** : si 112 a déjà été importé, augmenter ce nombre
  avant de construire un nouvel AAB.
- Java 17, Gradle 8.13, Kotlin, Rust JNI Android arm64, Chaquopy, LiteRT-LM, Vosk.

## 2. Signer un Android App Bundle (AAB)

Le workflow GitHub manuel `.github/workflows/play-console-bundle.yml`
construit `app/build/outputs/bundle/release/app-release.aab` pour **`jarvis.myapp`**. Il est
volontairement **manuel**, sans déploiement automatique ni publication Play.

Prévoir ces **secrets GitHub Actions** dans Settings > Secrets and variables > Actions :

- `KEYSTORE_BASE64` : contenu du **fichier keystore de téléversement** encodé en Base64.
- `KEYSTORE_PASSWORD` : mot de passe du keystore.
- `KEY_ALIAS` : alias.
- `KEY_PASSWORD` : mot de passe de la clé de téléversement.

**Ne jamais envoyer les clés, mots de passe ou le keystore dans un chat,
un problème GitHub, une PR ou des fichiers du dépôt.**

Si le package a déjà une clé de téléversement enregistrée dans Play App Signing,
réutiliser celle-ci ; ne pas générer un nouveau certificat sans procédure de
changement de clé. Si aucune clé n'existe, la créer **localement** dans un
environnement privé et l'enregistrer dans un gestionnaire de secrets sécurisé.

À partir de GitHub Actions > JARVIS - Google Play signed AAB (manual) >
Run workflow, choisir la branche validée puis lancer manuellement.
La tâche produit l'artifact `JARVIS-play-signed-aab` uniquement après
vérification `jarsigner -verify -strict` et SHA-256.

## 3. Entrer les informations dans Play Console

1. **Vérifier** dans Tableau de bord > Paramètres de l'application
   l'identifiant déjà visible **`jarvis.myapp`**, Play App Signing,
   la clé de téléversement et les pistes de test déjà existantes.
2. Préparer les informations de la fiche (nom, description, icône 512x512,
   visuels et captures d'écran Android réels), ainsi qu'une politique de
   confidentialité publique opérationnelle.
3. Compléter **Contenu de l'application** : classification de contenu,
   audience, accès à l'application, publicités (si applicable), collecte
   et transmission des données, justificatifs éventuels des autorisations
   microphone, localisation, service de premier plan.
4. Télécharger l'AAB **signé** dans **Tester et publier > Tests internes**
   (ou la piste de test adaptée) ; corriger toute erreur de validation
   de Google Play avant le lancement.
5. Installer la version distribuée par la **piste Play** et tester écoute,
   arrêt, reconnexion micro, lecture RSS, réseau, GPS, appareil sans modèle
   local, import du modèle et écrans.
6. Pour les **comptes personnels créés après le 13 novembre 2023**,
   Google Play exige généralement 12 testeurs inscrits en continu pendant
   14 jours sur la piste de test fermé avant la demande d'accès production.
   Vérifier les instructions propres au compte dans la console.
7. Passer en production **seulement** après tests sur appareils et examen
   de conformité. Aucune action de publication n'est incluse ici.

## 4. Vérifications bloquantes avant production

- **Package/clé existants** : le package Play cible est `jarvis.myapp` ;
  toute erreur de certificat ou de code de version peut bloquer la publication.
- **VersionCode unique** : vérifier les numéros déjà utilisés dans Play Console.
- **API 36 et comportements Android 16** : contrôler l'edge-to-edge,
  le bouton Retour, le microphone en service premier plan, les notifications,
  la gestion de l'emplacement et la compatibilité multi-écrans.
- **ARM64 uniquement** : vérifier que les appareils ciblés par le Play Store
  sont correctement pris en charge ; pas de promesse « tous téléphones ».
- **Compatibilité 16 Ko** : le workflow vérifie les en-têtes ELF des bibliothèques
  natives avec `scripts/check_play_16kb_elf.py`. Ce contrôle n'équivaut
  pas à une installation Play sur un appareil utilisant des pages de 16 Ko.
  Vérifier aussi l'alignement ZIP du bundle et tester sur un appareil compatible.
  Référence Android : https://developer.android.com/guide/practices/page-sizes
- **Permissions et vie privée** : les requêtes web et les demandes de
  commerces à proximité peuvent être envoyées à des prestataires ; les
  rapports de conversation sont exportables par consentement et contiennent
  des données en clair. La déclaration « aucune donnée transmise » serait fausse.
- **Politique de confidentialité** : compléter et publier un lien public
  persistant qui précise les traitements, fournisseurs, contacts et demandes
  de suppression, puis intégrer ce lien dans l'application et sa fiche.
- **Logo/nom originaux** : éviter toute implication d'une affiliation à
  une franchise ou à un éditeur tiers.

Sources Google :
- https://support.google.com/googleplay/android-developer/answer/11926878
- https://support.google.com/googleplay/android-developer/answer/9859348
- https://support.google.com/googleplay/android-developer/answer/9842756
- https://support.google.com/googleplay/android-developer/answer/10144311
- https://support.google.com/googleplay/android-developer/answer/14151465

**Non réalisé :** aucun changement sur le compte privé Play Console,
aucune clé créée ou téléversée, aucun AAB signé généré sans secrets,
aucun téléchargement de version dans la console, aucune diffusion publique.
