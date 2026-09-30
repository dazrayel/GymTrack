"""Etapa 4.4.2A — Instruction audit (analysis only, no translation).

Reads importable exercises from gymtrack-exercises.json, classifies each
instruction against the current translate_instructions.py mechanism, and
writes deterministic JSON + Markdown reports under analysis/.

Does NOT:
  - translate instructions
  - modify sourceData / gymtrack-exercises.json / translations.json / app/
"""

from __future__ import annotations

import hashlib
import json
import re
import sys
from collections import Counter, defaultdict
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "analysis"))
sys.path.insert(0, str(ROOT / "translate"))
sys.path.insert(0, str(ROOT / "curation"))

from catalog_paths import analysis_catalog_path  # noqa: E402
from classify_importability import classify as classify_importability  # noqa: E402
from translate_instructions import translate_sentence  # noqa: E402

NORMALIZED = analysis_catalog_path()
OUT_JSON = ROOT / "analysis" / "instruction-audit-4-4-2.json"
OUT_MD = ROOT / "analysis" / "INSTRUCTION_AUDIT_4_4_2.md"

IMPORTABLE_LABELS = frozenset({"IMPORTABLE", "IMPORTABLE_WITH_FALLBACK"})

# ---------------------------------------------------------------------------
# Heuristic language signals (conservative — UNKNOWN when unsure)
# ---------------------------------------------------------------------------

_PT_MARKERS = frozenset({
    "posição", "posicao", "inicial", "repita", "repetições",
    "braços", "braço", "pernas", "perna", "joelhos", "joelho",
    "ombros", "ombro", "costas", "peito", "quadril", "quadris", "tronco",
    "lentamente", "mantenha", "segure", "abaixe", "eleve", "empurre",
    "puxe", "inspire", "expire", "deite-se", "sente-se", "fique",
    "também", "não", "você", "seus", "suas", "este", "esta", "esse",
    "essa", "até", "após", "quando", "enquanto", "durante",
    "retorne", "comece", "exercício", "exercicio", "movimento",
    "halteres", "halter", "barra", "banco", "máquina", "maquina",
    "panturrilha", "panturrilhas", "glúteos", "gluteos", "abdômen",
    "abdomen", "cotovelo", "cotovelos", "pulso", "pulsos",
    "antebraço", "antebraco", "pescoço", "pescoco", "coluna",
})

_EN_MARKERS = frozenset({
    "the", "your", "and", "with", "from", "this", "that", "will", "be",
    "position", "starting", "repeat", "slowly", "lower", "raise", "keep",
    "hold", "push", "pull", "bend", "extend", "return", "inhale", "exhale",
    "lie", "sit", "stand", "grip", "barbell", "dumbbell", "bench", "cable",
    "machine", "while", "until", "after", "before", "during", "should",
    "must", "then", "into", "onto", "across", "above", "below", "between",
    "feet", "hands", "arms", "legs", "knees", "shoulders", "back", "chest",
    "hips", "elbows", "wrists", "core", "torso", "body", "weight",
})

_EN_START_VERBS = (
    "lie ", "sit ", "stand ", "hold ", "keep ", "grip ", "grasp ",
    "lower ", "raise ", "lift ", "push ", "pull ", "bend ", "extend ",
    "return ", "repeat ", "slowly ", "begin ", "start ", "place ",
    "position ", "secure ", "inhale ", "exhale ", "breathe ", "maintain ",
    "squeeze ", "contract ", "relax ", "pause ", "continue ", "bring ",
    "move ", "step ", "walk ", "jump ", "hop ", "drive ", "press ",
)

_ANATOMY_TERMS = [
    "chest", "back", "shoulder", "shoulders", "arm", "arms",
    "elbow", "elbows", "wrist", "wrists", "hand", "hands",
    "hip", "hips", "knee", "knees", "leg", "legs", "thigh", "thighs",
    "hamstring", "hamstrings", "calf", "calves", "abs", "abdomen",
    "abdominal", "core", "spine", "neck", "glute", "glutes",
    "forearm", "forearms", "lat", "lats", "trap", "traps",
    "quad", "quads", "quadriceps", "bicep", "biceps", "tricep", "triceps",
    "ankle", "ankles", "heel", "heels", "toe", "toes", "torso", "head",
    "foot", "feet",
]

_EQUIPMENT_TERMS = [
    "barbell", "dumbbell", "dumbbells", "cable", "machine", "bench",
    "bar", "rope", "band", "bands", "kettlebell", "kettlebells", "plate",
    "plates", "box", "ball", "mat", "rack", "smith", "pulley", "handle",
    "handles", "trap bar", "ez bar", "ez-bar", "medicine ball",
    "exercise ball", "physioball", "bosu", "sled", "chains", "chain",
    "collar", "collars", "platform", "step", "cone", "cones", "hurdle",
    "hurdles", "towel", "foam roller", "roller", "rings",
    "suspension", "sandbag", "tire", "prowler", "yoke",
]

_TECHNICAL_TERMS = [
    ("pulldown", "Pulldown — NUNCA traduzir como Puxada"),
    ("face pull", "Face Pull — manter em inglês"),
    ("leg press", "Leg Press — manter em inglês"),
    ("pull-up", "Pull-Up → Barra Fixa (nomes; validar em instruções)"),
    ("pullup", "Pull-Up → Barra Fixa"),
    ("chin-up", "Chin-Up → Barra Fixa Supinada"),
    ("chinup", "Chin-Up → Barra Fixa Supinada"),
    ("smith", "Smith — máquina Smith"),
    ("kettlebell", "Kettlebell — manter"),
    ("swing", "Swing — frequentemente mantido"),
    ("good morning", "Good Morning — UNCHANGED em nomes"),
    ("skull crusher", "Skull Crusher — UNCHANGED em nomes"),
    ("deadlift", "Deadlift / Levantamento Terra"),
    ("squat", "Squat / Agachamento"),
    ("lunge", "Lunge / Afundo"),
    ("clean", "Clean — olímpico UNCHANGED"),
    ("snatch", "Snatch — olímpico UNCHANGED"),
    ("jerk", "Jerk — olímpico UNCHANGED"),
    ("rack", "Rack — contexto olímpico/suporte"),
    ("lockout", "Lockout — termo técnico"),
    ("hook grip", "Hook grip — olímpico"),
    ("front rack", "Front rack — olímpico"),
    ("starting position", "posição inicial"),
]

_COMMON_EN_VERBS = frozenset({
    "stand", "sit", "lie", "lay", "hold", "grip", "grasp", "lower", "raise",
    "lift", "push", "pull", "bend", "extend", "rotate", "keep", "maintain",
    "return", "repeat", "inhale", "exhale", "breathe", "place", "position",
    "secure", "squeeze", "contract", "relax", "pause", "continue", "bring",
    "move", "step", "walk", "jump", "hop", "drive", "press", "begin",
    "start", "finish", "switch", "alternate", "curl", "row", "shrug",
    "twist", "flex", "stretch", "lean", "arch", "tuck", "reach", "grab",
    "release", "control", "descend", "ascend", "explode", "land", "swing",
})


def _importable_rows(catalog: list[dict]) -> list[dict]:
    rows = [r for r in catalog if classify_importability(r) in IMPORTABLE_LABELS]
    rows.sort(key=lambda r: (r.get("source") or "", r.get("externalId") or ""))
    return rows


def _word_set(text: str) -> set[str]:
    return set(re.findall(r"[a-zà-ü]+", text.lower()))


def classify_language(text: str) -> str:
    """Conservative heuristic language classification.

    Not a perfect detector — UNKNOWN when signals are weak/conflicting.
    """
    s = text.strip()
    if not s:
        return "EMPTY"

    words = _word_set(s)
    if not words:
        return "UNKNOWN"

    pt_hits = len(words & _PT_MARKERS)
    en_hits = len(words & _EN_MARKERS)
    lower = s.lower()
    starts_en = any(lower.startswith(v) for v in _EN_START_VERBS)

    has_pt_accent = bool(re.search(
        r"[áàâãéêíóôõúç]|ção|ções|mente\b|você|não\b|posição|repita",
        lower,
    ))

    if pt_hits >= 3 and en_hits <= 1 and has_pt_accent:
        return "PORTUGUESE"
    if pt_hits >= 2 and en_hits >= 3:
        return "MIXED"
    if has_pt_accent and en_hits >= 4:
        return "MIXED"
    if en_hits >= 2 or starts_en:
        return "ENGLISH"
    if pt_hits >= 2 and has_pt_accent:
        return "PORTUGUESE"
    return "UNKNOWN"


def classify_instruction(text: str) -> str:
    """Classify one instruction against current translator + language heuristic.

    Categories are mutually exclusive by priority:
      TRANSLATED_EXISTING > PORTUGUESE > MIXED > PASSTHROUGH_ENGLISH > UNKNOWN
    """
    s = text.strip()
    if not s:
        return "UNKNOWN"

    result = translate_sentence(s)
    if result.status == "TRANSLATED" and result.text != s:
        return "TRANSLATED_EXISTING"
    if getattr(result, "status", None) == "NOT_APPLICABLE":
        return "UNKNOWN"

    lang = classify_language(s)
    if lang == "PORTUGUESE":
        return "PORTUGUESE"
    if lang == "MIXED":
        return "MIXED"
    if lang == "ENGLISH":
        return "PASSTHROUGH_ENGLISH"
    if re.search(r"[A-Za-z]", s) and not re.search(r"[áàâãéêíóôõúçÁÀÂÃÉÊÍÓÔÕÚÇ]", s):
        return "PASSTHROUGH_ENGLISH"
    return "UNKNOWN"


def _leading_verb(text: str) -> str | None:
    m = re.match(r"^([A-Za-z]+)\b", text.strip())
    if not m:
        return None
    return m.group(1).lower()


def _pattern_key(text: str) -> str:
    """Normalize for pattern grouping only — does not alter originals."""
    s = text.strip()
    s = re.sub(r"\d+(\.\d+)?", "#", s)
    s = re.sub(r"\s+", " ", s)
    stems = [
        (r"^(Keep your)\s+\w+(?:\s+\w+)?\s+", r"\1 [part] "),
        (r"^(Maintain)\s+", r"\1 "),
        (r"^(Return to the starting position).*", r"Return to the starting position…"),
        (r"^(Slowly return to the starting position).*", r"Slowly return to the starting position…"),
        (r"^(Lower)\s+", r"\1 "),
        (r"^(Raise)\s+", r"\1 "),
        (r"^(Push)\s+", r"\1 "),
        (r"^(Pull)\s+", r"\1 "),
        (r"^(Hold)\s+", r"\1 "),
        (r"^(Repeat).*", r"Repeat…"),
        (r"^(Lie)\s+", r"\1 "),
        (r"^(Sit)\s+", r"\1 "),
        (r"^(Stand)\s+", r"\1 "),
        (r"^(Begin)\s+", r"\1 "),
        (r"^(Place)\s+", r"\1 "),
        (r"^(This will be your starting position).*", r"This will be your starting position."),
    ]
    for pat, repl in stems:
        new = re.sub(pat, repl, s, flags=re.I)
        if new != s:
            s = new
            break
    if len(s) > 80:
        s = s[:77] + "…"
    return s


def _count_term_occurrences(instructions: list[str], terms: list[str]) -> list[dict]:
    counts: Counter[str] = Counter()
    for instr in instructions:
        lower = instr.lower()
        for term in terms:
            pattern = r"(?<![a-z])" + re.escape(term.lower()) + r"(?![a-z])"
            n = len(re.findall(pattern, lower))
            if n:
                counts[term] += n
    rows = [{"term": t, "count": c} for t, c in counts.items()]
    rows.sort(key=lambda r: (-r["count"], r["term"].lower()))
    return rows


def _count_anatomy(instructions: list[str]) -> list[dict]:
    counts: Counter[str] = Counter()
    for instr in instructions:
        lower = instr.lower()
        for en in _ANATOMY_TERMS:
            pattern = r"(?<![a-z])" + re.escape(en) + r"(?![a-z])"
            n = len(re.findall(pattern, lower))
            if n:
                counts[en] += n
    rows = [{"term": t, "count": c} for t, c in counts.items()]
    rows.sort(key=lambda r: (-r["count"], r["term"].lower()))
    return rows


def _count_technical(instructions: list[str]) -> list[dict]:
    counts: Counter[str] = Counter()
    notes = {t: note for t, note in _TECHNICAL_TERMS}
    for instr in instructions:
        lower = instr.lower()
        for term, _note in _TECHNICAL_TERMS:
            pattern = r"(?<![a-z])" + re.escape(term) + r"(?![a-z])"
            n = len(re.findall(pattern, lower))
            if n:
                counts[term] += n
    rows = [
        {"term": t, "count": c, "note": notes[t]}
        for t, c in counts.items()
    ]
    rows.sort(key=lambda r: (-r["count"], r["term"].lower()))
    return rows


def _count_verbs(instructions: list[str]) -> list[dict]:
    counts: Counter[str] = Counter()
    for instr in instructions:
        words = re.findall(r"[A-Za-z]+", instr.lower())
        for w in words:
            if w in _COMMON_EN_VERBS:
                counts[w] += 1
    rows = [{"verb": v, "count": c} for v, c in counts.items()]
    rows.sort(key=lambda r: (-r["count"], r["verb"]))
    return rows


def _leading_verb_counts(instructions: list[str]) -> list[dict]:
    counts: Counter[str] = Counter()
    for instr in instructions:
        lead = _leading_verb(instr)
        if lead:
            counts[lead] += 1
    rows = [{"verb": v, "count": c} for v, c in counts.items()]
    rows.sort(key=lambda r: (-r["count"], r["verb"]))
    return rows


def _pattern_priority(pattern: str, count: int) -> str:
    lower = pattern.lower()
    high_stems = (
        "keep ", "maintain ", "return ", "slowly return", "lower ", "raise ",
        "push ", "pull ", "hold ", "repeat", "this will be your starting",
    )
    if any(lower.startswith(s) for s in high_stems) and count >= 3:
        return "HIGH"
    if count >= 20:
        return "HIGH"
    if count >= 5:
        return "MEDIUM"
    return "LOW"


def detect_potential_review(
    external_id: str,
    original_name: str,
    index: int,
    instruction: str,
) -> dict | None:
    """Flag instructions that should not be auto-translated without human review."""
    s = instruction.strip()
    reasons: list[str] = []

    if len(s) < 20:
        reasons.append("fragmento/muito curto")
    if re.search(r"<[^>]+>", s):
        reasons.append("contém HTML")
    if re.search(r"\.(jpg|png|gif|jpeg)\b", s, re.I):
        reasons.append("referência a imagem")
    if re.search(r"\b(tip:|note:|warning:)\b", s, re.I) and len(s) > 200:
        reasons.append("instrução longa com Tip/Note embutido")
    if re.search(
        r"\b(Janda|Gironda|Zottman|Svend|Tate|Pallof|Frankenstein|"
        r"Jefferson|Otis|Conan|Rocky|Bradford|Atlas)\b",
        s,
    ):
        reasons.append("nome próprio / variação named")
    if s.endswith(("...", "…")) or s.endswith(","):
        reasons.append("aparentemente truncada")
    if re.search(r"\b(see image|as shown|pictured|illustration)\b", s, re.I):
        reasons.append("referência visual implícita")
    if re.search(r"\b(your partner|a partner|spotter)\b", s, re.I):
        reasons.append("depende de parceiro/contexto")
    if re.search(r"\band you begin\b", s, re.I):
        reasons.append("possível erro gramatical na fonte")
    if re.search(r"\b(http|www\.)\b", s, re.I):
        reasons.append("contém URL")

    if not reasons:
        return None
    uniq = sorted(set(reasons))
    return {
        "externalId": external_id,
        "originalName": original_name,
        "instructionIndex": index,
        "instruction": s,
        "motivo": "; ".join(uniq),
    }


def _recommendations(
    class_counts: Counter,
    patterns: list[dict],
    verbs: list[dict],
    technical: list[dict],
    passthrough_freq: Counter,
) -> list[dict]:
    recs: list[dict] = []
    high = [p for p in patterns if p["priority"] == "HIGH"][:15]
    recs.append({
        "id": "expand-sentence-lookup",
        "priority": "HIGH",
        "action": (
            f"Expandir SENTENCE_LOOKUP com frases exact-match de alta frequência. "
            f"Atualmente TRANSLATED_EXISTING={class_counts['TRANSLATED_EXISTING']}, "
            f"PASSTHROUGH_ENGLISH={class_counts['PASSTHROUGH_ENGLISH']}."
        ),
        "examples": [p["pattern"] for p in high[:10]],
    })
    recs.append({
        "id": "consistent-verb-glossary",
        "priority": "HIGH",
        "action": (
            "Definir glossário consistente de verbos de ação para a próxima etapa "
            "(Keep→Mantenha, Lower→Abaixe, Raise→Eleve, Return→Retorne, etc.)."
        ),
        "examples": [f"{v['verb']} ({v['count']})" for v in verbs[:12]],
    })
    pull_related = [
        t for t in technical
        if "pull" in t["term"] or t["term"] in ("pulldown", "face pull", "pull-up", "chin-up")
    ]
    recs.append({
        "id": "pull-pulldown-rules",
        "priority": "HIGH",
        "action": (
            "Validar nas instruções as regras já estabelecidas: "
            "Pulldown≠Puxada; Pull-Up→Barra Fixa; Chin-Up→Barra Fixa Supinada; "
            "Face Pull permanece Face Pull."
        ),
        "examples": [f"{t['term']} ({t['count']})" for t in pull_related],
    })
    recs.append({
        "id": "no-partial-substitution",
        "priority": "HIGH",
        "action": (
            "Não reativar PHRASE_SUBS/WORD_SUBS parciais — produzem MIXED EN/PT. "
            "Preferir exact-match + templates estruturados."
        ),
        "examples": [],
    })
    top_pass = [
        t for t, _c in sorted(passthrough_freq.items(), key=lambda kv: (-kv[1], kv[0]))[:10]
    ]
    recs.append({
        "id": "next-lookup-candidates",
        "priority": "MEDIUM",
        "action": "Próximos candidatos exact-match pelo impacto (ocorrências).",
        "examples": [t if len(t) <= 120 else t[:117] + "…" for t in top_pass],
    })
    recs.append({
        "id": "manual-review-queue",
        "priority": "MEDIUM",
        "action": (
            "Tratar POTENTIAL_REVIEW separadamente (parceiro, Tip embutido, "
            "nomes próprios, fragmentos) — não traduzir em lote cego."
        ),
        "examples": [],
    })
    recs.append({
        "id": "keep-english-technical",
        "priority": "LOW",
        "action": (
            "Manter termos técnicos já decididos em inglês quando aparecerem "
            "nas instruções (Pulldown, Face Pull, Good Morning, Skull Crusher, "
            "Clean/Snatch/Jerk) salvo contexto que exija adaptação."
        ),
        "examples": [t["term"] for t in technical[:10]],
    })
    return recs


def build_audit(catalog: list[dict]) -> dict[str, Any]:
    rows = _importable_rows(catalog)
    if len(rows) != 777:
        raise AssertionError(f"Expected 777 importable, got {len(rows)}")

    all_instr: list[str] = []
    instr_meta: list[tuple[str, str, int, str]] = []
    length_dist: Counter[int] = Counter()
    with_instr = 0
    without_instr = 0
    identities: list[tuple[str, str]] = []

    for ex in rows:
        eid = ex["externalId"]
        name = ex.get("name") or ""
        source = ex.get("source") or "free-exercise-db"
        identities.append((source, eid))
        sd = ex.get("sourceData") or {}
        instructions = list(sd.get("instructions") or [])
        n = len(instructions)
        length_dist[n] += 1
        if n == 0:
            without_instr += 1
        else:
            with_instr += 1
        for i, text in enumerate(instructions):
            all_instr.append(text)
            instr_meta.append((eid, name, i, text))

    if len(identities) != len(set(identities)):
        raise AssertionError("Duplicate (source, externalId) identities")

    total_instructions = len(all_instr)
    freq = Counter(all_instr)
    unique_instructions = len(freq)

    class_counts: Counter[str] = Counter()
    class_by_phrase: dict[str, str] = {}
    for text in all_instr:
        label = classify_instruction(text)
        class_counts[label] += 1
        class_by_phrase.setdefault(text, label)

    for key in ("TRANSLATED_EXISTING", "PASSTHROUGH_ENGLISH", "PORTUGUESE", "MIXED", "UNKNOWN"):
        class_counts.setdefault(key, 0)

    cls_sum = sum(
        class_counts[k]
        for k in ("TRANSLATED_EXISTING", "PASSTHROUGH_ENGLISH", "PORTUGUESE", "MIXED", "UNKNOWN")
    )
    if cls_sum != total_instructions:
        raise AssertionError(f"Classification sum {cls_sum} != total {total_instructions}")

    top_instructions = [
        {"instruction": t, "count": c, "classification": class_by_phrase[t]}
        for t, c in sorted(freq.items(), key=lambda kv: (-kv[1], kv[0]))
    ]

    passthrough_texts = [t for t in all_instr if class_by_phrase[t] == "PASSTHROUGH_ENGLISH"]
    passthrough_freq = Counter(passthrough_texts)
    pattern_counts: Counter[str] = Counter()
    for t in passthrough_texts:
        pattern_counts[_pattern_key(t)] += 1

    patterns = []
    for pat, c in sorted(pattern_counts.items(), key=lambda kv: (-kv[1], kv[0])):
        patterns.append({
            "pattern": pat,
            "count": c,
            "priority": _pattern_priority(pat, c),
        })

    lead_groups: Counter[str] = Counter()
    for t in passthrough_texts:
        lead = _leading_verb(t)
        if lead:
            lead_groups[lead] += 1
    leading_verbs_passthrough = [
        {"verb": v, "count": c}
        for v, c in sorted(lead_groups.items(), key=lambda kv: (-kv[1], kv[0]))
    ]

    verbs = _count_verbs(all_instr)
    leading_verbs = _leading_verb_counts(all_instr)
    anatomy = _count_anatomy(all_instr)
    equipment = _count_term_occurrences(all_instr, _EQUIPMENT_TERMS)
    technical = _count_technical(all_instr)

    potential_review: list[dict] = []
    seen_review_keys: set[tuple[str, int, str]] = set()
    for eid, name, idx, text in instr_meta:
        item = detect_potential_review(eid, name, idx, text)
        if item is None:
            continue
        key = (eid, idx, item["motivo"])
        if key in seen_review_keys:
            continue
        seen_review_keys.add(key)
        potential_review.append(item)
    potential_review.sort(key=lambda r: (r["externalId"], r["instructionIndex"], r["motivo"]))

    duplicated_unique = sum(1 for _t, c in freq.items() if c > 1)
    exclusive_unique = sum(1 for _t, c in freq.items() if c == 1)
    duplicated_occurrences = sum(c for _t, c in freq.items() if c > 1)

    distribution = [
        {"instructionCount": n, "exerciseCount": length_dist[n]}
        for n in sorted(length_dist.keys())
    ]

    return {
        "source": "free-exercise-db",
        "version": 1,
        "stage": "4.4.2A",
        "totalExercises": 777,
        "exercisesWithInstructions": with_instr,
        "exercisesWithoutInstructions": without_instr,
        "totalInstructions": total_instructions,
        "uniqueInstructions": unique_instructions,
        "duplicatedUniqueInstructions": duplicated_unique,
        "exclusiveUniqueInstructions": exclusive_unique,
        "duplicatedOccurrences": duplicated_occurrences,
        "classification": {
            "TRANSLATED_EXISTING": class_counts["TRANSLATED_EXISTING"],
            "PASSTHROUGH_ENGLISH": class_counts["PASSTHROUGH_ENGLISH"],
            "PORTUGUESE": class_counts["PORTUGUESE"],
            "MIXED": class_counts["MIXED"],
            "UNKNOWN": class_counts["UNKNOWN"],
        },
        "classificationRule": (
            "TRANSLATED_EXISTING = exact SENTENCE_LOOKUP hit with different PT text; "
            "else PORTUGUESE/MIXED by conservative heuristic; "
            "else PASSTHROUGH_ENGLISH for English/passthrough; else UNKNOWN. "
            "Categories are mutually exclusive by priority."
        ),
        "languageNote": (
            "Language detection is heuristic and conservative. "
            "UNKNOWN means insufficient confidence — not a claim of foreign language."
        ),
        "instructionCountDistribution": distribution,
        "topInstructions": top_instructions[:50],
        "patterns": patterns[:100],
        "leadingVerbs": leading_verbs[:40],
        "leadingVerbsPassthrough": leading_verbs_passthrough[:40],
        "verbs": verbs[:60],
        "anatomy": anatomy,
        "equipment": equipment,
        "technicalTerms": technical,
        "potentialReview": potential_review,
        "potentialReviewCount": len(potential_review),
        "recommendations": _recommendations(
            class_counts, patterns, verbs, technical, passthrough_freq
        ),
    }


def dumps_deterministic(payload: dict) -> str:
    return json.dumps(payload, ensure_ascii=False, indent=2, sort_keys=False) + "\n"


def write_markdown(payload: dict) -> str:
    c = payload["classification"]
    lines: list[str] = []
    lines.append("# INSTRUCTION_AUDIT_4_4_2 — Auditoria de instruções (Etapa 4.4.2A)")
    lines.append("")
    lines.append("**Escopo**: análise apenas — **nenhuma tradução** foi aplicada.")
    lines.append("")
    lines.append(
        "A detecção de idioma é **heurística e conservadora**. "
        "`UNKNOWN` significa confiança insuficiente, não detecção perfeita."
    )
    lines.append("")
    lines.append("---")
    lines.append("")
    lines.append("## 1. Resumo")
    lines.append("")
    lines.append("```text")
    lines.append(f"Exercícios analisados: {payload['totalExercises']}")
    lines.append(f"Com instruções: {payload['exercisesWithInstructions']}")
    lines.append(f"Sem instruções: {payload['exercisesWithoutInstructions']}")
    lines.append("")
    lines.append(f"Total de instruções: {payload['totalInstructions']}")
    lines.append(f"Instruções únicas: {payload['uniqueInstructions']}")
    lines.append(f"Únicas repetidas (count>1): {payload['duplicatedUniqueInstructions']}")
    lines.append(f"Únicas exclusivas (count=1): {payload['exclusiveUniqueInstructions']}")
    lines.append("```")
    lines.append("")
    lines.append("## 2. Estado atual (mecanismo `translate_instructions.py`)")
    lines.append("")
    lines.append("```text")
    lines.append(f"TRANSLATED_EXISTING: {c['TRANSLATED_EXISTING']}")
    lines.append(f"PASSTHROUGH_ENGLISH: {c['PASSTHROUGH_ENGLISH']}")
    lines.append(f"PORTUGUESE:          {c['PORTUGUESE']}")
    lines.append(f"MIXED:               {c['MIXED']}")
    lines.append(f"UNKNOWN:             {c['UNKNOWN']}")
    lines.append("```")
    lines.append("")
    total_cls = sum(c.values())
    lines.append(
        f"Soma das categorias: **{total_cls}** "
        f"(deve igualar total de instruções = {payload['totalInstructions']})"
    )
    lines.append("")
    lines.append(f"Regra: {payload['classificationRule']}")
    lines.append("")
    lines.append("## 3. Distribuição por número de instruções")
    lines.append("")
    lines.append("| Nº de instruções | Exercícios |")
    lines.append("|-----------------:|-----------:|")
    for row in payload["instructionCountDistribution"]:
        lines.append(f"| {row['instructionCount']} | {row['exerciseCount']} |")
    lines.append("")
    lines.append("## 4. 50 frases mais frequentes")
    lines.append("")
    lines.append("| Ocorrências | Classificação | Frase |")
    lines.append("|------------:|---------------|-------|")
    for row in payload["topInstructions"]:
        phrase = row["instruction"].replace("|", "\\|").replace("\n", " ")
        if len(phrase) > 160:
            phrase = phrase[:157] + "…"
        lines.append(f"| {row['count']} | {row['classification']} | {phrase} |")
    lines.append("")
    lines.append("## 5. Principais padrões (passthrough)")
    lines.append("")
    lines.append("| Prioridade | Ocorrências | Padrão |")
    lines.append("|------------|------------:|--------|")
    for row in payload["patterns"][:40]:
        pat = row["pattern"].replace("|", "\\|")
        lines.append(f"| {row['priority']} | {row['count']} | {pat} |")
    lines.append("")
    lines.append("### Verbos iniciais no passthrough")
    lines.append("")
    lines.append("| Verbo (início) | Ocorrências |")
    lines.append("|----------------|------------:|")
    for row in payload["leadingVerbsPassthrough"][:25]:
        lines.append(f"| {row['verb']} | {row['count']} |")
    lines.append("")
    lines.append("## 6. Verbos")
    lines.append("")
    lines.append("| Verbo | Ocorrências |")
    lines.append("|-------|------------:|")
    for row in payload["verbs"][:40]:
        lines.append(f"| {row['verb']} | {row['count']} |")
    lines.append("")
    lines.append("## 7. Anatomia")
    lines.append("")
    lines.append("| Termo (EN encontrado) | Ocorrências |")
    lines.append("|-----------------------|------------:|")
    for row in payload["anatomy"][:40]:
        lines.append(f"| {row['term']} | {row['count']} |")
    lines.append("")
    lines.append("## 8. Equipamentos")
    lines.append("")
    lines.append("| Termo | Ocorrências |")
    lines.append("|-------|------------:|")
    for row in payload["equipment"]:
        lines.append(f"| {row['term']} | {row['count']} |")
    lines.append("")
    lines.append("## 9. Termos técnicos")
    lines.append("")
    lines.append("| Termo | Ocorrências | Observação |")
    lines.append("|-------|------------:|------------|")
    for row in payload["technicalTerms"]:
        lines.append(f"| {row['term']} | {row['count']} | {row['note']} |")
    lines.append("")
    lines.append("## 10. Casos POTENTIAL_REVIEW")
    lines.append("")
    lines.append(f"Total: **{payload['potentialReviewCount']}**")
    lines.append("")
    by_motivo: dict[str, list] = defaultdict(list)
    for item in payload["potentialReview"]:
        by_motivo[item["motivo"]].append(item)
    for motivo in sorted(by_motivo.keys()):
        items = by_motivo[motivo]
        lines.append(f"### {motivo} ({len(items)})")
        lines.append("")
        lines.append("| externalId | idx | Instrução (trecho) |")
        lines.append("|------------|----:|--------------------|")
        for item in items[:30]:
            snippet = item["instruction"].replace("|", "\\|").replace("\n", " ")
            if len(snippet) > 100:
                snippet = snippet[:97] + "…"
            lines.append(
                f"| {item['externalId']} | {item['instructionIndex']} | {snippet} |"
            )
        if len(items) > 30:
            lines.append(f"| … | … | *+{len(items) - 30} casos* |")
        lines.append("")
    lines.append("## 11. Recomendações para a próxima etapa")
    lines.append("")
    lines.append("**Não traduzir nesta etapa.** Próximos passos sugeridos:")
    lines.append("")
    for rec in payload["recommendations"]:
        lines.append(f"### [{rec['priority']}] {rec['id']}")
        lines.append("")
        lines.append(rec["action"])
        lines.append("")
        if rec.get("examples"):
            lines.append("Exemplos:")
            lines.append("")
            for ex in rec["examples"][:8]:
                lines.append(f"- `{ex}`")
            lines.append("")
    lines.append("---")
    lines.append("")
    lines.append("## Integridade")
    lines.append("")
    lines.append("```text")
    lines.append("Instruções traduzidas nesta etapa: NÃO")
    lines.append("sourceData.instructions alterado: NÃO")
    lines.append("gymtrack-exercises.json alterado: NÃO")
    lines.append("translations.json alterado: NÃO")
    lines.append("Asset Android alterado: NÃO")
    lines.append("app/ alterado: NÃO")
    lines.append("Git commit criado: NÃO")
    lines.append("```")
    lines.append("")
    return "\n".join(lines)


def main() -> None:
    catalog = json.loads(NORMALIZED.read_text(encoding="utf-8"))
    catalog_sha = hashlib.sha256(NORMALIZED.read_bytes()).hexdigest()[:16]

    payload = build_audit(catalog)
    payload["sourceCatalogSha16"] = catalog_sha

    text = dumps_deterministic(payload)
    OUT_JSON.write_text(text, encoding="utf-8")
    OUT_MD.write_text(write_markdown(payload), encoding="utf-8")

    c = payload["classification"]
    summary = {
        "totalExercises": payload["totalExercises"],
        "withInstructions": payload["exercisesWithInstructions"],
        "withoutInstructions": payload["exercisesWithoutInstructions"],
        "totalInstructions": payload["totalInstructions"],
        "uniqueInstructions": payload["uniqueInstructions"],
        **c,
        "jsonSha16": hashlib.sha256(text.encode("utf-8")).hexdigest()[:16],
        "catalogSha16": catalog_sha,
    }
    print(json.dumps(summary, ensure_ascii=False))


if __name__ == "__main__":
    main()
