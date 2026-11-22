"""Régression v3.14.4 : récupération de la page RSS et commandes de lecture."""
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest

ROOT=Path(__file__).resolve().parents[1]
JAVA=ROOT/"app/src/main/java/fr/friday/assistant"

class RssFullArticleReaderV3144(unittest.TestCase):
    def test_java_articles_and_voice_commands(self):
        if not shutil.which("javac") or not shutil.which("java"):
            self.skipTest("JDK absent")
        smoke=r'''
import fr.friday.assistant.RssArticleIntent;
import fr.friday.assistant.RssArticleText;
public class RssReadSmoke {
    static void yes(boolean v,String reason){if(!v)throw new AssertionError(reason);}
    public static void main(String[] args) {
        yes(RssArticleIntent.isRequest("Jarvis, lis l'article RSS numéro 2 en entier"),"voice command");
        yes(RssArticleIntent.index("lis l'article numéro 2")==2,"indexed article");
        yes(RssArticleIntent.index("lis le troisième article")==3,"ordinal");
        yes(RssArticleIntent.index("ouvre l'article 9")==-1,"invalid index");
        yes(!RssArticleIntent.isRequest("explique l'article 73 du code pénal"),"law question isolation");
        yes(!RssArticleIntent.isRequest("les actualités mondiales aujourd'hui"),"news discovery unchanged");
        String html="<html><body><nav>Menu publicité</nav><article><h1>Une nouvelle étude</h1>"
            +"<p>Cette étude apporte des résultats détaillés aux lecteurs et "
            +"présente la méthodologie employée lors de la recherche scientifique.</p>"
            +"<p>Le second paragraphe détaille les limites, les sources, la date "
            +"et les perspectives possibles de cette publication, avec des données "
            +"destinées au public et un texte substantiel pour le corps de page.</p>"
            +"<aside>Publicité privée</aside></article><footer>Abonnez-vous</footer></body></html>";
        RssArticleText.Extracted article=RssArticleText.extract(html);
        yes(article.detected,"article found");
        yes(article.text.contains("second paragraphe"),"full article body");
        yes(!article.text.contains("Menu publicité"),"nav removed");
        yes(!article.text.contains("Publicité privée"),"aside removed");
        yes(!RssArticleText.extract("<html><body>Brève.</body></html>").detected,"unknown full-page status");
    }
}
'''
        with tempfile.TemporaryDirectory() as folder:
            base=Path(folder)
            (base/"RssReadSmoke.java").write_text(smoke,encoding="utf-8")
            subprocess.run(["javac","-encoding","UTF-8","-d",folder,
                str(JAVA/"VoiceText.java"),str(JAVA/"WebPageReader.java"),
                str(JAVA/"RssArticleIntent.java"),str(JAVA/"RssArticleText.java"),
                str(base/"RssReadSmoke.java")],check=True,capture_output=True,text=True)
            run=subprocess.run(["java","-cp",folder,"RssReadSmoke"],
                capture_output=True,text=True)
            self.assertEqual(run.returncode,0,run.stdout+"\n"+run.stderr)

    def test_web_content_and_version(self):
        reader=(JAVA/"WebPageReader.java").read_text(encoding="utf-8")
        internet=(JAVA/"JarvisInternet.java").read_text(encoding="utf-8")
        engine=(JAVA/"LocalJarvisEngine.java").read_text(encoding="utf-8")
        ui=(ROOT/"app/src/main/assets/index.html").read_text(encoding="utf-8")
        gradle=(ROOT/"app/build.gradle").read_text(encoding="utf-8")
        self.assertIn("public static Page readArticle(",reader)
        self.assertIn("WebPageReader.readArticle(",internet)
        self.assertIn("rss_last_articles_v3144",internet)
        self.assertIn("RssArticleIntent.isRequest(",internet)
        self.assertIn("[TRADUCTION_ARTICLE_PUBLIC]",engine)
        self.assertIn("Lis l’article RSS 1",ui)
        self.assertIn("versionName '3.14.18'",gradle)
        self.assertIn("applicationIdSuffix '.readerfixpreview'",gradle)

if __name__=="__main__":unittest.main()
