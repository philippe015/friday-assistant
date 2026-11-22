# Manuel complet d'installation — J.A.R.V.I.S. Android v3.14.16

> **Guide destiné aux futurs utilisateurs — 22 novembre 2026**  
> **Version : JARVIS v3.14.16 (bêta) · APK Android arm64 · ~43,5 Mo · installation directement sur téléphone sans PC.**  
> Les tests automatiques et la compilation de l'APK ont réussi sur GitHub Actions, mais la stabilité et la compatibilité ne sont pas garanties sur tous les téléphones. Cette version reste une **préversion**, non une version stable certifiée.

[**Télécharger directement l'APK v3.14.16**](https://github.com/philippe015/friday-assistant/releases/download/v3.14.16-community-github-bugreport-beta/JARVIS-v3.14.16-community-github-bugreport-beta.apk) · [Page officielle de la version](https://github.com/philippe015/friday-assistant/releases/tag/v3.14.16-community-github-bugreport-beta) · [Signaler un problème](https://github.com/philippe015/friday-assistant/issues)

## Sommaire

1. [Présentation et fonctions](#1-présentation-et-fonctions)
2. [Prérequis et compatibilité](#2-prérequis-et-compatibilité)
3. [Télécharger et installer l'APK](#3-télécharger-et-installer-lapk)
4. [Première ouverture et autorisations](#4-première-ouverture-et-autorisations)
5. [Installer la reconnaissance vocale française Vosk](#5-installer-la-reconnaissance-vocale-française-vosk)
6. [Installer et vérifier le moteur IA local](#6-installer-et-vérifier-le-moteur-ia-local)
7. [Configurer la voix, l'écoute et la recherche Internet](#7-configurer-la-voix-lécoute-et-la-recherche-internet)
8. [Utiliser JARVIS : exemples concrets](#8-utiliser-jarvis--exemples-concrets)
9. [Mémoire, journaux et vie privée](#9-mémoire-journaux-et-vie-privée)
10. [Signaler un bug à la communauté GitHub](#10-signaler-un-bug-à-la-communauté-github)
11. [Résoudre les problèmes courants](#11-résoudre-les-problèmes-courants)
12. [Mises à jour, sauvegardes et désinstallation](#12-mises-à-jour-sauvegardes-et-désinstallation)
13. [Informations techniques, intégrité et source](#13-informations-techniques-intégrité-et-source)

---

## 1. Présentation et fonctions

JARVIS est un assistant vocal en français pour **smartphones Android**, construit autour d'une interface Cockpit et d'un moteur IA local :

- **Reconnaissance vocale en français** avec Vosk, après téléchargement initial du modèle acoustique.
- **Synthèse vocale Android** pour lire les réponses à voix haute.
- **Écoute continue** lorsque l'application est active, avec reprise après la réponse et une période de protection contre l'écho.
- **Moteur IA local** utilisant MediaPipe pour un modèle compatible \`.task\` ou LiteRT-LM pour un modèle compatible \`.litertlm\`. Un modèle doit être importé séparément.
- **Questions écrites**, utilisables même lorsque le microphone est arrêté.
- **Recherche Web HTTPS et actualités RSS** facultatives, avec indication des sources. Elles nécessitent Internet, et la lecture des articles dépend de l'accès réel au contenu du média.
- **Mémoire et historique chiffrés localement** par Android Keystore.
- **Journal de diagnostic chiffré** ; export JSON manuel sans ou avec conversation, au choix.
- **Bouton « Signaler sur GitHub »** pour contribuer à la correction d'anomalies, sans téléverser les conversations.

Le modèle IA est exécuté sur le **téléphone**. Le programme Linux OpenJarvis complet n'est pas incorporé dans cet APK. La météo, le Web, les flux RSS et les téléchargements de modèles nécessitent Internet ; les questions locales peuvent fonctionner hors connexion après installation des modèles compatibles.

## 2. Prérequis et compatibilité

| Élément | Exigence / information |
| --- | --- |
| Android | **Android 8.0 ou plus** (API 26 minimum) |
| Architecture | **ARM 64 bits / arm64-v8a uniquement** : les appareils 32 bits et les architectures non ARM ne sont pas prises en charge par cet APK |
| Microphone | Microphone Android disponible et autorisation accordée pour l'écoute vocale |
| Voix | Moteur de synthèse vocale Android avec voix française installée |
| Stockage | **APK ~43,5 Mo**, plus environ **41 Mo** de modèle Vosk FR et le modèle IA choisi (**~304 Mo ou ~2,34 Go**). Prévoir beaucoup plus d'espace libre pour les téléchargements, l'importation et les fichiers temporaires |
| Connexion | Requise pour télécharger l'APK et les modèles ; facultative ensuite pour les fonctions locales |
| IA locale | La compatibilité dépend de la RAM, du processeur, du GPU et de ses pilotes ; **non garantie par la version Android seule** |
| Ordinateur / Termux | **Inutiles** pour installer et utiliser l'APK sur un téléphone compatible |

L'APK est une **bêta signée en mode debug** et a pour identifiant Android \`fr.friday.assistant.readerfixpreview\`. Une signature différente entre deux APK peut empêcher l'installation en mise à jour. Les anciennes éditions à identifiant différent utilisent des stockages privés différents.

## 3. Télécharger et installer l'APK

### Sur le smartphone, sans ordinateur

1. Ouvrez cette [page GitHub officielle de la v3.14.16](https://github.com/philippe015/friday-assistant/releases/tag/v3.14.16-community-github-bugreport-beta).
2. Dans **Assets / Ressources**, sélectionnez **\`JARVIS-v3.14.16-community-github-bugreport-beta.apk\`**. Vous pouvez aussi utiliser le [téléchargement direct](https://github.com/philippe015/friday-assistant/releases/download/v3.14.16-community-github-bugreport-beta/JARVIS-v3.14.16-community-github-bugreport-beta.apk).
3. Dans **Téléchargements** (ou le gestionnaire de fichiers Android), touchez le fichier \`.apk\`.
4. Si Android bloque l'installation, ouvrez l'écran d'autorisation qu'il propose : **Autoriser l'installation depuis cette source** (navigateur ou gestionnaire de fichiers). Selon le fabricant : **Paramètres → Applications → Accès spécial → Installation d'applications inconnues**. Accordez ce droit uniquement à l'application que vous utilisez pour ouvrir l'APK.
5. Confirmez **Installer**, puis **Ouvrir**. Une fois terminé, vous pouvez retirer l'autorisation d'installation de sources inconnues.
6. Vérifiez que le Cockpit indique **v3.14.16** ou ouvrez les réglages / l'écran de gestion des versions.

**Sécurité :** téléchargez exclusivement depuis les Releases du dépôt \`philippe015/friday-assistant\` ; ne confondez pas le fichier \`.apk\` avec les archives du code source \`.zip\`. Android peut afficher un avertissement pour une application indépendante non publiée sur le Play Store. Vérifiez l'origine plutôt que de désactiver globalement les protections du téléphone.

### Si l'installation affiche « Application non installée »

La cause peut être une **signature Android différente**, un appareil non ARM64, un stockage insuffisant ou une incompatibilité du système. **Ne désinstallez pas l'ancienne version pour forcer l'installation sans avoir vérifié ce qui pourrait être perdu** : les fichiers privés, modèles et conversations chiffrées peuvent être supprimés sans restauration possible. Consultez la section [12](#12-mises-à-jour-sauvegardes-et-désinstallation).

## 4. Première ouverture et autorisations

1. Lancez **JARVIS · IA locale**.
2. Accordez la permission **Microphone** si vous souhaitez parler à JARVIS.
3. Sur les versions Android concernées, autorisez les **notifications** si vous souhaitez voir l'indication du service d'écoute actif. Selon les paramètres Android, une restriction des notifications peut gêner l'utilisation du service.
4. **Localisation** : facultative. Ne l'autorisez que si vous souhaitez les fonctions de position et d'alentours. Aucun objectif de précision à trois mètres ne peut être garanti.
5. Si le modèle Vosk français manque, JARVIS propose son téléchargement avant d'activer l'écoute. Il ne doit pas être confondu avec le modèle **IA** qui produit les réponses.

Les autorisations exactes dépendent de la version d'Android et du fabricant. **L'écoute automatique concerne l'ouverture de l'application visible**, pas un démarrage permanent et invisible après redémarrage du téléphone : les restrictions Android, notamment à partir d'Android 14, s'appliquent.

## 5. Installer la reconnaissance vocale française Vosk

**Vosk = transformer la voix en texte**, pas répondre à la question.

1. Sur le Cockpit, touchez **Activer écoute**. Si le modèle français est absent, acceptez **Télécharger et activer**.
2. Ou ouvrez **Réglages → Moteur vocal local · français Vosk → Télécharger le modèle français**.
3. Autorisez une connexion Internet et patientez jusqu'à l'installation complète du modèle (**environ 41 Mo à télécharger**). Le stockage réellement utilisé peut différer.
4. Lorsque le statut confirme que Vosk est prêt, utilisez **Activer écoute** si nécessaire.
5. Parlez normalement et vérifiez que votre question apparaît dans l'historique du Cockpit.

Vous pouvez conserver **Démarrer automatiquement l'écoute quand j'ouvre JARVIS** activé pour l'ouverture de l'application. Si **Mode une question** est coché, le micro s'arrête après la réponse : décochez-le pour le mode continu.

**Important :** Vosk peut mal interpréter un nom propre ou un nombre. Lorsque la transcription ne correspond pas à la demande, reformulez ou essayez la question écrite pour isoler un problème de reconnaissance vocale d'un problème du moteur IA.

## 6. Installer et vérifier le moteur IA local

**Sans modèle IA compatible importé, JARVIS ne peut pas générer ses réponses libres.** Ce téléchargement est distinct de Vosk.

### Choisir un modèle

| Option | Taille indicative | Usage |
| --- | ---: | --- |
| [Gemma 3 270M — \`gemma3-270m-it-q8.task\`](https://huggingface.co/litert-community/gemma-3-270m-it/tree/main) | **~304 Mo** | Premier test plus léger avec MediaPipe |
| [Gemma 4 E2B GPU — \`gemma-4-E2B-it-gpu.litertlm\`](https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/blob/main/gemma-4-E2B-it-gpu.litertlm) | **~2,34 Go** | Modèle plus volumineux ; requiert un GPU/RAM/pilote compatibles |

Les fichiers sont distribués séparément par leurs éditeurs sur Hugging Face, peuvent être soumis à des conditions d'utilisation et nécessiter une connexion ou acceptation de licence. **Un fichier \`GGUF\`, un modèle destiné au navigateur \`*-web.task\` ou une archive \`.tar.bz2\` n'est pas un modèle compatible à importer ici.**

### Installation du modèle sur Android

1. Ouvrez le lien du modèle choisi depuis le téléphone.
2. Téléchargez **le fichier exact** \`.task\` ou \`.litertlm\`, et non une page HTML enregistrée par erreur.
3. Ouvrez JARVIS → **Réglages** → **Importer un modèle IA local (.task / .litertlm)**.
4. Dans le sélecteur Android, choisissez le fichier dans **Téléchargements**.
5. Laissez JARVIS **copier complètement le fichier dans son stockage privé** ; évitez de fermer l'application pendant l'importation.
6. Appuyez sur **Tester Jarvis IA locale sans Internet**. Ce bouton tente une génération effective sur l'appareil : le test réussi est plus significatif que le seul message « fichier importé ».
7. Effectuez ensuite un essai écrit simple : **« Présente-toi en deux phrases en français. »** Le texte doit s'afficher et, si la voix est activée, être prononcé.

**Pour Gemma 4 GPU**, le téléchargement et l'importation peuvent nécessiter plusieurs gigaoctets disponibles. Un modèle uniquement GPU peut échouer sur un téléphone dont les pilotes OpenCL ou la mémoire sont insuffisants ; la reprise CPU ne résout pas nécessairement cette incompatibilité. Essayez alors le modèle léger compatible.

L'IA locale ne nécessite **aucune clé API Mistral ou OpenAI** dans cette édition JARVIS.

## 7. Configurer la voix, l'écoute et la recherche Internet

Dans **Réglages**, contrôlez les options suivantes :

| Réglage | Action conseillée |
| --- | --- |
| **Démarrer automatiquement l'écoute quand j'ouvre JARVIS** | Activer si vous souhaitez parler dès l'ouverture, avec les permissions accordées |
| **Répondre à voix haute** | Activer ; installer/configurer une voix française dans les paramètres de synthèse vocale Android |
| **Mode une question** | **Décocher** pour garder le mode continu |
| **Autoriser Jarvis à rechercher des informations sur Internet** | Activer si vous souhaitez météo, actualités et recherches publiques ; désactiver pour limiter l'usage réseau |
| **Rechercher sur le Web avant toute réponse générale** | Fonction optionnelle : le mode activé par défaut peut augmenter les délais et transmettre les mots-clés de recherche aux fournisseurs Web |
| **Localisation** | Activer uniquement si besoin. Le partage précis et les permissions doivent rester sous contrôle utilisateur |

Après une réponse vocale, une protection anti-écho d'environ **2,5 secondes** précède la reprise des questions directes. Le mot **« Friday »** ou **« Jarvis »** peut servir à réactiver une question après une pause. **Le STOP vocal est retiré** de cette édition : pendant une réponse, utilisez le bouton **« Interrompre la réponse · rester à l'écoute »**.

La sortie audio utilise le haut-parleur du téléphone. **Le microphone Bluetooth n'est pas pris en charge dans cette configuration** ; l'utilisation d'un casque Bluetooth ne garantit pas la capture de sa voix.

## 8. Utiliser JARVIS : exemples concrets

Pour commencer, testez dans cet ordre :

1. **Par écrit :** « Qui es-tu ? » ou « Explique ce qu'est un moteur électrique. » — teste l'IA sans Vosk.
2. **À voix haute :** « Jarvis, quelle heure est-il ? » — teste Vosk et la voix.
3. **En ligne :** « Jarvis, donne les dernières actualités scientifiques et cite les sources. » — teste la recherche HTTPS.
4. **RSS :** après un bulletin comportant plusieurs articles, dites « Jarvis, lis l'article numéro deux en entier. » — JARVIS tente de lire le **texte public accessible de l'éditeur**, mais ne peut garantir l'intégralité d'un article payant, bloqué, JavaScript ou introuvable.
5. **Analyse d'article :** « Jarvis, analyse l'article numéro un avec mon profil. » — nécessite un texte récupérable et un modèle IA local prêt.
6. **Météo :** « Jarvis, quelle est la météo à Paris ? » — nécessite une connexion Internet.
7. **GPS, facultatif :** « Jarvis, où suis-je ? » — nécessite une permission Android et un fournisseur de position opérationnel.

Les titres de flux RSS disposent parfois seulement d'un horodatage de collecte, qui n'est **pas nécessairement la date originale de publication**. Lorsqu'une source n'est pas lisible, JARVIS doit le signaler plutôt que présenter une analyse imaginaire.

## 9. Mémoire, journaux et vie privée

JARVIS dispose de deux supports différents :

- **Historique et mémoire de conversation** : conservés dans le stockage privé Android, chiffrés via **Android Keystore AES-GCM**. L'historique des échanges est limité aux **80 derniers messages** ; il ne constitue pas un enregistrement infini.
- **Journal technique** : événements fixes (microphone, reconnaissance, recherche Web, modèles, lecture vocale, durée), conservés de manière chiffrée avec une rotation d'environ **14 jours** et **420 événements** maximum. Il n'enregistre pas le texte des questions.

### Exporter un rapport local avec conversation

Cockpit → **Exporter rapport / conversation** → cochez explicitement **« Inclure la conversation : questions et réponses »** → **Choisir le fichier**. Un JSON est créé à l'endroit sélectionné via le sélecteur Android.

Le JSON enrichi contient \`events[]\` + \`conversation[]\` + \`timeline[]\` pour les messages datés. Les questions de l'utilisateur et les réponses de JARVIS sont présentes en clair dans **ce fichier exporté**, même si l'historique d'origine demeure chiffré. Les conversations anciennes peuvent ne pas posséder d'horodatage fiable.

**Attention : ce JSON n'est PAS chiffré. Il peut contenir vos propos, des données personnelles, des URL et des informations sensibles. Ne le publiez pas sur GitHub.**

### Exporter seulement les codes

Laissez **« Inclure la conversation »** **décoché**, puis sauvegardez. Le fichier contient uniquement les codes techniques et les durées sans transcriptions.

La sauvegarde locale des JSON ne déclenche aucune publication ni synchronisation automatique.

## 10. Signaler un bug à la communauté GitHub

L'entraide communautaire est intégrée à la v3.14.16 :

1. Dans le Cockpit, appuyez sur **Signaler sur GitHub**, ou sur **Signaler une anomalie sur GitHub** dans les réglages Android.
2. JARVIS prépare un **brouillon technique limité** avec la version de l'application, le SDK Android et **au plus 28 événements fixes** avec des délais **relatifs**. Il ne copie **aucune conversation**, transcription, clé, URL, identité, position GPS ou date absolue dans ce brouillon.
3. **Lisez attentivement** le texte affiché dans la confirmation Android. Vous pouvez **annuler**.
4. En choisissant **Ouvrir GitHub**, le navigateur reçoit le brouillon dans l'adresse de la nouvelle Issue ; le contenu est transmis à GitHub **dès l'ouverture de la page**, même si le ticket n'est finalement pas publié.
5. Connectez-vous à GitHub si nécessaire, décrivez la panne et les étapes pour la reproduire, relisez le contenu, puis cliquez sur le bouton GitHub de publication **uniquement si vous consentez à rendre le ticket public**.

[**Ouvrir la page des signalements du projet**](https://github.com/philippe015/friday-assistant/issues) · [**Créer un nouveau signalement**](https://github.com/philippe015/friday-assistant/issues/new)

**Ne joignez jamais le rapport JSON complet avec conversation à un ticket public.** Vous pouvez décrire un cas fictif ou reformuler le problème sans divulguer de paroles privées. La publication d'un ticket nécessite un compte GitHub ; JARVIS n'utilise aucun jeton GitHub et ne publie rien automatiquement.

## 11. Résoudre les problèmes courants

| Problème observé | Contrôles et solution |
| --- | --- |
| **APK refusée / application non installée** | Vérifier Android 8+, architecture ARM64, stockage, provenance du fichier et signature d'une version précédente. Ne pas désinstaller avant d'évaluer les pertes possibles |
| **Microphone muet** | Autorisation Micro, modèle Vosk FR installé, **Mode une question** décoché, bouton **Activer écoute**, éventuelles restrictions Android du micro/service |
| **JARVIS répond par écrit mais pas à la voix** | Examiner la transcription Vosk, les autorisations et la synthèse vocale française ; utiliser une phrase courte avec « Jarvis » |
| **Paroles non reconnues / mauvais numéro d'article** | Reformuler « article deux », puis vérifier la transcription et le dernier bulletin RSS ; un mauvais numéro Vosk ne prouve pas une panne de l'IA |
| **JARVIS ne parle pas** | Activer **Répondre à voix haute**, régler le volume multimédia/voix et installer la voix française Android |
| **« Modèle IA local manquant »** | Importer un vrai fichier \`.task\` ou \`.litertlm\` ; lancer **Tester Jarvis IA locale sans Internet** |
| **Erreur GPU / LiteRT / manque de mémoire** | Essayer un modèle compatible plus petit, fermer les applications gourmandes ; CPU/GPU et RAM doivent être pris en charge |
| **Pas de nouvelles / RSS indisponible** | Activer Internet JARVIS, vérifier la connexion ; les moteurs peuvent filtrer des résultats et certains médias bloquent leur contenu |
| **Article RSS introuvable** | Redemander d'abord un bulletin actualisé ; certains liens Google Actualités ne permettent pas de retrouver une page publique vérifiable de l'éditeur |
| **GPS indisponible** | Accorder l'autorisation Android, activer la localisation et tester dans un espace plus dégagé ; précision de 3 m non garantie |
| **Modèle Vosk impossible à télécharger** | Vérifier réseau et espace disponible, recommencer avec **Télécharger le modèle français** |
| **Un problème persiste** | Utiliser **Signaler sur GitHub** pour préparer un rapport de codes sans conversation, puis décrire comment reproduire la panne |

## 12. Mises à jour, sauvegardes et désinstallation

Cette APK bêta utilise l'identifiant \`fr.friday.assistant.readerfixpreview\` et une signature de compilation **debug**. Android autorise une mise à jour en place **seulement si** l'identifiant de package **et la signature** sont compatibles. Les APK de tests issus de différentes compilations GitHub Actions peuvent avoir des signatures différentes.

- **Si une version fonctionne déjà**, ne la supprimez pas uniquement pour tester une nouvelle APK.
- Sauvegardez manuellement les données exportables nécessaires (profils, diagnostics) avant une opération risquée. L'export de diagnostic **n'est pas une restauration complète de l'application**.
- Les modèles IA, l'historique chiffré, les réglages et les clés Android Keystore sont des **données privées propres à l'installation**. Après désinstallation, leur récupération ne peut pas être garantie. Android \`allowBackup=false\` : ne supposez pas qu'une sauvegarde cloud automatique permettra de les restaurer.
- Les différentes éditions ayant un identifiant de package distinct peuvent coexister, mais leurs modèles et mémoires **ne sont pas nécessairement partagés**.
- Si vous remplacez le modèle IA, conservez le fichier téléchargé d'origine jusqu'à ce que la nouvelle importation et le test réussissent.
- Vérifiez toujours le numéro de version affiché dans le Cockpit après installation.

Cette version a été compilée et testée automatiquement ; elle demeure **bêta** et n'est pas certifiée par un essai sur toutes les marques et versions Android.

## 13. Informations techniques, intégrité et source

**Version :** \`3.14.16\` · **Code Android :** \`110\` · **ID de l'APK bêta :** \`fr.friday.assistant.readerfixpreview\`  
**Taille vérifiée de l'APK :** \`43 505 683 octets\` (~43,5 Mo).  
**SHA-256 :** \`c72fa8b8d566a54f5e1602175ee1730b0d65dca0f5b31eeb51e239561090086d\`  
**Fichier officiel :** \`JARVIS-v3.14.16-community-github-bugreport-beta.apk\`

Le fichier [**SHA256SUMS.txt**](https://github.com/philippe015/friday-assistant/releases/download/v3.14.16-community-github-bugreport-beta/SHA256SUMS.txt) publié dans les ressources de la Release permet de comparer l'empreinte à celle de l'APK téléchargée. Sur Android, l'outil de hachage n'est pas nécessaire pour une installation ordinaire ; pour un contrôle avancé, comparez le SHA-256 obtenu avec la valeur ci-dessus. **Une empreinte valide ne garantit pas à elle seule la sécurité ou la compatibilité du logiciel.**

- [Release officielle JARVIS v3.14.16](https://github.com/philippe015/friday-assistant/releases/tag/v3.14.16-community-github-bugreport-beta)
- [Code source correspondant exactement à l'APK (tag v3.14.16)](https://github.com/philippe015/friday-assistant/tree/v3.14.16-community-github-bugreport-beta)
- [Contribution GitHub — Pull Request #105](https://github.com/philippe015/friday-assistant/pull/105)
- [Issues — aide, anomalies et propositions](https://github.com/philippe015/friday-assistant/issues)
- [Historique du projet et guide hors ligne antérieur](JARVIS_ANDROID_OFFLINE.md)

> **Note pour les développeurs :** la branche par défaut \`main\` et son historique peuvent ne pas contenir encore les changements de cette bêta. Le **tag de Release** est la référence pour le code correspondant à l'APK. Le dépôt comporte plusieurs éditions et workflows historiques ; ne pas confondre leur configuration avec la v3.14.16.

---

**Utilisation responsable.** Un assistant peut se tromper et une transcription vocale peut modifier le sens d'une commande. Pour les informations médicales, juridiques, de sécurité ou d'actualité, vérifiez les sources avant d'agir. Les fonctions nécessitant des services en ligne restent soumises à la disponibilité et aux conditions de ces services.
