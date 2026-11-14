# 🤖 F.R.I.D.A.Y. — Assistant personnel Android

**F.R.I.D.A.Y.** est un assistant vocal et conversationnel **100 % natif Android**, présenté sous la forme d'un **cockpit HUD** futuriste (WebView embarquée) piloté par un backend **Python** et enrichi par plusieurs fournisseurs d'IA.

> Projet en développement actif — version **2.0.2** (`fr.friday.assistant.preview`).

---

## ✨ Fonctionnalités

| Domaine | Détail |
|---|---|
| 🗣️ **Reconnaissance vocale** | Hors-ligne via **Vosk**, mot-clé d'activation « Friday » |
| 🔊 **Synthèse vocale** | Service **TTS** Android, activation volontaire du microphone |
| 💬 **Chat IA** | Intégration **Mistral** (`chat/completions`, `conversations`) |
| 🖼️ **Génération d'images** | API **Agnès** (`/v1/images/generations`) |
| 🧠 **Cerveau local** | Module Python embarqué (heure, date, jour, année — hors connexion) |
| 📍 **Localisation** | Position du téléphone via `FridayLocation` (foreground service) |
| 🎛️ **HUD / centre de commande** | Interface WebView : horloge, journal, sessions, profils |
| 🧩 **Personnalité** | Éditeur de directives, profils sauvegardés, export/import **JSON** |
| 🔌 **Diagnostic** | Tests intégrés : Internet, clé Mistral, clé Agnès, microphone |
| 💾 **Sauvegarde** | Export / import complet de la configuration et des profils |

## 🏗️ Architecture

```
┌──────────────────────────────────────────────────────┐
│                    APK (arm64-v8a)                   │
│                                                      │
│  MainActivity  ──► WebView ──► assets/index.html      │
│       │                         (HUD + personality)  │
│       │                              │               │
│       │                              ▼               │
│       │                     HTTP 127.0.0.1:8899      │
│       ▼                                              │
│  FridayService (foreground)                          │
│       ├── FridayLocation  (GPS)                      │
│       ├── ModelInstaller  (modèles Vosk)             │
│       └── TTS Android                                │
│                                                      │
│  Chaquopy ── Python 3.14 ── friday_logic.py          │
│  Vosk ── libvosk.so (10 Mo) ── reconnaissance offline│
│  JNA / OpenSSL / SQLite                              │
└──────────────────────────────────────────────────────┘
```

### Couches

1. **UI** — WebView plein écran chargeant `assets/index.html` (109 Ko) + `personality_v202.css/js` ; l'interface dialogue avec un serveur local Python (`http://127.0.0.1:8899`).
2. **Service Android** — `FridayService` (service de premier plan) gère micro, localisation et durée de vie de l'assistant.
3. **Backend Python** — via **Chaquopy** (Python 3.14), module `friday_logic` compilé en `.pyc`.
4. **IA distante** — Mistral (texte) et Agnès (texte + images), clés saisies par l'utilisateur dans l'interface.

## 📥 Installation

1. Télécharger l'APK depuis **[Releases](../../releases/latest)**
2. Autoriser l'installation de sources inconnues
3. Installer puis lancer **F.R.I.D.A.Y.**
4. Dans l'app : renseigner les **clés API** (Mistral / Agnès) et **tester** chaque moteur depuis l'écran de diagnostic

**Pré-requis** : Android **8.0+** (API 26), appareil **arm64-v8a**.

> ⚠️ APK de **développement**, signé avec une clé *debug* (`debuggable=true`) : destiné aux tests, pas à une publication Play Store.

## 🔐 Permissions déclarées

| Permission | Pourquoi |
|---|---|
| `INTERNET` | Appels Mistral / Agnès |
| `RECORD_AUDIO` | Reconnaissance vocale |
| `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_MICROPHONE` | Écoute continue en service de premier plan |
| `FOREGROUND_SERVICE_LOCATION` + `ACCESS_FINE/COARSE_LOCATION` | Position du téléphone |
| `POST_NOTIFICATIONS` | Notification du service |
| `WAKE_LOCK` | Maintien de l'écoute |

Points positifs : `allowBackup=false`, `usesCleartextTraffic=false`, **aucune clé API en clair** dans les ressources.

## 🗺️ Feuille de route

- [ ] Extraction des sources dans le dépôt (build reproductible)
- [ ] Sélection du modèle Vosk depuis l'interface
- [ ] Historique persistant des conversations
- [ ] Thèmes visuels du HUD
- [ ] Build CI (GitHub Actions) + APK signé release

## ⚠️ Limites connues

- Dépôt **documentaire** : le code source n'est pas encore publié ici
- Uniquement **arm64-v8a** (pas d'émulateur x86)
- Les clés API sont stockées côté application (WebView) — à protéger
- Dépend de services tiers (Mistral, Agnès) : quotas et disponibilité externes

## 📄 Licence

MIT — voir [LICENSE](LICENSE).

---

*Analyse technique détaillée : [ANALYSE.md](ANALYSE.md)*
