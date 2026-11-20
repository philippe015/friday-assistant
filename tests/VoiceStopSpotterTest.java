import fr.friday.assistant.VoiceStopSpotter;

public final class VoiceStopSpotterTest {
    private static void yes(boolean ok, String why) {
        if (!ok) throw new AssertionError(why);
    }
    public static void main(String[] args) {
        VoiceStopSpotter d = new VoiceStopSpotter();
        yes(d.accept("stop", "Bonjour, voici le résultat.", false)
                == VoiceStopSpotter.Action.NONE, "un seul partiel ne suffit pas");
        yes(d.accept("stop", "Bonjour, voici le résultat.", false)
                == VoiceStopSpotter.Action.STOP, "deux partiels identiques déclenchent l'arrêt");
        yes(d.accept("stop", "Bonjour, voici le résultat.", true)
                == VoiceStopSpotter.Action.STOP, "final stop immédiat");
        yes(d.accept("stop", "FRIDAY dit : stop, puis recommence.", true)
                == VoiceStopSpotter.Action.NONE, "écho de la propre voix ignoré");
        d.reset();
        yes(d.accept("jarvis stop", "Voici une longue réponse.", true)
                == VoiceStopSpotter.Action.STOP, "commande avec nom détectée");
        yes(d.accept("j'ai une précision", "Voici une réponse.", true)
                == VoiceStopSpotter.Action.CLARIFY, "demande de précision sans wake");
        yes(d.accept("Jarvis, j'ai une précision", "Bonjour.", true)
                == VoiceStopSpotter.Action.CLARIFY, "demande de précision avec wake");
        yes(d.accept("Jarvis arrête l'écoute", "Bonjour.", true)
                == VoiceStopSpotter.Action.STOP_MIC, "arrêt service explicite");
        yes(d.accept("quel est le temps", "Je vais répondre.", true)
                == VoiceStopSpotter.Action.NONE, "aucun faux arrêt");
        yes(VoiceStopSpotter.GRAMMAR.startsWith("[\"[unk]\""), "inconnus Vosk autorisés");
        yes(VoiceStopSpotter.GRAMMAR.contains("\"stop\""), "grammaire STOP");
        yes(VoiceStopSpotter.GRAMMAR.contains("\"j'ai une précision\""), "grammaire précision");
        yes(d.accept("stop stop", "Voici une réponse.", false)
                == VoiceStopSpotter.Action.STOP, "répétition détectée dès le premier résultat partiel");
        yes(d.accept("stop stop stop", "Voici une réponse.", true)
                == VoiceStopSpotter.Action.STOP, "stop répété trois fois");
        yes(d.accept("arrête arrête", "Voici une réponse.", false)
                == VoiceStopSpotter.Action.STOP, "répétition arrête acceptée");
        yes(d.accept("stop stop", "Il explique stop stop dans un tutoriel.", true)
                == VoiceStopSpotter.Action.NONE, "stop répété généré par FRIDAY ignoré");
        yes(VoiceStopSpotter.GRAMMAR.contains("\"stop stop stop\""),
                "grammaire Vosk inclut les trois STOP");
        System.out.println("VoiceStopSpotterTest OK (17 cas)");
    }
}
