"""Régression : les questions d'actualité demandent des preuves récentes, pas des souvenirs du LLM."""
from pathlib import Path
import unittest

ROOT=Path(__file__).resolve().parents[1]
JAVA=ROOT/"app/src/main/java/fr/friday/assistant"
NEWS=(JAVA/"RecentNewsPolicy.java").read_text(encoding="utf-8")
WEB=(JAVA/"JarvisInternet.java").read_text(encoding="utf-8")
FRESH=(JAVA/"RssFreshness.java").read_text(encoding="utf-8")

class FreshNewsResearch(unittest.TestCase):
    def test_daily_news_is_not_an_evergreen_search(self):
        self.assertIn("RecentNewsPolicy.newsWindowHours(q)", WEB)
        self.assertIn("NEWS_BRIEF", NEWS)
        self.assertIn("return NEWS_BRIEF.matcher(fold(question)).find() ? 24 : 0", NEWS)
        self.assertIn('URLEncoder.encode(topic + " when:" + hours + "h"', WEB)
        self.assertIn('Période de sélection', WEB)

    def test_recent_news_rejects_undated_items(self):
        self.assertIn("RssFreshness.withinHours(published, now, recentHours)",WEB)
        self.assertIn("candidate.publishedAt <= 0", WEB)
        self.assertIn("RssFreshness.display(source.publishedAt)",WEB)
        self.assertIn("publishedAt >= now - hours * 3600_000L", FRESH)
        self.assertIn("Les titres les plus récents", WEB)

    def test_more_than_five_rss_entries_are_inspected(self):
        self.assertIn("MAX_RSS_ITEMS = 40", WEB)
        self.assertIn("sources.size() < MAX_RSS_ITEMS", WEB)
        self.assertIn("MAX_NEWS_BYTES = 600_000", WEB)

    def test_multiple_publishers_and_deduplication(self):
        self.assertIn("https://news.google.com/rss", WEB)
        self.assertIn("https://www.francetvinfo.fr/titres.rss", WEB)
        self.assertIn("https://www.service-public.gouv.fr/abonnements/rss/", WEB)
        self.assertIn("https://www.bing.com/search?q=", WEB)
        self.assertIn("mergeDated", WEB)
        self.assertIn("normTitle", WEB)
        self.assertIn('Média indiqué par le flux',WEB)
        self.assertIn("candidate.publishedAt <= 0", WEB)

    def test_no_remote_llm_or_automatic_private_data(self):
        self.assertIn("context, settings, prompt, privateDialogue, cancellation)",WEB)
        self.assertNotIn("NativeAI.answer(", WEB)
        self.assertNotIn("PersonalMemory.", WEB)
        self.assertNotIn("FridayLocation.", WEB)
        self.assertIn("Aucune mémoire privée n'a été envoyée", WEB)
        self.assertIn("n'invente", WEB)

if __name__=="__main__":unittest.main()
