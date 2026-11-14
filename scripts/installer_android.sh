#!/usr/bin/env bash
set -Eeuo pipefail
cd "$(dirname "$0")/.."
APK="$PWD/dist/FRIDAY_v2.1.7_Android_Manjaro_Debug.apk"
[[ -s "$APK" ]] || { echo 'Compilez d’abord avec scripts/compiler_manjaro.sh'; exit 1; }
command -v adb >/dev/null || { echo 'Installer android-tools pour adb'; exit 1; }
adb devices
adb install -r "$APK"
