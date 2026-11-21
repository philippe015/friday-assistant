"""RSS monde : couverture multilingue, dates exactes et traductions locales FR."""
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest

ROOT=Path(__file__).resolve().parents[1]
JAVA=ROOT/"app/src/main/java/fr/friday/assistant"
def source(name):
    return (JAVA/name).read_text(encoding="utf-8")

class WorldwideRssFrenchRegression(unittest.TestCase):
    def test_local_translation_no_remote_api_or_private_history(self):
        web=source("JarvisInternet.java")
        ui=source("MainActivity.java")
        policy=source("WorldRssPolicy.java")
        self.assertIn("WorldRssPolicy.isWorldNews(publicNewsQuery)",web)
        self.assertIn("sources = worldRssSearch(publicNewsQuery, cancellation);",web)
        self.assertIn("LocalJarvisEngine.answer(context, settings,",web)
        self.assertIn('translationPrompt.toString(), "", cancellation)',web)
        self.assertIn("WorldRssTranslationPolicy.parse(generated)",web)
        self.assertIn("translation française indisponible",web)
        self.assertIn("Titre original",web) if False else None
        self.assertIn("titre original : ",web)
        self.assertIn("https://www.nhk.or.jp/rss/news/cat6.xml",policy)
        self.assertIn("https://feeds.bbci.co.uk/news/world/rss.xml",policy)
        self.assertIn("https://news.google.com/rss?hl=es&gl=ES",policy)
        self.assertIn("https://news.google.com/rss?hl=ar&gl=SA",policy)
        self.assertIn("jarvis_world_rss_translate_fr",ui)
        self.assertNotIn("research(privateDialogue",web)
        self.assertNotIn("LocalJarvisEngine.answer(context, settings,\n                        translationPrompt.toString(), privateDialogue",web)
        self.assertIn("            if (cancellation != null) cancellation.check();",web)

    def test_world_rss_datetime_and_urls_sanitized(self):
        src=source("JarvisInternet.java")
        self.assertIn("WebPageReader.checkedUri(url)",src)
        self.assertIn("fetch(feed.url, true)",src)
        self.assertIn('xmlField(item, "dc:date")',src)
        self.assertIn('xmlField(item, "pubDate")',src)
        self.assertIn("RssFreshness.withinHours",src)
        self.assertIn("MAX_WORLD_RESULTS = 7",src)
        self.assertIn("item.feedName = feed.name",src)
        self.assertIn("item.originalLanguage = feed.language",src)

    def test_smoke_java_locale_catalogue_translation_and_iso_dates(self):
        if not shutil.which("javac") or not shutil.which("java"):
            self.skipTest("JDK unavailable")
        smoke=r"""
import fr.friday.assistant.RecentNewsPolicy;
import fr.friday.assistant.WorldRssPolicy;
import fr.friday.assistant.WorldRssTranslationPolicy;
import fr.friday.assistant.RssFreshness;
import java.time.OffsetDateTime;
import java.util.Map;
public final class WorldwideRssSmoke {
    static void yes(boolean ok, String reason) {
        if (!ok) throw new AssertionError(reason);
    }
    public static void main(String[] args) {
        yes(WorldRssPolicy.CATALOGUE.size() >= 15,"international catalogue");
        yes(WorldRssPolicy.isWorldNews("Jarvis, donne les actualités mondiales"),
            "global news trigger");
        yes(WorldRssPolicy.isWorldNews("actualités du Japon aujourd'hui"),
            "specific country trigger");
        yes(!WorldRssPolicy.isWorldNews("Jarvis, la météo mondiale"),
            "no accidental weather routing");
        yes(!WorldRssPolicy.isWorldNews("actualités scientifiques du jour"),
            "scientific searches still specialized");
        yes(WorldRssPolicy.select("actualités mondiales").size() >= 6,
            "global cross-region selection");
        yes(WorldRssPolicy.select("actualités du Japon").get(0).language.equals("ja"),
            "Japanese native feed prioritized");
        yes(WorldRssPolicy.select("actualités du Brésil").get(0).language.equals("pt"),
            "Brazilian Portuguese provider");
        yes(WorldRssPolicy.select("actualités aux États-Unis").get(0).language.equals("en"),
            "US edition recognized");
        for (WorldRssPolicy.Feed feed : WorldRssPolicy.CATALOGUE) {
            yes(feed.url.startsWith("https://"),"HTTPS only");
            yes(!feed.name.isEmpty() && !feed.region.isEmpty(),"source attribution");
        }
        Map<String,String> tr=WorldRssTranslationPolicy.parse(
            "T1: Les nouvelles internationales\nR1: D'autres précisions\n"
            + "T2: La météo de Tokyo\nT3: https://unsafe.example/\n");
        yes("Les nouvelles internationales".equals(tr.get("T1")),"French title");
        yes("D'autres précisions".equals(tr.get("R1")),"French summary");
        yes(!tr.containsKey("T3"),"translation URL rejected");
        yes("Titre original".equals(WorldRssTranslationPolicy.apply(
            tr,"T7","Titre original")),"honest fallback");
        long iso=RssFreshness.publishedMillis("2026-11-21T17:00:00Z");
        long offset=RssFreshness.publishedMillis("2026-11-21T18:00:00+01:00");
        long rfc=RssFreshness.publishedMillis("Sat, 21 Nov 2026 17:00:00 GMT");
        yes(iso>0 && iso==offset && iso==rfc,"ISO and RFC timestamps");
        long now=OffsetDateTime.parse("2026-11-21T18:15:00Z").toInstant().toEpochMilli();
        yes(RssFreshness.withinHours(iso,now,2),"correct freshness");
        yes(!RssFreshness.withinHours(0,now,2),"missing date rejected");
    }
}
"""
        files=["RecentNewsPolicy.java","WorldRssPolicy.java",
               "WorldRssTranslationPolicy.java","RssFreshness.java"]
        with tempfile.TemporaryDirectory() as folder:
            path=Path(folder)
            (path/"WorldwideRssSmoke.java").write_text(smoke,encoding="utf-8")
            subprocess.run(["javac","-encoding","UTF-8","-d",folder,
                *[str(JAVA/f) for f in files],str(path/"WorldwideRssSmoke.java")],
                check=True,text=True,capture_output=True)
            subprocess.run(["java","-cp",folder,"WorldwideRssSmoke"],
                check=True,text=True,capture_output=True)

if __name__=="__main__": unittest.main()
