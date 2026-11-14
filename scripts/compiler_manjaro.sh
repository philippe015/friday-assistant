#!/usr/bin/env bash
set -Eeuo pipefail
cd "$(dirname "$0")/.."
ROOT="$PWD"
err(){ echo "ERREUR : $*" >&2; exit 1; }
echo '=== F.R.I.D.A.Y. v2.2.0 — Construction APK Android sur Manjaro ==='
for cmd in python3.14 cargo rustup curl unzip; do
  command -v "$cmd" >/dev/null || err "$cmd absent"
done
# AGP 8.7 / Gradle 8.9 : JDK 17 requis. Ne pas utiliser la JVM 11 système.
JDK_CHOISI=""
for candidat in /usr/lib/jvm/java-17-openjdk /usr/lib/jvm/java-21-openjdk "${JAVA_HOME:-}"; do
  [[ -n "$candidat" && -x "$candidat/bin/java" && -x "$candidat/bin/javac" ]] || continue
  version_java="$("$candidat/bin/java" -version 2>&1 | head -n 1)"
  if [[ "$version_java" =~ (17|21)\. ]]; then
    JDK_CHOISI="$candidat"
    break
  fi
done
[[ -n "$JDK_CHOISI" ]] || err 'Java 17/21 introuvable. Installer : sudo pacman -S --needed jdk17-openjdk'
export JAVA_HOME="$JDK_CHOISI"
export PATH="$JAVA_HOME/bin:$HOME/.cargo/bin:$PATH"
echo "JAVA_HOME=$JAVA_HOME"
java -version 2>&1
javac -version 2>&1
python3.14 --version
if [[ -z "${ANDROID_HOME:-}" ]]; then
 for candidate in "$HOME/Android/Sdk" "$HOME/Android/sdk" /opt/android-sdk /opt/android-sdk-linux; do
  if [[ -d "$candidate/platforms/android-35" ]]; then export ANDROID_HOME="$candidate"; break; fi
 done
fi
[[ -d "${ANDROID_HOME:-}/platforms/android-35" ]] || err 'Android SDK 35 introuvable, définir ANDROID_HOME.'
[[ -d "$ANDROID_HOME/build-tools/35.0.0" ]] || err 'Installer build-tools;35.0.0.'
export ANDROID_SDK_ROOT="$ANDROID_HOME"
# Réutiliser une version récente déjà présente (le NDK 27.2 est utilisable avec cargo-ndk).
NDK_VERSION=''
for candidat in 28.2.13676358 27.2.12479018; do
  if [[ -d "$ANDROID_HOME/ndk/$candidat/toolchains/llvm/prebuilt/linux-x86_64" ]]; then
    NDK_VERSION="$candidat"
    break
  fi
done
if [[ -z "$NDK_VERSION" ]]; then
  NDK_VERSION='28.2.13676358'
  MANAGER="$(find "$ANDROID_HOME/cmdline-tools" -type f -name sdkmanager -print -quit 2>/dev/null || true)"
  [[ -n "$MANAGER" ]] || err 'NDK absent et sdkmanager introuvable.'
  echo 'Installation NDK Android 28.2 avec Java compatible...'
  "$MANAGER" --sdk_root="$ANDROID_HOME" "ndk;$NDK_VERSION"
fi
export ANDROID_NDK_HOME="$ANDROID_HOME/ndk/$NDK_VERSION"
echo "Android NDK : $ANDROID_NDK_HOME"
command -v cargo-ndk >/dev/null || {
 echo 'Installation de cargo-ndk dans le compte utilisateur...'
 cargo install cargo-ndk --locked
}
if ! rustup target list --installed | grep -qx aarch64-linux-android; then rustup target add aarch64-linux-android; fi
mkdir -p "$ROOT/app/src/main/jniLibs"
echo '[1/3] Construction de la bibliothèque Rust JNI pour Android ARM64'
(cd "$ROOT/native" && cargo test && cargo ndk -t arm64-v8a -o "$ROOT/app/src/main/jniLibs" build --release)
SO="$ROOT/app/src/main/jniLibs/arm64-v8a/libfriday_core.so"
[[ -s "$SO" ]] || err 'Bibliothèque Rust JNI non produite.'
echo '[2/3] Gradle 8.9 et Python embarqué via Chaquopy'
mkdir -p "$ROOT/.tools" "$ROOT/dist"
GRADLE="$ROOT/.tools/gradle-8.9/bin/gradle"
if [[ ! -x "$GRADLE" ]]; then
  curl -fL --retry 3 https://services.gradle.org/distributions/gradle-8.9-bin.zip -o "$ROOT/.tools/gradle-8.9-bin.zip"
  unzip -qo "$ROOT/.tools/gradle-8.9-bin.zip" -d "$ROOT/.tools"
fi
set -o pipefail
"$GRADLE" --no-daemon --max-workers=2 --console=plain :app:assembleDebug 2>&1 | tee "$ROOT/dist/compilation_manjaro.log"
APK="$ROOT/app/build/outputs/apk/debug/app-debug.apk"
[[ -s "$APK" ]] || err 'Gradle terminé sans APK.'
DEST="$ROOT/dist/FRIDAY_v2.2.0_Android_Manjaro_Debug.apk"
cp -f "$APK" "$DEST"
sha256sum "$DEST" > "$DEST.sha256"
echo '[3/3] APK produite :'
ls -lh "$DEST"
echo 'Installation : adb install -r "$DEST"'
echo 'Cette variante DEBUG utilise un ID séparé (.preview) pour préserver la v1.x.'
