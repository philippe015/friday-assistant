import sys, unittest
from pathlib import Path
sys.path.insert(0, str(Path(__file__).resolve().parents[1] / 'app/src/main/python'))
import friday_logic
class TestLogic(unittest.TestCase):
    def test_question_complete(self):
        self.assertEqual(friday_logic.prepare_question("Friday,  quelle  heure est-il ?"),"Friday, quelle heure est-il ?")
    def test_local_answer(self):
        self.assertIn("Il est", friday_logic.local_answer("Quelle heure est-il ?", 1793532600000))
    def test_regular_questions(self):
        self.assertIsNone(friday_logic.local_answer("Qu'est-ce que la relativité ?", 1793532600000))
    def test_date(self):
        self.assertIn("2026", friday_logic.local_answer("Quel jour sommes nous ?", 1793532600000))
    def test_strict_local_time_and_date(self):
        epoch = 1793532600000
        for question in (
            "Quelle heure est-il ?",
            "Friday, peux-tu me dire l'heure ?",
            "Il est quelle heure maintenant ?",
        ):
            with self.subTest(question=question):
                self.assertIn("Il est", friday_logic.local_answer(question, epoch))
        for question in ("Quelle est la date ?", "En quelle année sommes nous ?"):
            with self.subTest(question=question):
                self.assertIn("2026", friday_logic.local_answer(question, epoch))

    def test_local_does_not_hijack_other_questions(self):
        epoch = 1793532600000
        for question in (
            "Quelle est la météo à Vitry ?",
            "Friday, quelle heure est-il à Vitry ?",
            "Donne-moi l'heure de Paris.",
            "Quel jour est férié à Paris ?",
            "Quel est le prix aujourd'hui ?",
            "Quelle est la date de sortie du film ?",
            "Quelle heure de départ pour le train de Paris ?",
            "Quelle heure est-il à 12 kilomètres ?",
        ):
            with self.subTest(question=question):
                self.assertIsNone(friday_logic.local_answer(question, epoch))
if __name__ == '__main__': unittest.main()
