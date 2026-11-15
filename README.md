# 🤖 F.R.I.D.A.Y. — Assistant personnel Android

**F.R.I.D.A.Y.** est un assistant vocal et conversationnel **100 % natif Android** : cockpit **HUD** holographique (WebView), backend **Python embarqué**, détection de mot-clé en **Rust**, reconnaissance vocale **hors-ligne (Vosk)** et IA **DeepSeek**.

> Sources **v3.2.2** — application en développement actif.
> APK **release signé** publié par la CI (voir [Releases](../../releases/latest)).

---

## ✨ Fonctionnalités (v3.2.2)

### 🎙️ Voix
- **Écoute interactive** : le microphone ne se coupe plus pendant le calcul DeepSeek ni pendant la lecture TTS (micro et traitement sur deux threads)
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
- **DeepSeek Chat Completions** (`https://api.deepseek.com/chat/completions`)
- **Recherche Web intelligente** : API DeepSeek Search pour les questions factuelles et les relances, URLs de sources ; DeepSeek Fetch pour lire un lien fourni dans la question. Les appels Web peuvent être facturés.
- **Réglage Web explicite** : case Recherche Web et bouton pour l'activer et tester réellement l'accès. En cas d'échec, l'application le signale.
- **Mémoire personnelle locale chiffrée** sur demande
- **10 profils de personnalité** (16 000 caractères), chiffrés via **Android Keystore**
- **Cerveau local Python** : heure, date, jour, année — **fonctionne sans réseau**

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
