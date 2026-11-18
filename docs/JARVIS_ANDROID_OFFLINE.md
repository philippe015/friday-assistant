# FRIDAY Android — compatibilité des modèles IA locaux

La v3.11.0 ajoute LiteRT-LM à MediaPipe. Pour les téléphones Android ARM64 compatibles, **privilégier d'abord le CPU**, sans exiger OpenCL/GPU. L'application accepte `.task` (MediaPipe) et `.litertlm` (LiteRT-LM), mais les extensions seules ne prouvent pas qu'un modèle fonctionne.

| Niveau | Fichier exact | Taille approximative | Moteur |
|---|---|---|---|
| Essai léger | [`gemma3-270m-it-q8.task`](https://huggingface.co/litert-community/gemma-3-270m-it/blob/main/gemma3-270m-it-q8.task) | 304 Mo | MediaPipe CPU |
| LiteRT-LM plus léger | [`Qwen3-0.6B.litertlm`](https://huggingface.co/litert-community/Qwen3-0.6B/blob/main/Qwen3-0.6B.litertlm) | 614 Mo | LiteRT-LM CPU/GPU |
| Plus puissant | [`gemma-4-E2B-it.litertlm`](https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/blob/main/gemma-4-E2B-it.litertlm) | 2,59 Go | LiteRT-LM CPU/GPU |

**Pourquoi ne plus proposer `gemma-4-E2B-it-gpu.litertlm` par défaut ?** Ce fichier de 2,34 Go est documenté notamment pour les déploiements Web. Le fichier sans suffixe GPU est le modèle standard décrit pour Android CPU et GPU. La v3.11.0 déjà publiée contient l'ancien bouton ; ouvrir manuellement le lien du modèle standard ci-dessus. Les nouveaux boutons de choix sont prévus dans la prochaine compilation, et non dans l'APK déjà publiée.

**Installation sans ordinateur :** télécharger un seul modèle depuis le navigateur du téléphone, ouvrir FRIDAY → Réglages → Jarvis local → Importer un modèle IA local, choisir le fichier téléchargé, activer Jarvis local et effectuer **Tester Jarvis IA locale sans Internet**. Ne pas désinstaller l'application existante. L'import copie le modèle dans le stockage privé et nécessite assez d'espace libre pour la copie. Aucun modèle n'est contenu dans l'APK.

**Compatibilité :** application Android 8+ ARM64 uniquement selon la configuration actuelle. CPU par défaut pour les modèles LiteRT-LM sans `-gpu`; accélération GPU dépendante du chipset/pilote. Éviter `*-web.task`, les fichiers GGUF et les versions NPU spécifiques à une puce. Les performances sur le téléphone restent à valider par un vrai test. Les actualités nécessitent toujours Mistral + Internet.

La voix française, Memory+ chiffrée et l'arrêt vocal STOP sont conservés. Le runtime Python/Rust OpenJarvis complet n'est pas intégré dans l'APK.

---

# FRIDAY v3.10.0 — Jarvis IA autonome sur téléphone Android

## Objectif et périmètre vérifiable

**Sans ordinateur, sans serveur local, sans Termux.** Cette version ajoute un vrai **modèle de langage exécuté directement sur Android**. La synthèse vocale française, la reconnaissance Vosk, la mémoire Memory+ chiffrée, les informations d'heure locales et certains outils déterministes continuent de fonctionner sur le téléphone.

La puissance complète du programme Python/Rust **OpenJarvis** n'est pas copiée dans l'APK. Nous adaptons son architecture *agent → modèle → outils → mémoire* :
- moteur d'inférence Android : **MediaPipe Tasks GenAI v0.10.35**, via API officielle `LlmInference` ;
- modèle `.task` (ou `.bin` compatible MediaPipe) à importer **une fois depuis le téléphone** ;
- calcul arithmétique hors ligne avec un parseur borné et sans exécution de code ;
- recherche facultative dans les notes Memory+ chiffrées **sans réseau** ;
- commande vocale et lecture française déjà présentes dans FRIDAY ;
- Mistral/Web reste disponible séparément **uniquement si tu l'utilises**.

## Installer sans ordinateur

1. Installer FRIDAY v3.10.0 sur le téléphone Android **sans désinstaller l'ancienne version**. Le modèle n'est pas livré dans l'APK, pour éviter un fichier d'installation gigantesque.
2. Depuis le **navigateur du téléphone**, ouvrir [LiteRT Community — Gemma 3 270M](https://huggingface.co/litert-community/gemma-3-270m-it/tree/main). Ce dépôt est soumis à la licence de Gemma et peut demander un compte Hugging Face pour accepter les conditions. Choisir le fichier **`gemma3-270m-it-q8.task`** (environ 304 Mo), PAS `*-web.task` et PAS `*.litertlm`. Une autre tâche MediaPipe compatible peut convenir.
3. Dans FRIDAY, ouvrir **Réglages → Jarvis local sur Android → Importer un modèle IA local** puis choisir le fichier téléchargé. Attendre la copie complète en stockage privé ; ne pas fermer l'application pendant l'import.
4. Activer **Jarvis IA local**, puis utiliser **Tester Jarvis IA locale sans Internet**. Cette opération charge le modèle natif et effectue une vraie génération sur l'appareil. Si le moteur rejette le modèle ou manque de RAM, le test doit afficher un diagnostic ; un test ne suffit pas pour certifier le fonctionnement sur tous les appareils.
5. Laisser la case « toutes les questions » désactivée et dire **« Friday, Jarvis, explique-moi le fonctionnement d'un moteur »**. Après confirmation, cocher **Utiliser Jarvis local pour toutes les questions générales** si tu souhaites utiliser le modèle local sans passer par Mistral. En mode local forcé, une erreur du modèle **ne transmet pas automatiquement la question dans le cloud**.

## Capacités locales

| Fonction | Fonctionne sans PC | Fonctionne sans Internet après téléchargement |
|---|---|---|
| Microphone, reconnaissance vocale Vosk FR | Oui | Oui |
| Synthèse vocale Android FR | Oui, si moteur FR installé | Généralement oui selon le moteur TTS |
| Réponses générées par un modèle IA MediaPipe | Oui sur téléphone compatible | Oui |
| Calcul et commandes de base | Oui | Oui |
| Notes Memory+ chiffrées et recherche locale | Oui | Oui |
| Accès à des actualités des 7 dernières heures | Oui, via Mistral Web | **Non**, nécessite Internet |
| Agent Python/Rust OpenJarvis complet | **Non embarqué** | Non |
| Analyse autonome du Web, scripts shell, installation d'applications, accès fichiers système | **Non fournis** | Non |

Aucun écran ne fait croire que la commande `curl … | bash` lance Jarvis sur Android. L'ancienne passerelle facultative OpenJarvis-PC reste dans le projet pour compatibilité mais **n'est pas nécessaire** pour utiliser Jarvis local.

## Sécurité, confidentialité et risques pratiques

- Le modèle importé est enregistré dans le répertoire **privé** Android `files/local-jarvis/model.task`. L'application ne copie pas ce fichier sur GitHub ni vers un serveur.
- Le modèle est importé via le **sélecteur Android** ; l'application ne lit pas directement tous les fichiers du téléphone. La copie est limitée à **4,5 Go**, vérifie la taille, et remplace l'ancien modèle après une copie réussie.
- Les notes Memory+ sont déchiffrées **uniquement sur l'appareil** pour le contexte du modèle local ; une note fournie en contexte reste traitée comme une donnée et non une instruction. Elles ne quittent pas le téléphone par ce chemin.
- Le mode local est **désactivé par défaut** pour préserver les utilisateurs de l'ancien APK sans modèle installé.
- Le moteur peut demander beaucoup de RAM et de temps CPU. Un modèle volumineux peut ralentir ou fermer le processus sur certains appareils, notamment Android 16 et certains chipsets. Choisir un modèle compact, tester d'abord avec une réponse courte.
- Les actions autonomes potentiellement dangereuses (shell, suppression de fichiers, contrôle arbitraire des paramètres Android, accès sans consentement) ne sont pas proposées. Les commandes des agents sont limitées à des outils locaux explicitement codés.
- **Absence de génération vérifiée sur téléphone à ce stade** : les tests CI prouvent la compilation, pas la compatibilité d'un modèle chargé sur ton propre matériel.

## Provenance et licences

- [OpenJarvis (Apache 2.0)](https://github.com/open-jarvis/OpenJarvis) : architecture fonctionnelle et inspiration des agents/outils/mémoire ; **aucun runtime Linux copié dans l'APK**.
- [MediaPipe LLM Inference](https://github.com/google-ai-edge/mediapipe/blob/master/mediapipe/tasks/java/com/google/mediapipe/tasks/genai/llminference/LlmInference.java) : bibliothèque d'inférence Android utilisée.
- [LiteRT Community Gemma](https://huggingface.co/litert-community/gemma-3-270m-it/tree/main) : modèle optionnel soumis à sa propre licence et potentiellement à une acceptation explicite.
- [Guide Android LiteRT-LM](https://developers.google.com/edge/litert-lm/android) : alternative moderne pour les modèles `.litertlm` (non compatible avec l'import MediaPipe v3.10.0).

## Recette Android pour confirmer le fonctionnement

1. Installer la v3.10.0 par-dessus la v3.9.0.
2. Importer le modèle MediaPipe, faire le test en activant temporairement le **mode avion** pour confirmer l'absence de connexion réseau ; le bouton « Test local » doit encore générer.
3. Dire « Friday, Jarvis, combien font 2 + 3 fois 4 ? » et « Friday, Jarvis, raconte l'histoire de la Lune ».
4. Poser trois questions de suite avec « Friday ». Vérifier que la reconnaissance s'ouvre après la synthèse sans écho.
5. Désactiver le mode avion pour une question d'actualité avec Mistral ; celle-ci reste un **mode en ligne distinct**.
6. Si une requête locale échoue, copier le diagnostic dans les réglages et relever la taille du modèle et le message d'erreur (sans partage de clé ni de notes).

