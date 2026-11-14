#!/usr/bin/env bash
set -Eeuo pipefail
printf '\n=== F.R.I.D.A.Y. v2.0 Android : diagnostic Manjaro ===\n'
for c in java javac python3.14 cargo rustup curl unzip; do
 if command -v "$c" >/dev/null; then printf 'OK   %-12s %s\n' "$c" "$(command -v "$c")"; else printf 'ABSENT %-12s\n' "$c"; fi
done
if [[ -z "${ANDROID_HOME:-}" ]]; then
 for candidate in "$HOME/Android/Sdk" "$HOME/Android/sdk" /opt/android-sdk /opt/android-sdk-linux; do
  if [[ -d "$candidate/platforms/android-35" ]]; then export ANDROID_HOME="$candidate"; break; fi
 done
fi
printf 'ANDROID_HOME=%s\n' "${ANDROID_HOME:-non défini}"
echo '--- Java disponible ---'
command -v archlinux-java >/dev/null && archlinux-java status || true
for jdk in /usr/lib/jvm/java-17-openjdk /usr/lib/jvm/java-21-openjdk; do
 if [[ -x "$jdk/bin/java" ]]; then "$jdk/bin/java" -version 2>&1 | head -n 1; fi
done

if [[ -n "${ANDROID_HOME:-}" ]]; then
 ls -d "$ANDROID_HOME/platforms/android-35" "$ANDROID_HOME/build-tools/35.0.0" 2>/dev/null || true
 ls -d "$ANDROID_HOME/ndk/"* 2>/dev/null || true
fi
