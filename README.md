## FRIDAY Memory+ v3.8.0

**La nouvelle mémoire est privée par défaut.** Elle conserve jusqu'à 200 notes catégorisées (600 caractères chacune) sur le téléphone, chiffrées via Android Keystore. Recherche, modification, suppression et marquage de relecture se font dans les Réglages, hors ligne.

La mémoire locale et son partage avec Mistral ont **deux autorisations indépendantes** : l'envoi des notes pertinentes à Mistral est désactivé par défaut. Aucune note, clé ou conversation n'est automatiquement synchronisée vers GitHub. L'historique vocal des 40 derniers échanges est lui aussi stocké sous chiffrement avec migration de l'ancienne version.

Une commande « cherche dans ma mémoire ... » fonctionne localement, tandis que « retiens que ... » mémorise uniquement à la demande. Les suppressions globales exigent une confirmation dans les Réglages. La mémoire ne remplace pas la consultation des sources : elle peut contenir des informations anciennes ou inexactes.

[Documentation de confidentialité Memory+](docs/PRIVACY_MEMORY_PLUS_V380.md)

## v3.7.3 — Répétition de STOP et diagnostic microphone

- Les répétitions « stop stop », « stop stop stop » et « arrête arrête » sont traitées comme une seule commande d'interruption pendant la parole, sans exiger « Friday ».
- La télémétrie d'interruption affiche le **niveau sonore PCM capté** (0–100), indicatif et local. Un niveau non nul peut aussi provenir de l'écho : il ne prouve pas que le mot « stop » a été reconnu.
- Si le bouton « Interrompre la réponse » fonctionne mais pas la commande orale, vérifier le réglage d'interruption, l'accès microphone Android et essayer un casque pour réduire l'écho.

## Correctif d'interruption v3.7.2

**Deux décodeurs vocaux** sont désormais utilisés pendant une réponse : le Vosk libre pour les questions et un décodeur Vosk limité aux commandes « stop », « arrête », « attends » et « j'ai une précision ». Le second reste inactif hors réponse pour limiter la consommation. Son état apparaît dans le cockpit sous **Interruption vocale**. Le bouton Interrompre fonctionne même si Android coupe le microphone.

Il s'agit d'une amélioration de reconnaissance nécessitant **des tests sur téléphone**, pas d'une garantie matérielle que la voix sera captée au haut-parleur.

# 🤖 F.R.I.D.A.Y. — Assistant personnel Android

**F.R.I.D.A.Y.** est un assistant vocal et conversationnel **application Android native** : cockpit **HUD** holographique (WebView), backend **Python embarqué**, détection de mot-clé en **Rust**, reconnaissance vocale **hors-ligne (Vosk)** et IA **Mistral**.

> Sources **v3.7.0** — application en développement actif.
> APK **release signé** publié par la CI (voir [Releases](../../releases/latest)).

---

## Feuille de route des prochaines versions

Le plan de développement validé est disponible dans **[docs/ROADMAP_MISTRAL.md](docs/ROADMAP_MISTRAL.md)** :

- **[v3.5.0 — Fiabilité Mistral, GPS et Web](https://github.com/philippe015/friday-assistant/issues/29)** : diagnostics, reprise réseau annulable, tests du profil actif, consentement GPS.
- **[v3.6.0 — Streaming et TTS progressif](https://github.com/philippe015/friday-assistant/issues/30)** : démarrage vocal accéléré, file de phrases et interruptions fiables.
- **[v3.7.0 — Reconnaissance vocale avancée](https://github.com/philippe015/friday-assistant/issues/31)** : vocabulaire Vosk, options Voxtral et consommation batterie.

Ces fonctions sont **prévues**, pas encore livrées. L'IA reste **Mistral uniquement** ; le profil actif définit la personnalité et le GPS ne peut être transmis à Mistral qu'avec l'accord ponctuel de l'utilisateur.

---

## Correctifs d'interruption v3.7.1

- « Stop », « Arrête », « Silence », « Pause » ou « Attends » peuvent interrompre une réponse **sans répéter le mot Friday**, uniquement pendant que FRIDAY répond.
- « J'ai une précision », avec ou sans « Friday », coupe la réponse et garde le micro ouvert pour que l'utilisateur précise sa demande.
- Le TTS Android est interrompu **avant** d'attendre la déconnexion réseau Mistral ; son démarrage est synchronisé avec l'ordre d'arrêt.
- Dans le mode « une question », le micro reste ouvert pendant la réponse pour permettre les interruptions puis s'arrête après la fin de la réponse. La compréhension des commandes dépend toutefois de la capture audio et de l'anti-écho réellement disponibles sur l'appareil.

## Nouveautés v3.5–v3.7
- v3.5.0 : fiabilité réseau et modèles Mistral.
- v3.6.0 : streaming SSE et TTS progressif.
- v3.7.0 : Vosk spécialisé facultatif et écoute une question.

## ✨ Fonctionnalités (v3.4.1)

### 🎙️ Voix
- **Écoute interactive** : le microphone ne se coupe plus pendant le calcul Mistral ni pendant la lecture TTS (micro et traitement sur deux threads)
- **Interruption par la voix** :
  - « Friday stop » / « Friday attends » → coupe **l'audio seul**, le micro reste actif
  - « Friday, j'ai une précision » → coupe la réponse et ouvre une courte fenêtre d'écoute
  - « Friday, explique autrement » → interrompt et relance une nouvelle réponse
  - « Friday, arrête l'écoute » → arrête complètement le service
- **Mot-clé « Friday »** détecté par une bibliothèque **Rust** (`native/`, JNI)
- **Vosk** pour la reconnaissance française **hors-ligne**
- **TTS Android** pour la restitution
- Anti-écho : `VOICE_COMMUNICATION` + `AcousticEchoCanceler` / `NoiseSuppressor` (selon matériel)

### 🧠 Intelligence
- **Mistral Chat Completions** (`https://api.mistral.ai/v1/chat/completions`)
- **Recherche Web intelligente** : Mistral Conversations et l'outil intégré `web_search` pour les questions factuelles, les relances et la consultation des liens publics. Les sources sont affichées uniquement lorsqu'elles sont réellement fournies par l'API. Les appels Web peuvent être facturés.
- **Réglage Web explicite** : case Recherche Web et bouton pour l'activer et tester réellement l'accès. En cas d'échec, l'application le signale.
- **Mémoire personnelle locale chiffrée** sur demande
- **10 profils de personnalité** (16 000 caractères), chiffrés via **Android Keystore**
- **Cerveau local Python** : heure, date, jour, année — **fonctionne sans réseau**

### Correctifs v3.4.1
- **Identité du profil actif** : FRIDAY répond au nom choisi ; « Mistral » désigne le moteur de génération, pas l'assistant.
- **GPS Mistral avec consentement** : l'activation Android du GPS est indépendante du partage de coordonnées à Mistral. Activez le partage ponctuel depuis le cockpit et redémarrez l'écoute si le GPS a été activé après le démarrage du service.
- **Transparence** : indicateur « GPS vers Mistral » et dernier envoi API confirmé, sans coordonnées persistantes.
- **Tests à faire sur appareil** : identité, coordonnées, recherche d'informations locales via Mistral Web.

### Fonctionnement amélioré (v3.4.0)
- **Personnalité active** : le prompt sauvegardé du profil choisi est appliqué à chaque réponse Mistral, que la recherche Web soit active ou non ; le cockpit affiche le nom de ce profil.
- **Diagnostic Web** : état réel de l'option et du test de l'outil, sans confondre « activé » et « testé ».
- **GPS** : questions géolocalisées avec permission Android et diagnostic de la précision ; accord distinct pour un partage ponctuel à Mistral.
- **Interruption et précision** : « Friday, attends, j'ai une précision » stoppe la réponse mais conserve le sujet pour la question suivante.

### 🖥️ Interface
- Cockpit HUD holographique (HTML/CSS/JS local dans un WebView)
- Journal vocal local, sessions, diagnostics intégrés (Internet, clé, micro)
- Bouton **« Interrompre la réponse »** dans le cockpit **et** dans la notification
- Écoute possible **écran verrouillé** (service de premier plan)

## 🏗️ Architecture

```
app/src/main/java/fr/friday/assistant/   (24 classes Java)
├── MainActivity.java          (58 Ko)  activité principale + WebView
├── FridayService.java         (39 Ko)  service de premier plan (micro, TTS, HTTP)
├── NativeAI.java              orchestration DeepSeek (annulation, tokens)
├── VoiceSampleRecorder.java            capture de l'échantillon vocal
├── BargeInPolicy.java                  règles stop / précision / autrement
├── FridayLocation.java                 GPS + précision
├── AndroidPlaceResolver.java           résolution de lieux
├── NearbyIntent.java / LocationIntent.java
├── PersonalMemory.java / MemoryIntent.java
├── PersonalityProfiles.java            profils chiffrés
├── SecurePrefs.java                    stockage chiffré (Keystore)
├── ModelInstaller.java                 installation du modèle Vosk
├── RustWake.java                       pont JNI → mot-clé Rust
├── WebSearch.java                     recherche web et relances
├── FridayVersionGuard.java / FridayVersionPolicy.java
└── ...

app/src/main/python/friday_logic.py     logique locale (heure / date)
native/src/lib.rs + Cargo.toml          détection de mot-clé (Rust, JNI)
app/src/main/assets/index.html          cockpit HUD
tests/                                  tests Java + Python
scripts/                                build, diagnostic, installation
```

### Chaîne d'exécution

```
Micro ─► Vosk / Rust (mot-clé) ─► FridayService ─► NativeAI ─► API DeepSeek
                                        │                        │
                                        ▼                        ▼
                                   TTS Android  ◄──── réponse ──┘
                                        │
WebView (cockpit HUD) ◄── état/événements ── FridayService
```

## 📥 Installation

1. Télécharger l'APK depuis **[Releases](../../releases/latest)**
2. Autoriser l'installation de sources inconnues
3. Lancer l'app → **Réglages natifs** → bouton **« Se connecter / créer ma clé »** (page DeepSeek) → coller la clé → **Enregistrer**

**Pré-requis** : Android **8.0+** (API 26), appareil **arm64-v8a**.

> ⚠️ APK **debug** (`debuggable=true`, signature debug) : usage personnel / test.


**Depuis la v3.3.0 :** utiliser exclusivement une clé API Mistral. Les anciennes clés DeepSeek sont supprimées des préférences ; elles ne sont jamais envoyées à Mistral. Activez la recherche Web dans les réglages pour utiliser `web_search` avec Conversations. Cette fonction dépend de l'offre API et peut être facturée.

## 🛠️ Compiler depuis les sources

Voir **[BUILD.md](BUILD.md)**. En résumé :

| Outil | Version |
|---|---|
| JDK | 17 ou 21 |
| Python | **3.14** (Chaquopy `buildPython`) |
| Rust + cargo-ndk | stable |
| Android SDK | 35 + build-tools 35.0.0 |
| Android NDK | 27.2 ou 28.2 |
| Gradle | 8.9+ (AGP 8.7.3) |

```bash
bash tests/../scripts/tester_sources.sh   # tests Java + Python
bash scripts/compiler_manjaro.sh          # APK dans dist/
```

## 🔐 Sécurité & vie privée

| Point | État |
|---|---|
| Clés API | **chiffrées** via `SecurePrefs` / Android Keystore |
| Profils de personnalité | chiffrés |
| `usesCleartextTraffic` | `false` ✅ |
| `allowBackup` | `false` ✅ |
| Clés en dur dans le code | **aucune** ✅ |
| Micro / GPS | activés **volontairement**, service visible (notification) |
| Partage de position | **facultatif** et ponctuel |

## ⚠️ Limites connues

- Le téléphone peut entendre sa propre voix (l'anti-écho n'est pas une validation d'identité vocale) — un casque améliore nettement la fiabilité
- L'annulation d'une requête HTTP peut survenir **après** facturation côté fournisseur
- Le plein-duplex dépend des restrictions micro d'Android selon l'appareil
- **arm64-v8a uniquement** (pas d'émulateur x86)
- Le build exige **Python 3.14** et le **NDK** ; il n'est pas réalisable sur toutes les machines

## 🗺️ Feuille de route

- [x] APK **v2.2.0** compilé (debug) et publié
- [x] APK **release signé** par la CI (keystore en secret GitHub, `debuggable=false`)
- [ ] Workflow GitHub Actions (build reproductible)
- [ ] Historique persistant chiffré des conversations
- [ ] Thèmes du cockpit
- [ ] Variante `armeabi-v7a`

## 🛠️ Développement

| Ressource | Rôle |
|---|---|
| [CONTRIBUTING.md](CONTRIBUTING.md) | flux de contribution, conventions, processus de release |
| [CHANGELOG.md](CHANGELOG.md) | historique des versions |
| [BUILD.md](BUILD.md) | compilation (Linux x86_64, aarch64, CI) |
| [Tests](tests/) | tests Java, Python et Rust |

**Intégration continue** :

| Workflow | Déclencheur | Action |
|---|---|---|
| [`Tests`](.github/workflows/ci.yml) | pull request, push | tests Java / Python / Rust / syntaxe JS |
| [`Build APK`](.github/workflows/build.yml) | push, tag `v*` | APK debug + APK **release signé**, publication sur tag |

Dependabot surveille **Gradle**, **Cargo** et les **GitHub Actions**.

## 🔐 Signature de release

Les APK de release sont signés automatiquement par GitHub Actions avec un keystore dédié
(secrets : `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`).

```
Certificat : CN=Philippe, OU=FRIDAY, O=philippe015, L=Paris, C=FR
SHA-256    : 81becec7f794bb647cd5abb1db9d84f12d51c5c9b00f609bde52804c2930106e
```

> ⚠️ Sauvegardez le keystore : sans lui, aucune mise à jour de l'app ne pourra être signée avec la même identité.

## 📄 Licence

MIT — voir [LICENSE](LICENSE).

## 📚 Documentation complémentaire

- [ANALYSE.md](ANALYSE.md) — analyse statique de l'APK v2.0.2 (composants, permissions, libs, sécurité)
- [docs/README_FR.md](docs/README_FR.md) — documentation d'origine de l'auteur
- [docs/README_ECOUTE_INTERRUPTION_V220.md](docs/README_ECOUTE_INTERRUPTION_V220.md) — spécification de l'écoute interactive
- [docs/README_VOIX_CLONE_V219.md](docs/README_VOIX_CLONE_V219.md) — clonage vocal
- [docs/README_INTELLIGENCE_V216.md](docs/README_INTELLIGENCE_V216.md) — mémoire et analyse approfondie
