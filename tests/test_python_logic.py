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
if __name__ == '__main__': unittest.main()
