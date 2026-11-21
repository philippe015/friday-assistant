"""FRIDAY/Jarvis 3.14 Intelligence+: offline memory, real weather, trusted news.

The pure Java decision rules are executed by the host JDK; Android integration
is checked statically. A phone test is still required for TTS/Vosk and models.
"""
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest

ROOT=Path(__file__).resolve().parents[1]
JAVA=ROOT/"app/src/main/java/fr/friday/assistant"
read=lambda f:(JAVA/f).read_text(encoding="utf-8")

class IntelligencePlusV314(unittest.TestCase):
    def test_memory_retrieval_stays_local_and_reuses_encrypted_history(self):
        history=read("DialogueContext.java")
        recall=read("ConversationRecallPolicy.java")
        self.assertIn("PrivateConversationHistory.read(context, prefs)",history)
        self.assertIn("ConversationRecallPolicy.score(",history)
        self.assertIn("MAX_RECALLED_TOPICS = 2",history)
        self.assertIn("RAPPEL THÉMATIQUE ANCIEN",history)
        self.assertNotIn("https:",recall)
        self.assertNotIn("HttpURLConnection",recall)

    def test_forecast_tool_honors_city_and_never_leaks_gps(self):
        weather=read("JarvisWeather.java")
        android_tools=read("JarvisAndroidTools.java")
        self.assertIn("JarvisWeather.answer(utterance",android_tools)
        self.assertIn("JarvisWeather.followUp(context, settings, utterance)",android_tools)
        self.assertIn("https://api.open-meteo.com/v1/forecast?",weather)
        self.assertIn("https://geocoding-api.open-meteo.com/v1/search?",weather)
        self.assertIn('if (!webEnabled)',weather)
        self.assertIn("connection.setInstanceFollowRedirects(false)",weather)
        self.assertIn("PrivateConversationHistory.read(context, prefs)",weather)
        self.assertNotIn("FridayLocation.locate",weather)
        self.assertNotIn("getLastKnownLocation",weather)

    def test_verified_news_excerpts_not_generated_by_model(self):
        news=read("JarvisInternet.java")
        self.assertIn("NewsEvidencePolicy.distinctRssExcerpt(",news)
        self.assertIn("Extrait du flux, non vérifié indépendamment",news)
        self.assertIn("RssFreshness.withinHours(source.publishedAt, now, hours)",news)
        self.assertNotIn("research(privateDialogue",news)

    def test_model_advice_only_and_vosk_unchanged(self):
        model=read("LocalModelAdvisor.java")
        prompts=read("LocalJarvisEngine.java")
        service=read("FridayService.java")
        self.assertIn("Qwen3 0.6B",model)
        self.assertIn("vrai .task MediaPipe",model)
        self.assertIn("aucun",model.lower())
        self.assertIn("Distingue ce qui est établi",prompts)
        self.assertIn("boolean blocked = TtsEchoGate.suppressGeneralAsr(",service)
        self.assertNotIn("boolean blocked = currentTurn != null",service)
        self.assertIn('settings.put("barge_in_enabled", "0")',service)

    def test_java_intent_recall_weather_followups_and_rss_evidence(self):
        if not shutil.which("javac") or not shutil.which("java"):
            self.skipTest("JDK unavailable")
        smoke=r"""
import fr.friday.assistant.ConversationRecallPolicy;
import fr.friday.assistant.WeatherQueryPolicy;
import fr.friday.assistant.WeatherFollowUpPolicy;
import fr.friday.assistant.NewsEvidencePolicy;
public final class IntelligencePlusSmoke {
    static void yes(boolean ok, String why) {
        if(!ok) throw new AssertionError(why);
    }
    static boolean eq(String a,String b) { return a == null ? b == null : a.equals(b); }
    public static void main(String[] args) {
        yes(ConversationRecallPolicy.score(
            "que sais-tu des drones et du cargo russe",
            "le cargo russe attaqué par un drone")>=2,
            "must retrieve an older on-topic question");
        yes(ConversationRecallPolicy.score(
            "météo pour Lyon", "candidats à une présidentielle")==0,
            "unrelated old topic must not be recalled");
        yes(!ConversationRecallPolicy.usableAnswer(
            "Recherche Internet non aboutie : aucune source"),
            "error must not become a remembered fact");
        yes(eq(WeatherQueryPolicy.city("Quelle est la météo à Paris ?"),"paris"),
            "French city");
        yes(eq(WeatherQueryPolicy.city("Quel temps fait-il à Lyon demain ?"),"lyon"),
            "tomorrow French city");
        yes(eq(WeatherQueryPolicy.city("météo pour Paris, France"),"paris, france"),
            "city + country");
        yes(eq(WeatherQueryPolicy.city("météo aujourd'hui"),""),
            "city must be requested, never guessed");
        yes(WeatherQueryPolicy.city("parlons de mémoire conversationnelle")==null,
            "only explicit weather questions");
        String forecast="Prévisions météo pour Paris, France, aujourd'hui (2026-11-21) : "
          + "ciel dégagé. Source : Open-Meteo (prévision météorologique).";
        yes(eq(WeatherFollowUpPolicy.enrich("et demain ?",forecast),
            "météo demain pour Paris, France"),"carry public city into tomorrow");
        yes(eq(WeatherFollowUpPolicy.enrich("et pour Lyon ?",forecast),
            "météo pour lyon"),"new public city");
        yes(eq(WeatherFollowUpPolicy.enrich("pour Marseille",
            "Pour quelle ville souhaitez-vous la météo ? Dites un nom."),
            "météo pour marseille"),"answer to city clarification");
        yes(WeatherFollowUpPolicy.enrich("et demain ?",
            "Voici une note de santé personnelle") == null,
            "private text must not become a remote query");
        yes(NewsEvidencePolicy.distinctRssExcerpt(
            "Attaque de drone", "Texte très long qui explique des faits divers "
             + "sans apporter de nouvelles données vérifiées.", 
            "https://news.google.com/rss/articles/example").isEmpty(),
            "aggregated news summary untrusted");
        yes(!NewsEvidencePolicy.distinctRssExcerpt(
            "Titre différent", "Un résumé spécifique et détaillé fourni "
             + "par le flux du journal sur la situation au port.",
            "https://www.francetvinfo.fr/article").isEmpty(),
            "distinct publisher excerpt");
    }
}
"""
        with tempfile.TemporaryDirectory() as folder:
            path=Path(folder)
            (path/"IntelligencePlusSmoke.java").write_text(smoke,encoding="utf-8")
            sources=[
                "ConversationRecallPolicy.java","WeatherQueryPolicy.java",
                "WeatherFollowUpPolicy.java","NewsEvidencePolicy.java"]
            subprocess.run(["javac","-encoding","UTF-8","-d",folder,
                *[str(JAVA/s) for s in sources],
                str(path/"IntelligencePlusSmoke.java")],
                check=True,text=True,capture_output=True)
            subprocess.run(["java","-cp",folder,"IntelligencePlusSmoke"],
                check=True,text=True,capture_output=True)

if __name__=="__main__":unittest.main()
