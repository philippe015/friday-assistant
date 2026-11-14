# 🔬 Analyse technique — F.R.I.D.A.Y. v2.0.2

> Analyse statique réalisée sur l'APK `FRIDAYv202.apk` (aucun code source fourni).

## 1. Identité

| Champ | Valeur |
|---|---|
| Package | `fr.friday.assistant.preview` |
| Nom affiché | `F.R.I.D.A.Y.` |
| versionName / versionCode | **2.0.2** / 22 |
| minSdk / targetSdk / compileSdk | 26 / 35 / 35 |
| Taille APK | **17 156 651 octets** (≈ 16,4 Mio) |
| ABI | **arm64-v8a uniquement** |
| Signature | **Android Debug** (`CN=Android Debug`) → `debuggable=true` |
| Empreinte SHA-256 (certificat) | `96598a1b0eac81f042a59bd97b62888c10c7fd607426cabee68f80a873a39657` |
| SHA-256 APK | `797c632090cedf2b85a9f778dc318f42fdea2825905dd1e6282b68daeb834e00` |

## 2. Composants déclarés

| Type | Nom | Exporté | Rôle |
|---|---|---|---|
| Application | `com.chaquo.python.android.PyApplication` | — | Initialisation de l'interpréteur Python |
| Activity | `fr.friday.assistant.MainActivity` | **oui** | Écran principal (LAUNCHER) |
| Service | `fr.friday.assistant.FridayService` | non | Service de premier plan (micro / localisation) |
| Intent filter | `android.intent.action.TTS_SERVICE` | — | Synthèse vocale |

Classes applicatives identifiées (hors bibliothèques) :

```
fr.friday.assistant.MainActivity
fr.friday.assistant.FridayService
fr.friday.assistant.FridayLocation      (+ Fix)
fr.friday.assistant.LocationIntent
fr.friday.assistant.ModelInstaller      (+ Progress)
fr.friday.assistant.R
```

**241 classes** au total dans les 3 fichiers DEX (le reste = Chaquopy, JNA, Vosk, AndroidX).

## 3. Bibliothèques natives (`lib/arm64-v8a/`)

| Fichier | Taille | Usage |
|---|---|---|
| `libvosk.so` | **10 042 800** | Reconnaissance vocale hors-ligne |
| `libpython3.14.so` | 5 835 936 | Interpréteur Python (Chaquopy) |
| `libcrypto_python.so` | 4 544 368 | OpenSSL pour Python |
| `libsqlite3_python.so` | 1 217 120 | SQLite pour Python |
| `libssl_python.so` | 963 112 | TLS |
| `libfriday_core.so` | 494 904 | **Bibliothèque métier propre au projet** |
| `libchaquopy_java.so` | 157 896 | Pont Java ↔ Python |
| `libjnidispatch.so` | 176 520 | JNA |
| *(+ 4 petits modules : sqlite3/java, ssl/java, crypto/java, python3)* | | |

## 4. Assets

| Asset | Taille | Rôle |
|---|---|---|
| `assets/index.html` | 109 816 | **Interface HUD** (WebView) |
| `assets/personality_v202.js` | 12 063 | Logique de l'interface |
| `assets/personality_v202.css` | 7 905 | Styles |
| `assets/chaquopy/app.imy` | 2 248 | Archive du code Python (`friday_logic.pyc`) |
| `assets/chaquopy/stdlib-*.imy` | 6,5 Mo | Bibliothèque standard Python |
| `assets/chaquopy/bootstrap.imy` | 627 119 | Bootstrap Chaquopy |
| `assets/chaquopy/cacert.pem` | 235 863 | Autorités de certification |

## 5. Backend Python embarqué

Module unique `friday_logic.py` (compilé en bytecode), exposant :

- `prepare_question(...)` — normalisation de la question (NFKD, suppression des accents, espaces, ponctuation)
- `local_answer(...)` — réponses **hors-ligne** pour l'heure et la date, avec `datetime`, fuseau horaire de l'appareil, jours et mois localisés en français
- `epoch_ms(...)` — conversion timestamps

> Intérêt : même sans réseau, F.R.I.D.A.Y. répond correctement à « quelle heure est-il ? », « quel jour sommes-nous ? », « en quelle année »…

## 6. Interface et intégrations réseau

- L'UI dialogue avec `http://127.0.0.1:8899` (serveur local Python)
- Endpoints externes détectés :
  - `https://api.mistral.ai/v1/chat/completions`
  - `https://api.mistral.ai/v1/conversations`
  - `https://apihub.agnes-ai.com/v1` (chat, images)
  - `https://apihub.agnes-ai.com/v1/chat/completions`
  - `https://apihub.agnes-ai.com/v1/images/generations`
- **Aucune clé API en clair** dans `index.html` / `personality_v202.js` ✅

## 7. Sécurité — constats

| Point | État | Commentaire |
|---|---|---|
| `usesCleartextTraffic` | `false` ✅ | Pas de HTTP en clair |
| `allowBackup` | `false` ✅ | Sauvegarde système désactivée |
| Clés API en dur | aucune ✅ | Saisies par l'utilisateur |
| `debuggable` | `true` ⚠️ | Build de développement : `run-as` et débogage possibles |
| Signature | debug ⚠️ | À remplacer par un keystore de release |
| `MainActivity` exportée | oui (normal) | Comportement attendu pour un LAUNCHER |
| Service exporté | non ✅ | Correct |
| Permissions | cohérentes avec les fonctions | Micro + localisation en service de premier plan |

## 8. Taille de l'application

| Poste | Poids approx. |
|---|---|
| Bibliothèques natives | ~24 Mo décompressés (~13 Mo compressés) |
| Bibliothèque standard Python | ~6,5 Mo |
| Interface HUD | ~130 Ko |
| Code DEX | quelques centaines de Ko |

Le poids est dominé par **Vosk (10 Mo)** et **Python 3.14 + stdlib** : c'est le prix de la reconnaissance vocale **hors-ligne** et d'un backend Python embarqué.

## 9. Recommandations

1. **Signer en release** et passer `debuggable=false` avant diffusion
2. Séparer les clés API dans Android Keystore plutôt que le stockage WebView
3. Envisager `arm64-v8a` + `armeabi-v7a` si des appareils plus anciens doivent être couverts
4. Ajouter un `READ_PHONE_STATE`-free fallback si Mistral/Agnès sont indisponibles (déjà partiellement géré par `local_answer`)
5. Publier les sources pour un build reproductible et une contribution externe
