"""Structured instruction patterns + validated compose (Etapa 4.4.2B).

Quality rules:
  - Full-sentence / compound templates only (no naive global WORD_SUBS).
  - Compose path rejects any English residue (no MIXED output).
  - Pulldown never becomes Puxada.
"""

from __future__ import annotations

import re

STATUS_TRANSLATED = "TRANSLATED"
STATUS_UNCHANGED = "UNCHANGED"
STATUS_REVIEW = "REVIEW"
STATUS_NOT_APPLICABLE = "NOT_APPLICABLE"

# Keep your … compounds (complete gender-aware phrases)
_KEEP_COMPOUNDS: dict[tuple[str, ...], str] = {
    ("back", "straight"): "as costas retas",
    ("back", "flat"): "as costas retas",
    ("arms", "extended"): "os braços estendidos",
    ("arms", "straight"): "os braços estendidos",
    ("elbows", "tucked", "in"): "os cotovelos fechados",
    ("knees", "slightly", "bent"): "os joelhos levemente flexionados",
    ("chest", "up"): "o peito erguido",
    ("chest", "out"): "o peito projetado",
    ("head", "up"): "a cabeça erguida",
    ("head", "facing", "forward"): "a cabeça voltada para frente",
    ("core", "tight"): "o core firme",
    ("core", "engaged"): "o core ativado",
    ("torso", "upright"): "o tronco ereto",
    ("shoulders", "back"): "os ombros para trás",
    ("feet", "flat"): "os pés planos",
    ("heels", "down"): "os calcanhares no chão",
}


def _keep_compound(m: re.Match[str]) -> str | None:
    tokens = tuple(m.group(1).lower().rstrip(".").split())
    for length in range(len(tokens), 0, -1):
        key = tokens[:length]
        if key in _KEEP_COMPOUNDS:
            rest = tokens[length:]
            base = f"Mantenha {_KEEP_COMPOUNDS[key]}"
            if rest in {("throughout", "the", "movement"), ("throughout", "the", "entire", "movement")}:
                return base + " durante todo o movimento."
            if not rest:
                return base + "."
            return None
    return None


_STRUCTURED: list[tuple[re.Pattern[str], object]] = [
    (re.compile(r"^Return to the starting position\.?$", re.I),
     lambda m: "Retorne à posição inicial."),
    (re.compile(r"^Slowly return to the starting position\.?$", re.I),
     lambda m: "Retorne lentamente à posição inicial."),
    (re.compile(r"^Return to the starting position as you (inhale|breathe in)\.?$", re.I),
     lambda m: "Retorne à posição inicial ao inspirar."),
    (re.compile(r"^Return to the starting position as you (exhale|breathe out)\.?$", re.I),
     lambda m: "Retorne à posição inicial ao expirar."),
    (re.compile(r"^Slowly return to the starting position as you (inhale|breathe in)\.?$", re.I),
     lambda m: "Retorne lentamente à posição inicial ao inspirar."),
    (re.compile(r"^This will be your starting position\.?$", re.I),
     lambda m: "Essa é a sua posição inicial."),
    (re.compile(r"^This is your starting position\.?$", re.I),
     lambda m: "Essa é a sua posição inicial."),
    (re.compile(r"^Hold for (\d+) seconds?\.?$", re.I),
     lambda m: f"Segure por {m.group(1)} {'segundo' if m.group(1) == '1' else 'segundos'}."),
    (re.compile(r"^Hold this position for (\d+) seconds?\.?$", re.I),
     lambda m: f"Mantenha essa posição por {m.group(1)} {'segundo' if m.group(1) == '1' else 'segundos'}."),
    (re.compile(r"^Hold for a second\.?$", re.I),
     lambda m: "Segure por um segundo."),
    (re.compile(r"^Pause for a second\.?$", re.I),
     lambda m: "Faça uma pausa de um segundo."),
    (re.compile(r"^Keep your (.+)$", re.I), _keep_compound),
    (re.compile(r"^Repeat for the recommended amount of repetitions\.?$", re.I),
     lambda m: "Repita pelo número recomendado de repetições."),
    (re.compile(r"^Repeat for the prescribed amount of repetitions\.?$", re.I),
     lambda m: "Repita pelo número prescrito de repetições."),
    (re.compile(r"^Repeat for the prescribed number of repetitions\.?$", re.I),
     lambda m: "Repita pelo número prescrito de repetições."),
    (re.compile(r"^Repeat the recommended amount of repetitions\.?$", re.I),
     lambda m: "Repita pelo número recomendado de repetições."),
    (re.compile(r"^Breathe out as you perform this portion of the movement\.?$", re.I),
     lambda m: "Expire ao executar esta fase do movimento."),
    (re.compile(r"^Breathe in as you perform this portion of the movement\.?$", re.I),
     lambda m: "Inspire ao executar esta fase do movimento."),
    (re.compile(r"^Exhale as you perform this portion of the movement\.?$", re.I),
     lambda m: "Expire ao executar esta fase do movimento."),
    (re.compile(r"^Inhale as you perform this portion of the movement\.?$", re.I),
     lambda m: "Inspire ao executar esta fase do movimento."),
]


def try_structured(sentence: str) -> str | None:
    s = sentence.strip()
    for pat, fn in _STRUCTURED:
        m = pat.match(s)
        if not m:
            continue
        out = fn(m)
        if out:
            return out
    return None


# ---------------------------------------------------------------------------
# Safe compose
# ---------------------------------------------------------------------------

_SAFE_PHRASES: list[tuple[str, str]] = sorted(
    [
        ("as you breathe out", "ao expirar"),
        ("as you breathe in", "ao inspirar"),
        ("as you exhale", "ao expirar"),
        ("as you inhale", "ao inspirar"),
        ("starting position", "posição inicial"),
        ("contracted position", "posição contraída"),
        ("shoulder width apart", "na largura dos ombros"),
        ("shoulder-width apart", "na largura dos ombros"),
        ("palms facing forward", "palmas para frente"),
        ("palms facing up", "palmas para cima"),
        ("palms facing down", "palmas para baixo"),
        ("upper arms", "braços superiores"),
        ("lower back", "lombar"),
        ("upper chest", "peito superior"),
        ("middle chest", "meio do peito"),
        ("lower chest", "peito inferior"),
        ("shoulder blades", "escápulas"),
        ("medicine ball", "bola medicinal"),
        ("exercise ball", "bola de exercícios"),
        ("smith machine", "máquina Smith"),
        ("pull-up bar", "barra de Barra Fixa"),
        ("face pull", "Face Pull"),
        ("leg press", "Leg Press"),
        ("good morning", "Good Morning"),
        ("skull crusher", "Skull Crusher"),
        ("flat bench", "banco reto"),
        ("incline bench", "banco inclinado"),
        ("decline bench", "banco declinado"),
        ("recommended amount of repetitions", "número recomendado de repetições"),
        ("prescribed amount of repetitions", "número prescrito de repetições"),
        ("prescribed number of repetitions", "número prescrito de repetições"),
        ("throughout the movement", "durante todo o movimento"),
        ("back to the starting position", "de volta à posição inicial"),
        ("to the starting position", "à posição inicial"),
        ("return to the starting position", "retorne à posição inicial"),
        ("keep your back straight", "mantenha as costas retas"),
        ("keeping your body straight", "mantendo o corpo reto"),
        ("make sure that", "certifique-se de que"),
        ("make sure", "certifique-se"),
        ("tip:", "dica:"),
        ("dumbbells", "halteres"),
        ("dumbbell", "halter"),
        ("barbell", "barra"),
        ("kettlebells", "kettlebells"),
        ("kettlebell", "kettlebell"),
        ("pulldown", "Pulldown"),
        ("pull-down", "Pulldown"),
        ("chin-up", "Barra Fixa Supinada"),
        ("pull-up", "Barra Fixa"),
        ("repetitions", "repetições"),
        ("repetition", "repetição"),
        ("seconds", "segundos"),
        ("second", "segundo"),
        ("degrees", "graus"),
    ],
    key=lambda kv: -len(kv[0]),
)

_SAFE_WORDS: dict[str, str] = {
    "the": "", "a": "", "an": "", "your": "seu", "you": "você",
    "and": "e", "or": "ou", "with": "com", "from": "de", "into": "em",
    "onto": "sobre", "on": "em", "in": "em", "at": "em", "to": "para",
    "of": "de", "for": "por", "by": "por", "as": "ao", "while": "enquanto",
    "until": "até", "after": "após", "before": "antes", "during": "durante",
    "then": "em seguida", "this": "esta", "that": "aquele", "will": "",
    "be": "", "is": "é", "are": "são", "should": "deve", "can": "pode",
    "must": "deve", "not": "não", "both": "ambos", "each": "cada",
    "other": "outro", "same": "mesmo", "again": "novamente", "only": "apenas",
    "slowly": "lentamente", "quickly": "rapidamente", "slightly": "levemente",
    "fully": "totalmente", "firmly": "firmemente",
    "keep": "mantenha", "keeping": "mantendo", "hold": "segure",
    "holding": "segurando", "lower": "abaixe", "lowering": "abaixando",
    "raise": "eleve", "raising": "elevando", "lift": "levante",
    "push": "empurre", "pushing": "empurrando", "pull": "puxe",
    "pulling": "puxando", "press": "pressione", "bend": "dobre",
    "bending": "dobrando", "extend": "estenda", "extending": "estendendo",
    "return": "retorne", "place": "posicione", "begin": "comece",
    "start": "comece", "continue": "continue", "repeat": "repita",
    "switch": "troque", "stand": "fique", "sit": "sente-se",
    "lie": "deite-se", "exhale": "expire", "inhale": "inspire",
    "squeeze": "contraia", "contract": "contraia", "relax": "relaxe",
    "pause": "pause", "back": "costas", "chest": "peito",
    "shoulders": "ombros", "shoulder": "ombro", "arms": "braços",
    "arm": "braço", "elbows": "cotovelos", "elbow": "cotovelo",
    "wrists": "pulsos", "wrist": "pulso", "hands": "mãos", "hand": "mão",
    "hips": "quadris", "hip": "quadril", "knees": "joelhos", "knee": "joelho",
    "legs": "pernas", "leg": "perna", "thighs": "coxas", "thigh": "coxa",
    "feet": "pés", "foot": "pé", "heels": "calcanhares", "heel": "calcanhar",
    "glutes": "glúteos", "core": "core", "torso": "tronco", "neck": "pescoço",
    "head": "cabeça", "spine": "coluna", "bar": "barra", "bench": "banco",
    "cable": "polia", "machine": "máquina", "rope": "corda",
    "band": "faixa elástica", "bands": "faixas elásticas", "plate": "anilha",
    "plates": "anilhas", "rack": "rack", "box": "box", "ball": "bola",
    "mat": "colchonete", "floor": "chão", "ground": "chão", "weight": "peso",
    "weights": "pesos", "body": "corpo", "movement": "movimento",
    "exercise": "exercício", "grip": "pegada", "up": "para cima",
    "down": "para baixo", "forward": "para frente", "backward": "para trás",
    "straight": "reto", "extended": "estendido", "bent": "dobrado",
    "tight": "firme", "wide": "aberto", "stationary": "estacionário",
    "upright": "ereto", "flat": "plano", "recommended": "recomendado",
    "prescribed": "prescrito", "desired": "desejado", "appropriate": "adequado",
    "possible": "possível", "position": "posição",
}

_ALLOW_EN = frozenset({
    "pulldown", "kettlebell", "kettlebells", "smith", "swing", "lockout",
    "core", "rack", "box", "trx", "bosu", "ez", "face", "leg", "press",
    "good", "morning", "skull", "crusher", "clean", "snatch", "jerk",
    "ab", "roller",  # named gear sometimes kept
})

# Portuguese tokens that may remain after mapping (articles, verbs, anatomy…)
_PT_OK = frozenset({
    "de", "do", "da", "dos", "das", "em", "no", "na", "nos", "nas", "ao", "à",
    "aos", "às", "para", "por", "com", "sem", "sobre", "entre", "até", "após",
    "antes", "durante", "enquanto", "quando", "como", "mais", "menos", "muito",
    "apenas", "também", "ainda", "já", "não", "sim", "ou", "e", "mas", "se",
    "seu", "sua", "seus", "suas", "um", "uma", "uns", "umas", "o", "a", "os", "as",
    "este", "esta", "estes", "estas", "esse", "essa", "isso", "aquele", "aquela",
    "você", "vocês", "ele", "ela", "eles", "elas",
    "mantenha", "mantendo", "segure", "segurando", "abaixe", "abaixando",
    "eleve", "elevando", "levante", "empurre", "empurrando", "puxe", "puxando",
    "pressione", "dobre", "dobrando", "estenda", "estendendo", "retorne",
    "posicione", "comece", "continue", "repita", "troque", "fique", "sente",
    "deite", "expire", "inspire", "contraia", "relaxe", "pause",
    "costas", "peito", "ombros", "ombro", "braços", "braço", "cotovelos",
    "cotovelo", "pulsos", "pulso", "mãos", "mão", "quadris", "quadril",
    "joelhos", "joelho", "pernas", "perna", "coxas", "coxa", "pés", "pé",
    "calcanhares", "calcanhar", "glúteos", "tronco", "pescoço", "cabeça",
    "coluna", "barra", "banco", "polia", "máquina", "corda", "faixa",
    "elástica", "elásticas", "anilha", "anilhas", "bola", "colchonete",
    "chão", "peso", "pesos", "corpo", "movimento", "exercício", "pegada",
    "cima", "baixo", "frente", "trás", "reto", "reta", "retas", "estendido",
    "estendidos", "estendida", "estendidas", "dobrado", "dobrados", "firme",
    "aberto", "estacionário", "ereto", "plano", "recomendado", "prescrito",
    "desejado", "adequado", "possível", "posição", "inicial", "contraída",
    "lentamente", "rapidamente", "levemente", "totalmente", "firmemente",
    "ambos", "cada", "outro", "mesmo", "novamente", "deve", "pode",
    "são", "é", "segundo", "segundos", "repetição", "repetições", "graus",
    "halter", "halteres", "kettlebells", "dica", "certifique", "se",
    "largura", "ombros", "palmas", "escápulas", "lombar", "inferior",
    "superior", "meio", "medicinal", "exercícios", "supinada", "fixa",
    "número", "todo", "amplitude", "próximo", "próximos", "leve", "flexão",
    "máximo", "mínimo", "série", "séries", "cadência", "contração",
    "extensão", "rotação", "postura", "forma", "força", "resistência",
    "carga", "intensidade", "volume", "frequência", "execute", "complete",
    "controle", "estabilize", "ative", "alongue", "comprima",
    "horizontalmente", "verticalmente", "diagonalmente", "lateralmente",
    "dentro", "fora", "paralelo", "perpendicular", "neutro", "pronado",
    "supinado", "inclinado", "declinado", "elevado", "flexionado",
    "estreito", "fechado", "brevemente", "momentaneamente", "imediatamente",
    "gradualmente", "simultaneamente", "inicialmente", "fim", "seguida",
    "primeiro", "terceiro",  # segundo already listed
    "dele", "dela", "disso", "nisso", "neles", "nelas", "nele", "nela",
    "pelo", "pela", "pelos", "pelas", "num", "numa", "dele", "dessa",
    "desse", "neste", "nesta", "naquele", "naquela",
    "retas", "retos", "firmes", "abertos", "planos", "eretas",
    "estando", "sendo", "tendo", "fazendo", "indo", "vindo",
    "uma", "vez", "duas", "vezes", "dez", "vinte",
})


def _has_english_residue(text: str) -> bool:
    """Reject if any non-allowlisted ASCII token is not recognized Portuguese."""
    for w in re.findall(r"[A-Za-zÀ-ÿ]+", text.lower()):
        if w in _ALLOW_EN:
            continue
        if w in _PT_OK:
            continue
        if re.search(r"[áàâãéêíóôõúç]", w):
            continue
        # digits-only already excluded by regex
        if w.isascii() and len(w) >= 2:
            return True
    return False


def _cleanup_pt(text: str) -> str:
    t = re.sub(r"\s{2,}", " ", text).strip()
    t = re.sub(r"\s+([.,;:!?])", r"\1", t)
    if t:
        t = t[0].upper() + t[1:]
    return t


def try_safe_compose(sentence: str) -> str | None:
    s = sentence.strip()
    if len(s) > 180:
        return None
    out = s
    for src, repl in _SAFE_PHRASES:
        out = re.compile(re.escape(src), re.I).sub(repl, out)

    def word_repl(m: re.Match[str]) -> str:
        low = m.group(0).lower()
        return _SAFE_WORDS.get(low, m.group(0))

    out = re.sub(r"[A-Za-zÀ-ÿ]+", word_repl, out)
    out = _cleanup_pt(out)
    if not out or out.lower() == s.lower():
        return None
    if _has_english_residue(out):
        return None
    if re.search(r"(?i)\bpuxada\b", out) and re.search(r"(?i)pull.?down", s):
        return None
    if not re.search(
        r"[áàâãéêíóôõúçÁÀÂÃÉÊÍÓÔÕÚÇ]|ção|ões|\b(de|do|da|dos|das|para|com|em|ao|à|seu|sua)\b",
        out,
        re.I,
    ):
        return None
    return out


def force_review_reason(sentence: str) -> str | None:
    s = sentence.strip()
    if not s:
        return "empty"
    if len(s) < 12:
        return "fragment"
    if re.search(r"<[^>]+>", s):
        return "html"
    if re.search(r"\.(jpg|png|gif|jpeg)\b", s, re.I):
        return "image-ref"
    if s.endswith(("...", "…")) or s.endswith(","):
        return "truncated"
    if re.search(r"\b(your partner|a partner|spotter)\b", s, re.I):
        return "partner"
    if re.search(r"\b(see image|as shown|pictured|illustration)\b", s, re.I):
        return "visual-ref"
    if re.search(r"\b(http|www\.)\b", s, re.I):
        return "url"
    return None
