"""Logique Python embarquée dans F.R.I.D.A.Y. Android (Chaquopy)."""
from datetime import datetime
import re
import unicodedata

DAY_NAMES = ("lundi", "mardi", "mercredi", "jeudi", "vendredi", "samedi", "dimanche")
MONTHS = ("janvier", "février", "mars", "avril", "mai", "juin", "juillet", "août", "septembre", "octobre", "novembre", "décembre")


def _fold(s: str) -> str:
    s = unicodedata.normalize("NFKD", str(s).lower())
    return "".join(c for c in s if not unicodedata.combining(c))


def prepare_question(text: str) -> str:
    """Réduit espaces, caractères de contrôle ; ne coupe jamais la phrase."""
    s = " ".join(str(text).split())
    return s[:12000]


# Les intentions locales doivent être strictes : une phrase parlant de météo,
# d'une ville ou d'un autre sujet doit arriver au moteur DeepSeek.
_TIME_WORDS = frozenset("""
    friday frida freiday freday fride vendredi
    quelle quel quelles quels heure heures il est sont
    as tu vous peux peut me dire donner donne dis moi
    maintenant actuelle actuellement s te plait stp l
""".split())
_DATE_WORDS = frozenset("""
    friday frida freiday freday fride vendredi
    quelle quel quelles quels jour date sommes nous on est en
    annee aujourd hui mois le combien tu peux peut me dire
    donner donne dis moi stp s te plait
""".split())


def local_answer(question: str, epoch_ms: int):
    """Réponse hors ligne uniquement pour une demande d'heure/date sans autre sujet.

    Vosk peut mal transcrire « météo à Vitry » en « heure à Vitry » :
    toute ville, tout sujet et tout nombre supplémentaire excluent la réponse locale.
    """
    tokens = re.findall(r"[a-z]+|[0-9]+", _fold(question))
    if not tokens or len(tokens) > 9:
        return None
    words = set(tokens)
    now = datetime.fromtimestamp(int(epoch_ms) / 1000)
    if ("heure" in words or "heures" in words) and words <= _TIME_WORDS:
        return f"Il est {now.hour} heures {now.minute:02d}."
    if words.intersection(("jour", "date", "annee", "aujourd")) and words <= _DATE_WORDS:
        return f"Nous sommes le {DAY_NAMES[now.weekday()]} {now.day} {MONTHS[now.month-1]} {now.year}."
    return None
