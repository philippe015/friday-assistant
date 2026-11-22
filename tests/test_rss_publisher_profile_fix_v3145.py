"""v3.14.5: refuse Google News JavaScript, distinguish reading and profile analysis."""
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest

ROOT=Path(__file__).resolve().parents[1]
JAVA=ROOT/"app/src/main/java/fr/friday/assistant"

class PublisherArticleFixV3145(unittest.TestCase):
    def test_java_regressions_from_phone_diagnostic(self):
        if not shutil.which("javac") or not shutil.which("java"):
            self.skipTest("JDK non disponible")
        smoke=r'''
import fr.friday.assistant.VoiceText;
import fr.friday.assistant.RssArticleIntent;
import fr.friday.assistant.RssArticleText;
import fr.friday.assistant.RssPublisherArticlePolicy;

public class PublisherArticleSmoke {
    static void yes(boolean v,String msg){if(!v)throw new AssertionError(msg);}
    public static void main(String[] args) {
        String realDiagnostic="service lit les articles en entier et analyse avec le profit";
        yes(!RssArticleIntent.isRequest(realDiagnostic),
            "general capability question must never trigger article 1");
        yes(RssArticleIntent.isRequest("Jarvis, lis l'article RSS numero 2 en entier"),
            "read explicit article");
        yes(RssArticleIntent.index("Jarvis, lis l'article RSS numero 2 en entier")==2,
            "article 2");
        yes(!RssArticleIntent.wantsAnalysis("lis l'article 2"),"reading is not analyzing");
        yes(RssArticleIntent.isRequest("Jarvis, analyse l'article 1 avec mon profil"),
            "profile analysis request");
        yes(RssArticleIntent.wantsAnalysis("Jarvis, analyse l'article 1 avec mon profil"),
            "analysis mode");
        yes(!RssArticleIntent.isRequest("les articles RSS sont-ils lus en entier ?"),
            "general plural inquiry");
        yes(RssPublisherArticlePolicy.googleRssUrl(
            "https://news.google.com/rss/articles/CBMxyz?oc=5"),"Google RSS link");
        yes("ouest-france.fr".equals(RssPublisherArticlePolicy.publisherDomain(
            "Ouest-France","Soprano au Mans")),"publisher Ouest-France");
        yes("franceinfo.fr".equals(RssPublisherArticlePolicy.publisherDomain(
            "France 3 Regions","Actualites Grenoble")),"France 3");
        yes(RssPublisherArticlePolicy.publisherDomain("mystery","unknown").isEmpty(),
            "unknown publisher must fail closed");
        yes(RssPublisherArticlePolicy.publisherHost(
            "https://www.ouest-france.fr/culture/soprano", "ouest-france.fr"),
            "allow known publisher hostname");
        yes(!RssPublisherArticlePolicy.publisherHost(
            "https://news.google.com/rss/articles/fake", "ouest-france.fr"),
            "never treat Google as publisher");
        yes(!RssPublisherArticlePolicy.publisherHost(
            "https://ouest-france.fr.attacker.example/false", "ouest-france.fr"),
            "reject prefix domain spoofing");
        yes(RssPublisherArticlePolicy.matchingTitle(
            "Soprano à la rencontre de ses fans au stade Marie Marvingt du Mans - Ouest-France",
            "Soprano à la rencontre de ses fans au stade Marie Marvingt du Mans"),
            "same headline matching");
        yes(!RssPublisherArticlePolicy.matchingTitle(
            "Soprano à la rencontre de ses fans au Mans",
            "Manifestation du samedi à Grenoble, plusieurs milliers de personnes"),
            "reject unrelated article");
        String googleScript="\"use strict\";this.default_DotsSplashUi_desktop_ms="
            +"this.default_DotsSplashUi_desktop_ms||{};(function(_){var window=this;"
            +" /* Copyright The Closure Library Authors */";
        yes(RssArticleText.looksLikeExecutableCode(googleScript),
            "real Google News JS signature");
        yes(RssArticleText.extract(googleScript).text.isEmpty(),
            "Google JS cannot reach TTS");
        yes(!RssArticleText.looksLikeExecutableCode(
            "L'article de presse raconte la visite de Soprano au stade du Mans "
            +"et les témoignages de ses fans venus pour l'événement."),
            "legitimate prose kept");
    }
}
'''
        with tempfile.TemporaryDirectory() as tmp:
            file=Path(tmp)/"PublisherArticleSmoke.java"
            file.write_text(smoke,encoding="utf-8")
            args=["javac","-encoding","UTF-8","-d",tmp]+[
                str(JAVA/filename) for filename in (
                    "VoiceText.java","WebPageReader.java","RssArticleText.java",
                    "RssArticleIntent.java","RssPublisherArticlePolicy.java")
            ]+[str(file)]
            subprocess.run(args,check=True,capture_output=True,text=True)
            run=subprocess.run(["java","-cp",tmp,"PublisherArticleSmoke"],
                capture_output=True,text=True)
            self.assertEqual(run.returncode,0,run.stdout+"\n"+run.stderr)

    def test_source_linkage_and_profile_are_real(self):
        internet=(JAVA/"JarvisInternet.java").read_text(encoding="utf-8")
        web=(JAVA/"WebPageReader.java").read_text(encoding="utf-8")
        profiles=(JAVA/"LocalJarvisEngine.java").read_text(encoding="utf-8")
        gradle=(ROOT/"app/build.gradle").read_text(encoding="utf-8")
        html=(ROOT/"app/src/main/assets/index.html").read_text(encoding="utf-8")
        assert "resolvePublisherLink(" in internet
        assert "RssPublisherArticlePolicy.matchingTitle" in internet
        assert "RssPublisherArticlePolicy.publisherHost" in internet
        assert "RssPublisherArticlePolicy.googleRssUrl" in internet
        assert "analysePublicArticleWithProfile(" in internet
        assert "RssArticleIntent.wantsAnalysis(question)" in internet
        assert "PersonalityProfiles.activeName(settings)" in internet
        assert "PersonalityProfiles.activePrompt(cfg)" in profiles
        assert "RssArticleText.looksLikeExecutableCode(text)" in web
        assert "versionCode 108" in gradle and "versionName '3.14.14'" in gradle
        assert "applicationIdSuffix '.readerfixpreview'" in gradle
        assert "Analyse RSS 1 avec mon profil" in html
        assert "STOP vocal" in (JAVA/"FridayService.java").read_text(encoding="utf-8")

if __name__=="__main__":
    unittest.main()
