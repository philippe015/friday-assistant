#!/usr/bin/env bash
set -Eeuo pipefail
cd "$(dirname "$0")/.."
python3 -m compileall -q app/src/main/python
python3 -m unittest discover -s tests -v
bash -n scripts/compiler_manjaro.sh scripts/diagnostic_manjaro.sh scripts/installer_android.sh
if command -v node >/dev/null; then
  python3 - <<'PY'
from pathlib import Path
html=Path('app/src/main/assets/index.html').read_text()
assert html.count('<script>')==1 and html.count('</script>')==1
Path('/tmp/friday216_inline.js').write_text(html.split('<script>',1)[1].split('</script>',1)[0])
PY
  node --check /tmp/friday216_inline.js
  rm -f /tmp/friday216_inline.js
fi
if command -v cargo >/dev/null; then (cd native && cargo test); fi
TMP_TEST="$(mktemp -d)"
trap 'rm -rf "$TMP_TEST"' EXIT
javac -encoding UTF-8 -d "$TMP_TEST" \
  app/src/main/java/fr/friday/assistant/VoiceText.java \
  app/src/main/java/fr/friday/assistant/HttpRetryPolicy.java \
  app/src/main/java/fr/friday/assistant/SpeechSentenceBuffer.java \
  app/src/main/java/fr/friday/assistant/VoiceVocabulary.java \
  app/src/main/java/fr/friday/assistant/AssistantIdentity.java \
  app/src/main/java/fr/friday/assistant/WebSearch.java \
  app/src/main/java/fr/friday/assistant/WebToolPolicy.java \
  app/src/main/java/fr/friday/assistant/WebPageReader.java \
  app/src/main/java/fr/friday/assistant/BargeInPolicy.java \
  app/src/main/java/fr/friday/assistant/VoiceStopSpotter.java \
  app/src/main/java/fr/friday/assistant/TtsEchoGate.java \
  app/src/main/java/fr/friday/assistant/MicSignalMeter.java \
  app/src/main/java/fr/friday/assistant/LocationIntent.java \
  app/src/main/java/fr/friday/assistant/MemoryIntent.java \
  app/src/main/java/fr/friday/assistant/MemoryRules.java \
  app/src/main/java/fr/friday/assistant/FridayVersionPolicy.java \
  tests/WebPageReaderTest.java tests/WebToolPolicyTest.java tests/MemoryRulesTest.java tests/MicSignalMeterTest.java tests/VoiceStopSpotterTest.java tests/TtsEchoGateTest.java tests/VoiceVocabularyTest.java tests/SpeechSentenceBufferTest.java tests/HttpRetryPolicyTest.java tests/AssistantIdentityTest.java tests/WebSearchTest.java tests/VoiceTextTest.java tests/BargeInPolicyTest.java tests/LocationIntentTest.java tests/MemoryIntentTest.java tests/FridayVersionPolicyTest.java
for name in fr.friday.assistant.WebPageReaderTest WebToolPolicyTest MemoryRulesTest MicSignalMeterTest VoiceStopSpotterTest TtsEchoGateTest VoiceVocabularyTest SpeechSentenceBufferTest HttpRetryPolicyTest AssistantIdentityTest WebSearchTest VoiceTextTest BargeInPolicyTest fr.friday.assistant.LocationIntentTest MemoryIntentTest fr.friday.assistant.FridayVersionPolicyTest; do java -cp "$TMP_TEST" "$name"; done
echo 'Tests F.R.I.D.A.Y. réussis (Java, Python, Rust, JS).'
