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


def local_answer(question: str, epoch_ms: int):
    """Utilise l'horloge et le fuseau horaire réellement définis sur Android."""
    q = _fold(question)
    now = datetime.fromtimestamp(int(epoch_ms) / 1000)
    if any(x in q for x in ("quelle heure", "heure est-il", "il est quelle heure", "donne moi l'heure", "donne-moi l'heure")):
        return f"Il est {now.hour} heures {now.minute:02d}."
    if any(x in q for x in ("quel jour", "quelle date", "date sommes nous", "date sommes-nous", "quelle annee", "en quelle annee")):
        return f"Nous sommes le {DAY_NAMES[now.weekday()]} {now.day} {MONTHS[now.month-1]} {now.year}."
    return None
