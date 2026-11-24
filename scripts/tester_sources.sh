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
  app/src/main/java/fr/friday/assistant/VoiceInputQualityPolicy.java \
  app/src/main/java/fr/friday/assistant/TvGuidePolicy.java \
  app/src/main/java/fr/friday/assistant/DialogueMemoryPolicy.java \
  app/src/main/java/fr/friday/assistant/ConversationTurnPolicy.java \
  app/src/main/java/fr/friday/assistant/DialogueTestPolicy.java \
  app/src/main/java/fr/friday/assistant/ConversationClarificationPolicy.java \
  app/src/main/java/fr/friday/assistant/ConversationRecallPolicy.java \
  app/src/main/java/fr/friday/assistant/VoiceUtteranceTiming.java \
  app/src/main/java/fr/friday/assistant/RssFreshness.java \
  app/src/main/java/fr/friday/assistant/HttpRetryPolicy.java \
  app/src/main/java/fr/friday/assistant/SpeechSentenceBuffer.java \
  app/src/main/java/fr/friday/assistant/VoiceVocabulary.java \
  app/src/main/java/fr/friday/assistant/AssistantIdentity.java \
  app/src/main/java/fr/friday/assistant/WebSearch.java \
  app/src/main/java/fr/friday/assistant/OpenJarvisPolicy.java \
  app/src/main/java/fr/friday/assistant/LocalJarvisPolicy.java \
  app/src/main/java/fr/friday/assistant/JarvisMistralRouting.java \
  app/src/main/java/fr/friday/assistant/LocalJarvisArithmetic.java \
  app/src/main/java/fr/friday/assistant/RecentNewsPolicy.java \
  app/src/main/java/fr/friday/assistant/WebToolPolicy.java \
  app/src/main/java/fr/friday/assistant/WebPageReader.java \
  app/src/main/java/fr/friday/assistant/RssArticleText.java \
  app/src/main/java/fr/friday/assistant/RssArticleIntent.java \
  app/src/main/java/fr/friday/assistant/DiagnosticLogPolicy.java \
  app/src/main/java/fr/friday/assistant/LiteRtPromptBudget.java \
  app/src/main/java/fr/friday/assistant/RssPublisherArticlePolicy.java \
  app/src/main/java/fr/friday/assistant/WebAnalysisPolicy.java \
  app/src/main/java/fr/friday/assistant/BargeInPolicy.java \
  app/src/main/java/fr/friday/assistant/VoiceStopSpotter.java \
  app/src/main/java/fr/friday/assistant/VoiceSelfEchoPolicy.java \
  app/src/main/java/fr/friday/assistant/TtsEchoGate.java \
  app/src/main/java/fr/friday/assistant/MicSignalMeter.java \
  app/src/main/java/fr/friday/assistant/LocationIntent.java \
  app/src/main/java/fr/friday/assistant/MemoryIntent.java \
  app/src/main/java/fr/friday/assistant/MemoryRules.java \
  app/src/main/java/fr/friday/assistant/FridayVersionPolicy.java \
  tests/WebAnalysisPolicyTest.java \
  tests/RssSourceEchoFixTest.java \
  tests/RssFollowupV31418Test.java \
  tests/RssSpeechRecoveryTest.java \
  tests/LiteRtPromptBudgetTest.java \
  tests/RssLivePublisherPolicyTest.java \
  tests/RssPublisherBodyMatchTest.java \
  tests/DiagnosticLogPolicyTest.java \
  tests/DialogueMemoryPolicyTest.java tests/ConversationClarificationPolicyTest.java tests/LocalJarvisPolicyTest.java tests/JarvisMistralRoutingTest.java tests/OpenJarvisPolicyTest.java tests/WebPageReaderTest.java tests/WebToolPolicyTest.java tests/MemoryRulesTest.java tests/MicSignalMeterTest.java tests/VoiceStopSpotterTest.java tests/TtsEchoGateTest.java tests/VoiceVocabularyTest.java tests/SpeechSentenceBufferTest.java tests/HttpRetryPolicyTest.java tests/AssistantIdentityTest.java tests/WebSearchTest.java tests/RecentNewsPolicyTest.java tests/VoiceTextTest.java tests/BargeInPolicyTest.java tests/LocationIntentTest.java tests/MemoryIntentTest.java tests/FridayVersionPolicyTest.java tests/VoiceUtteranceTimingTest.java tests/RssFreshnessTest.java
for name in fr.friday.assistant.DiagnosticLogPolicyTest fr.friday.assistant.RssPublisherBodyMatchTest fr.friday.assistant.RssLivePublisherPolicyTest fr.friday.assistant.LiteRtPromptBudgetTest fr.friday.assistant.RssSpeechRecoveryTest fr.friday.assistant.RssSourceEchoFixTest fr.friday.assistant.RssFollowupV31418Test fr.friday.assistant.WebAnalysisPolicyTest DialogueMemoryPolicyTest fr.friday.assistant.ConversationClarificationPolicyTest LocalJarvisPolicyTest JarvisMistralRoutingTest OpenJarvisPolicyTest fr.friday.assistant.WebPageReaderTest WebToolPolicyTest MemoryRulesTest MicSignalMeterTest VoiceStopSpotterTest TtsEchoGateTest VoiceVocabularyTest SpeechSentenceBufferTest HttpRetryPolicyTest AssistantIdentityTest WebSearchTest RecentNewsPolicyTest VoiceTextTest BargeInPolicyTest fr.friday.assistant.LocationIntentTest MemoryIntentTest fr.friday.assistant.FridayVersionPolicyTest VoiceUtteranceTimingTest RssFreshnessTest; do java -cp "$TMP_TEST" "$name"; done
echo 'Tests F.R.I.D.A.Y. réussis (Java, Python, Rust, JS).'
