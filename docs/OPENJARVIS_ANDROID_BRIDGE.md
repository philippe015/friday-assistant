# Intégration OpenJarvis ↔ FRIDAY Android v3.9.0

## Ce qui est réellement intégré

**FRIDAY Android reste une application native** (Vosk, synthèse Android, Mistral/Web, mémoire locale chiffrée). Un **nouveau client HTTPS OpenJarvis** permet d'envoyer certaines questions à un serveur OpenJarvis tournant sur votre PC Linux/Manjaro, puis de faire prononcer sa réponse par FRIDAY.

OpenJarvis est un projet tiers sous licence **Apache-2.0** :
- Code source : https://github.com/open-jarvis/OpenJarvis
- Installation : https://open-jarvis.github.io/OpenJarvis/getting-started/install/
- API documentée : https://open-jarvis.github.io/OpenJarvis/deployment/api-server/
- Licence : https://github.com/open-jarvis/OpenJarvis/blob/main/LICENSE

Le code de la passerelle Android est original. **Aucun interpréteur OpenJarvis, Ollama, serveur Python ou moteur Rust OpenJarvis n'est incorporé dans l'APK** ; la passerelle utilise son API officielle `POST /v1/chat/completions`. Ce n'est donc pas une installation du programme Linux sur le smartphone.

## 1. Installer OpenJarvis sur Manjaro

Ne lancez pas un script téléchargé depuis Internet sans l'examiner.

```bash
curl -fsSL https://open-jarvis.github.io/OpenJarvis/install.sh -o /tmp/openjarvis-install.sh
less /tmp/openjarvis-install.sh
bash /tmp/openjarvis-install.sh
```

L'installateur officiel installe l'environnement Python, les dépendances et éventuellement Ollama et un modèle. Lancez ensuite `jarvis doctor` pour contrôler l'installation. L'outil `jarvis` peut nécessiter l'ouverture d'un nouveau terminal après installation.

Pour activer le **serveur** (utilisez un agent simple sans outils dangereux pour commencer) :

```bash
read -rsp "Jeton privé pour OpenJarvis : " OPENJARVIS_API_KEY; echo
export OPENJARVIS_API_KEY
jarvis serve --host 127.0.0.1 --port 8000 --agent simple
```

Vérifiez sur **Manjaro** :

```bash
curl --fail http://127.0.0.1:8000/health
```

**Attention :** cette adresse `localhost` n'est accessible que depuis le PC. Si le téléphone Android utilise `localhost`, il tente de joindre le téléphone, pas Manjaro.

## 2. Exposer le service au téléphone sans affaiblir sa sécurité

Utilisez une **connexion HTTPS valide et authentifiée**, de préférence via un tunnel/VPN personnel et un reverse proxy TLS (Tailscale Serve ou Caddy/Nginx correctement configuré). L'adresse saisie dans l'application doit ressembler à `https://jarvis.mon-domaine.fr` ou à une adresse HTTPS privée accessible au téléphone.

- N'ouvrez **pas** le port HTTP 8000 sur Internet sans protection.
- Ne désactivez **pas** la vérification du certificat TLS Android.
- Le serveur doit appliquer `OPENJARVIS_API_KEY` à ses appels HTTP. Dans FRIDAY, le jeton est conservé chiffré grâce à Android Keystore.
- Les modèles OpenJarvis/Ollama tournent sur le PC et peuvent demander des ressources importantes. Commencez avec un modèle léger installé sur cet ordinateur.
- Les agents `orchestrator`, `react` et `openhands` peuvent faire appel à des outils du PC si vous leur en donnez les droits ; préférez `--agent simple` tant que vous n'avez pas défini des permissions et une isolation adaptées.

## 3. Configurer FRIDAY

Dans l'APK FRIDAY v3.9.0, ouvrez **Réglages → OPENJARVIS**.

1. Activez **Autoriser les questions déléguées à mon serveur OpenJarvis**.
2. Indiquez le nom DNS **HTTPS** du serveur accessible depuis le téléphone (sans `/v1/chat/completions`).
3. Indiquez le **modèle** tel qu'il est installé sur le PC, par exemple `qwen3.5:2b` si cette version existe effectivement sur l'instance.
4. Collez le jeton OpenJarvis défini sur le PC ; il ne sera pas affiché en clair après sauvegarde.
5. Appuyez sur **Enregistrer la connexion OpenJarvis**, puis **Tester mon serveur OpenJarvis**. Le test réalise **une vraie génération de quelques mots**, pas seulement un ping.
6. Par défaut, la seconde case « Envoyer les questions générales à OpenJarvis » reste décochée. Dites **« Friday, Jarvis, explique-moi ... »** pour déléguer une demande. Cochez la seconde case **uniquement si vous voulez explicitement utiliser OpenJarvis pour toutes les questions générales**.

L'état et les erreurs s'affichent sous le bouton de test, de manière persistante. Si OpenJarvis est désactivé ou inaccessible, le moteur habituel **Mistral** reste disponible pour les questions ordinaires ; une question destinée explicitement à Jarvis signale l'échec, elle n'est pas envoyée à un autre fournisseur de façon cachée.

## 4. Séparation des données

| Information | Mistral (moteur normal) | OpenJarvis (si activé) |
|---|---|---|
| Question actuelle | Oui, si Mistral sollicité | Oui, pour les questions déléguées |
| Historique privé du dialogue | Selon les règles FRIDAY existantes | **Non, jamais ajouté automatiquement** |
| Mémoire locale Memory+ | Consentement Mistral selon les réglages | **Non, jamais ajoutée automatiquement** |
| Position GPS | Uniquement avec l'autorisation ponctuelle existante | **Non, jamais ajoutée automatiquement** |
| Clé API | Clé Mistral dans Android Keystore | Jeton OpenJarvis séparé dans Android Keystore |

Le client OpenJarvis refuse les URL HTTP, les URL avec identifiants en clair, les redirections et les adresses `localhost`. Il ne contourne pas TLS et ne fait **aucune recherche, découverte ou synchronisation automatique en arrière-plan**.

## Limites & validation

Les tests automatiques vérifient le routage, la syntaxe et les protections ; GitHub Actions compile l'APK Android. Les tests de bout en bout exigent toutefois un vrai serveur OpenJarvis configuré par l'utilisateur et accessible en HTTPS, ce qui n'est pas disponible au moment de l'intégration. La fonctionnalité est **nouvelle et facultative** ; elle ne garantit pas les performances acoustiques du téléphone ni les outils exacts présents sur le PC.
