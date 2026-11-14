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

---

## 🅰️ Compiler sur **aarch64** (Linux ARM / téléphone) — recette validée

Build **réussi de bout en bout** sur un conteneur aarch64. Les outils Google (aapt2, NDK) sont x86_64 : voici les contournements utilisés.

### 1. Python 3.14 (arm64)

```bash
sudo add-apt-repository ppa:deadsnakes/ppa
sudo apt-get update
sudo apt-get install -y python3.14 python3.14-dev python3.14-venv
```

> ⚠️ **Locale obligatoire** : Python 3.14 crée dans le venv un lien symbolique `𝜋thon` (easter egg).
> Avec une locale `POSIX`, Gradle échoue sur `Failed to create MD5 hash ... ????thon`.
> ```bash
> export LANG=C.UTF-8 LC_ALL=C.UTF-8
> export JAVA_TOOL_OPTIONS="-Dfile.encoding=UTF-8"
> ```

### 2. aapt2 x86_64 sous QEMU

```bash
sudo apt-get install -y qemu-user-static libc6:amd64 libstdc++6:amd64 zlib1g:amd64
mkdir -p /opt/android-sdk/aapt2-qemu
cat > /opt/android-sdk/aapt2-qemu/aapt2 <<'SH'
#!/bin/sh
exec /usr/bin/qemu-x86_64-static /opt/android-sdk/build-tools/35.0.0/aapt2 "$@"
SH
chmod +x /opt/android-sdk/aapt2-qemu/aapt2
```

### 3. Rust → Android (sans NDK exécutable)

Le clang du NDK est x86_64 et **ne peut pas s'auto-réexécuter** sous QEMU sans `binfmt_misc`.
Solution : lier avec **`rust-lld` fourni par Rust** (binaire natif aarch64) et les objets du NDK.

```bash
rustup target add aarch64-linux-android
RL=~/.rustup/toolchains/stable-aarch64-unknown-linux-gnu/lib/rustlib/aarch64-unknown-linux-gnu/bin/rust-lld
printf '#!/bin/sh\nexec %s -flavor gnu "$@"\n' "$RL" > /usr/local/bin/rust-ndk-ld
chmod +x /usr/local/bin/rust-ndk-ld

SYS=$ANDROID_HOME/ndk/27.2.12479018/toolchains/llvm/prebuilt/linux-x86_64/sysroot/usr/lib/aarch64-linux-android
CLC=$ANDROID_HOME/ndk/27.2.12479018/toolchains/llvm/prebuilt/linux-x86_64/lib/clang/18/lib/linux/aarch64

export CARGO_TARGET_AARCH64_LINUX_ANDROID_LINKER=/usr/local/bin/rust-ndk-ld
export RUSTFLAGS="-C link-arg=-L$SYS/26 -C link-arg=-L$SYS -C link-arg=-L$CLC \
  -C link-arg=$SYS/26/crtbegin_so.o -C link-arg=$SYS/26/crtend_so.o"

cd native && cargo build --release --target aarch64-linux-android
mkdir -p ../app/src/main/jniLibs/arm64-v8a
cp target/aarch64-linux-android/release/libfriday_core.so ../app/src/main/jniLibs/arm64-v8a/
```

### 4. Gradle

```bash
gradle --no-daemon -Pandroid.aapt2FromMavenOverride=/opt/android-sdk/aapt2-qemu/aapt2 :app:assembleDebug
```

Résultat obtenu : **BUILD SUCCESSFUL**, APK ≈ 22 Mo contenant `libfriday_core.so` (511 Ko) et `libvosk.so` (10 Mo).
