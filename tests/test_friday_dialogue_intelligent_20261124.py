"""FRIDAY 3.14.27 — multi-turn policies and synthetic 6-turn runner.
No user's private logs, contact data, or recorded speech are included.
"""
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest

ROOT=Path(__file__).resolve().parents[1]
J=ROOT/"app/src/main/java/fr/friday/assistant"

class DialogueIntelligentRegression(unittest.TestCase):
    def test_multi_turn_route_in_jvm(self):
        if not shutil.which("javac") or not shutil.which("java"):
            self.skipTest("JDK not available")
        source=r"""
import fr.friday.assistant.ConversationTurnPolicy;
import fr.friday.assistant.DialogueTestPolicy;

public class DialogueIntelligentSmoke {
 static void yes(boolean ok, String msg) {
  if(!ok) throw new AssertionError(msg);
 }
 static final String start="[CONTEXTE PRIVÉ DES ÉCHANGES PRÉCÉDENTS — TÉLÉPHONE UNIQUEMENT]\n"
   +"[ÉCHANGES LES PLUS RÉCENTS]\n";
 static final String end="[FIN CONTEXTE PRIVÉ]\n";
 public static void main(String[] args) {
  String history=start;
  for(int i=0;i<6;i++){
   history+="Utilisateur : " + DialogueTestPolicy.question(i) + "\n";
   history+="Jarvis : Réponse fictive " + (i+1) + " sur les plantes.\n";
  }
  history+=end;
  yes(ConversationTurnPolicy.hasRecentPair(history),"Recognize a recent local exchange");
  yes(ConversationTurnPolicy.shouldUseLocalModel("Et pourquoi ?",history),"Why follow-up local");
  yes(ConversationTurnPolicy.shouldUseLocalModel("Peux-tu détailler la deuxième étape ?",history),
      "Second step follow-up local");
  yes(ConversationTurnPolicy.shouldUseLocalModel("Et pour lui ?",history),"Pronoun follow-up local");
  yes(ConversationTurnPolicy.shouldUseLocalModel("Donne-moi des exemples",history),
      "Examples follow-up local");
  yes(ConversationTurnPolicy.shouldUseLocalModel("Continue",history),
      "Continue follow-up local");
  yes(ConversationTurnPolicy.shouldUseLocalModel("Pourquoi ce choix ?",history),
      "Why this choice follow-up local");
  yes(!ConversationTurnPolicy.shouldUseLocalModel("Et la météo demain ?",history),
      "Weather remains an external tool");
  yes(!ConversationTurnPolicy.shouldUseLocalModel("Lis l'article 2 en entier",history),
      "RSS numbered request must not be stolen");
  yes(!ConversationTurnPolicy.shouldUseLocalModel("Vérifie les dernières actualités",history),
      "Explicit news uses Web");
  yes(!ConversationTurnPolicy.shouldUseLocalModel("Explique Python depuis le début",history),
      "Standalone topic is not a context-only follow-up");
  yes(!ConversationTurnPolicy.shouldUseLocalModel("Et pourquoi ?",""),
      "No context, no invented subject");
  yes(ConversationTurnPolicy.clarification("Et pourquoi ?","").contains("Sur quel sujet"),
      "Missing antecedent asks for the subject");
  yes(ConversationTurnPolicy.clarification("La deuxième étape ?","").contains("liste"),
      "Missing step asks for the list");
  yes(ConversationTurnPolicy.topicReset("Changeons de sujet"),
      "Explicit topic reset detected");
  yes(ConversationTurnPolicy.topicResetAnswer("Changeons de sujet").contains("Quel sujet"),
      "Topic reset solicits next topic");
  yes(!ConversationTurnPolicy.isContextual("Quel est le président actuel ?"),
      "Never turn live facts into local follow-up");
  yes(!ConversationTurnPolicy.hasRecentPair("[RAPPEL THÉMATIQUE ANCIEN]\nAncien sujet utilisateur : 3\n"),
      "Old recall cannot provide a fresh antecedent");
  yes(DialogueTestPolicy.count()==6,"Six ordered synthetic turns");
  String synthetic="";
  for(int i=0;i<DialogueTestPolicy.count();i++){
    String prompt=DialogueTestPolicy.prompt(synthetic,i);
    yes(prompt.contains(DialogueTestPolicy.question(i)), "Current turn included");
    yes(prompt.length()<4500,"Synthetic prompt bounded");
    yes(!prompt.contains("https://"),"No network URLs");
    synthetic=DialogueTestPolicy.appendTurn(synthetic,i,"Des plantes sur Android.");
  }
  yes("INDICE_THEME_REPRIS".equals(DialogueTestPolicy.evaluation(0,"Je suis vos plantes.")),
      "Simple continuity indicator");
  yes("LECTURE_MANUELLE".equals(DialogueTestPolicy.evaluation(5,"Voici mon résumé.")),
      "Long-range summary cannot be automatically certified");
  System.out.println("DialogueIntelligentSmoke OK");
 }
}
"""
        with tempfile.TemporaryDirectory() as tmp:
            f=Path(tmp)/"DialogueIntelligentSmoke.java"
            f.write_text(source,encoding="utf-8")
            p=subprocess.run(["javac","-encoding","UTF-8","-d",tmp,
                              str(J/"ConversationTurnPolicy.java"),
                              str(J/"DialogueTestPolicy.java"),str(f)],
                              capture_output=True,text=True)
            self.assertEqual(0,p.returncode,p.stderr)
            p=subprocess.run(["java","-cp",tmp,"DialogueIntelligentSmoke"],
                             capture_output=True,text=True)
            self.assertEqual(0,p.returncode,p.stderr+p.stdout)

    def test_android_voice_text_privacy_and_llm_integration(self):
        service=(J/"FridayService.java").read_text()
        activity=(J/"MainActivity.java").read_text()
        internet=(J/"JarvisInternet.java").read_text()
        context=(J/"DialogueContext.java").read_text()
        engine=(J/"LocalJarvisEngine.java").read_text()
        bench=(J/"LocalDialogueBenchmark.java").read_text()
        ui=(ROOT/"app/src/main/assets/index.html").read_text()
        self.assertIn("shouldResearch(settings, question, privateDialogue)",service)
        self.assertIn("shouldResearch(prefs, request, privateDialogue)",activity)
        self.assertIn("ConversationTurnPolicy.shouldUseLocalModel(question, privateDialogue)",internet)
        self.assertIn("ConversationTurnPolicy.clarification(",(J/"ConversationClarificationPolicy.java").read_text())
        self.assertIn("MAX_DIALOGUE_MESSAGES = 16",context)
        self.assertIn("ConversationTurnPolicy.localInstruction(cleaned)",engine)
        self.assertIn("LocalJarvisEngine.dialogueTestRaw(",bench)
        self.assertIn("prefs.setSecret(VAULT, lastReport)",bench)
        self.assertIn("LocalDialogueBenchmark.Session",activity)
        self.assertIn('id="dialogueTestPanel"',ui)
        self.assertIn("function dialogueTestRender()",ui)
        self.assertIn("dialogueBenchCopy()",activity)
        self.assertNotIn("JarvisInternet.answer(",bench)
        self.assertNotIn("PrivateConversationHistory.read(",bench)
        self.assertNotIn("DialogueContext.recent(",bench)
        self.assertNotIn("HttpURLConnection",bench)

    def test_play_release_unchanged(self):
        gradle=(ROOT/"app/build.gradle").read_text()
        self.assertIn("versionCode 112",gradle)
        self.assertIn("versionName '3.14.18'",gradle)
        self.assertIn("applicationIdSuffix '.v31421matrix'",gradle)
        self.assertIn("output.versionCode.set(123)",gradle)
        self.assertIn('output.versionName.set("3.14.29-voice-response-optimizer-preview")',gradle)

if __name__=="__main__":
    unittest.main()
