# 🛠️ Compiler F.R.I.D.A.Y. depuis les sources

## Pré-requis

| Outil | Version minimale | Rôle |
|---|---|---|
| **JDK** | 17 ou 21 | compilation Android (AGP 8.7.3) |
| **Python** | **3.14** | requis par Chaquopy (`buildPython('python3.14')`) |
| **Rust** | stable + `cargo-ndk` | bibliothèque de détection de mot-clé (`native/`) |
| **Android SDK** | Platform 35 | `ANDROID_HOME` |
| **Build-Tools** | 35.0.0 | aapt2, d8, apksigner |
| **Android NDK** | 27.2 ou 28.2 | cross-compilation Rust |
| **Gradle** | 8.9+ | wrapper ou installation système |

## Compilation

```bash
git clone https://github.com/philippe015/friday-assistant.git
cd friday-assistant

export ANDROID_HOME="$HOME/Android/Sdk"      # adapter
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk

bash scripts/tester_sources.sh               # tests Java + Python
bash scripts/compiler_manjaro.sh             # build complet
```

Résultat attendu :

```
BUILD SUCCESSFUL
dist/FRIDAY_v2.2.0_Android_Manjaro_Debug.apk
```

Installation :

```bash
adb install -r dist/FRIDAY_v2.2.0_Android_Manjaro_Debug.apk
```

## Détail des scripts

| Script | Rôle |
|---|---|
| `scripts/compiler_manjaro.sh` | vérifie les pré-requis, installe Gradle/cargo-ndk si besoin, compile Rust puis Gradle |
| `scripts/tester_sources.sh` | compile et exécute les tests Java (`tests/`) et Python |
| `scripts/diagnostic_manjaro.sh` | diagnostic de l'environnement |
| `scripts/installer_android.sh` | installation via adb |

## Configuration

`app/build.gradle` :

```groovy
chaquopy {
    defaultConfig {
        version = '3.14'
        buildPython('python3.14')
    }
}
dependencies {
    implementation 'com.alphacephei:vosk-android:0.3.75'
}
```

> `buildPython` doit pointer vers un interpréteur **exactement** en version 3.14. Sur Ubuntu/Debian, `python3.14` provient généralement du PPA **deadsnakes** ou d'une compilation manuelle.

## Notes et dépannage

- **`python3.14` introuvable** → Chaquopy refuse le build. Installer Python 3.14 (deadsnakes, pyenv, compilation source) et s'assurer que la commande `python3.14` est dans le `PATH`.
- **NDK absent** → `sdkmanager "ndk;27.2.12479018"`.
- **Rust** → `rustup target add aarch64-linux-android` puis `cargo install cargo-ndk`.
- **Machines ARM (aarch64)** : les binaires `aapt2` et les toolchains NDK fournis par Google sont **x86_64**. Il faut soit émuler (`qemu-user-static` + wrapper `aapt2` via `android.aapt2FromMavenOverride`), soit compiler sur une machine x86_64 (ou via GitHub Actions).
- **Build reproductible** : committer les versions (Gradle 8.9+ / AGP 8.7.3 / Chaquopy 17.2.0) et éviter `local.properties`.

## Signature de release (à faire)

```bash
keytool -genkeypair -v -keystore friday-release.jks -alias friday \
        -keyalg RSA -keysize 4096 -validity 10000

# dans app/build.gradle :
# signingConfigs { release { storeFile file(...) ... } }
# buildTypes { release { signingConfig signingConfigs.release; minifyEnabled true } }
```

> ⚠️ Ne **jamais** committer le keystore ni les mots de passe : ils sont exclus par `.gitignore`.
