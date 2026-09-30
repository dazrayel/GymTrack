"""Audit importable Free Exercise DB names for pt-BR translation (Etapa 4.3).

Does not modify gymtrack-exercises.json, Android assets, or the app.
"""

from __future__ import annotations

import json
import re
import sys
from collections import Counter, OrderedDict
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "analysis"))
sys.path.insert(0, str(ROOT / "curation"))
from catalog_paths import analysis_catalog_path  # noqa: E402
from classify_importability import classify as classify_importability

NORMALIZED = analysis_catalog_path()
GLOSSARY_PATH = ROOT / "translate" / "glossary.json"
OUT_JSON = ROOT / "analysis" / "translation-audit.json"
OUT_MD = ROOT / "analysis" / "TRANSLATION_AUDIT.md"

IMPORTABLE_LABELS = frozenset({"IMPORTABLE", "IMPORTABLE_WITH_FALLBACK"})
VALID_CLASSES = frozenset({"DIRECT", "CONTEXTUAL", "REVIEW"})
EXPECTED_IMPORTABLE = 777

CONTEXTUAL_MODIFIERS = frozenset(
    {
        "Caminhando",
        "Reverso",
        "Alternado",
        "no Chão",
        "por Trás da Nuca",
        "Alta",
        "Acima da Cabeça",
    }
)

PATTERN_SPECS: list[tuple[str, re.Pattern[str]]] = [
    ("Barbell + movimento", re.compile(r"\bBarbell\b", re.I)),
    ("Dumbbell + movimento", re.compile(r"\bDumbbell", re.I)),
    ("Kettlebell + movimento", re.compile(r"\bKettlebell", re.I)),
    ("Cable + movimento", re.compile(r"\bCable", re.I)),
    ("Machine + movimento", re.compile(r"\bMachine\b", re.I)),
    ("Smith + movimento", re.compile(r"\bSmith\b", re.I)),
    ("Band + movimento", re.compile(r"\bBands?\b", re.I)),
    ("Incline", re.compile(r"\bIncline\b", re.I)),
    ("Decline", re.compile(r"\bDecline\b", re.I)),
    ("Seated", re.compile(r"\bSeated\b", re.I)),
    ("Standing", re.compile(r"\bStanding\b", re.I)),
    ("Lying", re.compile(r"\bLying\b", re.I)),
    ("Kneeling", re.compile(r"\bKneeling\b", re.I)),
    ("Reverse", re.compile(r"\bReverse\b", re.I)),
    ("Walking", re.compile(r"\bWalking\b", re.I)),
    ("Alternating", re.compile(r"\bAlternat", re.I)),
    ("Single-Leg / One-Leg", re.compile(r"\b(Single-Leg|One-Leg)\b", re.I)),
    ("One-Arm / Single-Arm", re.compile(r"\b(One-Arm|Single-Arm|One Arm)\b", re.I)),
    ("Wide-Grip", re.compile(r"\bWide-Grip\b", re.I)),
    ("Close-Grip", re.compile(r"\bClose-Grip\b", re.I)),
    ("Pulldown (termo técnico)", re.compile(r"\bPulldowns?\b", re.I)),
    ("Pull-Up", re.compile(r"\bPull-Ups?\b|\bPull Ups\b", re.I)),
    ("Chin-Up", re.compile(r"\bChin-Ups?\b", re.I)),
    ("Face Pull", re.compile(r"\bFace Pull\b", re.I)),
    ("Press", re.compile(r"\bPress\b", re.I)),
    ("Curl", re.compile(r"\bCurls?\b", re.I)),
    ("Squat", re.compile(r"\bSquats?\b", re.I)),
    ("Deadlift", re.compile(r"\bDeadlift\b", re.I)),
    ("Row", re.compile(r"\bRows?\b", re.I)),
    ("Stretch", re.compile(r"\bStretch", re.I)),
    ("Olympic (Clean/Snatch/Jerk)", re.compile(r"\b(Clean|Snatch|Jerk)\b", re.I)),
]


def _load_glossary() -> dict:
    return json.loads(GLOSSARY_PATH.read_text(encoding="utf-8"))


def tokenize(name: str) -> list[str]:
    cleaned = name.replace("—", "-").replace("–", "-")
    parts = re.split(r"\s+", cleaned.strip())
    tokens: list[str] = []
    for part in parts:
        token = part.strip("()[],.:;\"'")
        if not token or token == "-":
            continue
        tokens.append(token.lower())
    return tokens


def _sorted_phrases(items: list[dict], key: str = "tokens") -> list[dict]:
    return sorted(items, key=lambda item: (-len(item[key]), tuple(item[key])))


def _find_phrase(tokens: list[str], phrase: list[str]) -> int:
    n = len(phrase)
    if n == 0 or n > len(tokens):
        return -1
    for i in range(0, len(tokens) - n + 1):
        if tokens[i : i + n] == phrase:
            return i
    return -1


def _consume(tokens: list[str], phrases: list[dict], value_key: str) -> tuple[list[str], list[dict]]:
    remaining = list(tokens)
    matched: list[dict] = []
    changed = True
    while changed:
        changed = False
        for item in phrases:
            idx = _find_phrase(remaining, item["tokens"])
            if idx < 0:
                continue
            n = len(item["tokens"])
            remaining = remaining[:idx] + remaining[idx + n :]
            matched.append(item)
            changed = True
            break
    return remaining, matched


def audit_name(original_name: str, glossary: dict, muscle_group: str | None = None) -> dict:
    tokens = tokenize(original_name)
    cores = _sorted_phrases(glossary["coreMovements"])
    equipment = _sorted_phrases(glossary["equipment"])
    modifiers = _sorted_phrases(glossary["modifiers"])
    stopwords = set(glossary["stopwords"])

    remaining, core_hits = _consume(tokens, cores, "pt")
    remaining, eq_hits = _consume(remaining, equipment, "pt")
    remaining, mod_hits = _consume(remaining, modifiers, "pt")

    leftover = [t for t in remaining if t not in stopwords and t != ""]
    leftover = [t for t in leftover if not re.fullmatch(r"\d+(?:/\d+)?", t)]

    matched_terms: list[str] = []
    for hit in core_hits + eq_hits + mod_hits:
        label = " ".join(hit["tokens"])
        if label not in matched_terms:
            matched_terms.append(label)

    core_pt = [h["pt"] for h in core_hits if h.get("pt")]
    muscle = (muscle_group or "").strip()
    remapped_press = False
    if core_pt == ["Press"] or (core_pt and core_pt[-1] == "Press" and "Supino" not in core_pt and "Desenvolvimento" not in " ".join(core_pt)):
        replacement = None
        if muscle == "Peitoral":
            replacement = "Supino"
        elif muscle == "Ombros":
            replacement = "Desenvolvimento"
        elif muscle == "Tríceps":
            replacement = "Extensão de Tríceps"
        if replacement:
            core_pt = [replacement if p == "Press" else p for p in core_pt]
            remapped_press = True

    eq_pt = [h["pt"] for h in eq_hits if h.get("pt")]
    mod_pt = [h["pt"] for h in mod_hits if h.get("pt")]

    seen_mod: set[str] = set()
    mods_unique: list[str] = []
    for m in mod_pt:
        if m and m not in seen_mod:
            seen_mod.add(m)
            mods_unique.append(m)

    seen_eq: set[str] = set()
    eq_unique: list[str] = []
    for e in eq_pt:
        if e and e not in seen_eq:
            seen_eq.add(e)
            eq_unique.append(e)

    parts = core_pt + mods_unique
    for e in eq_unique:
        already = any(e.lower() in p.lower() or p.lower() in e.lower() for p in parts)
        if not already:
            parts.append(e)
    suggested = " ".join(p for p in parts if p).strip()
    suggested = re.sub(r"\s+", " ", suggested)

    pulldown_in_name = bool(re.search(r"\bpulldowns?\b", original_name, re.I))
    if pulldown_in_name and re.search(r"\bpuxada\b", suggested, re.I):
        raise RuntimeError(f"Pulldown translated as Puxada: {original_name!r} -> {suggested!r}")

    hints = [h.get("classHint", "DIRECT") for h in core_hits]
    has_review_core = any(h == "REVIEW" for h in hints)
    has_contextual_core = any(h == "CONTEXTUAL" for h in hints) or remapped_press
    has_contextual_mod = any(m in CONTEXTUAL_MODIFIERS for m in mods_unique)

    if not core_hits or has_review_core or leftover or not suggested:
        classification = "REVIEW"
        if not suggested:
            suggested = original_name
        reason = "Requer revisão humana: nome incompleto no glossário, termo ambíguo ou nomenclatura pouco consolidada."
        if leftover:
            reason = (
                "Requer revisão humana. Tokens não cobertos pelo glossário: "
                + ", ".join(leftover)
                + "."
            )
        if pulldown_in_name:
            reason += " Pulldown permanece termo técnico (não traduzir como Puxada)."
        if leftover:
            suggested = (suggested + " [" + " ".join(leftover) + "]").strip()
    elif has_contextual_core or has_contextual_mod:
        classification = "CONTEXTUAL"
        reason = "Tradução depende do movimento/modificador completo, não de palavras isoladas."
    else:
        classification = "DIRECT"
        reason = "Nomenclatura fitness coberta pelo glossário e pelo nome completo."

    if classification in {"DIRECT", "CONTEXTUAL"} and not suggested:
        raise RuntimeError(f"Empty suggestedName for {classification}: {original_name}")

    return {
        "suggestedName": suggested,
        "classification": classification,
        "reason": reason,
        "matchedGlossaryTerms": matched_terms,
        "uncoveredTokens": leftover,
    }


def importable_rows(catalog: list[dict]) -> list[dict]:
    rows = [row for row in catalog if classify_importability(row) in IMPORTABLE_LABELS]
    rows.sort(key=lambda row: (row.get("source") or "", row.get("externalId") or ""))
    return rows


def rejected_ids(catalog: list[dict]) -> set[tuple[str, str]]:
    out: set[tuple[str, str]] = set()
    for row in catalog:
        if classify_importability(row) == "REJECTED":
            out.add((row.get("source") or "", row.get("externalId") or ""))
    return out


def validate(catalog: list[dict], exercises: list[dict]) -> None:
    if len(exercises) != EXPECTED_IMPORTABLE:
        raise SystemExit(f"totalImportable != {EXPECTED_IMPORTABLE}: {len(exercises)}")
    seen: set[tuple[str, str]] = set()
    for item in exercises:
        key = (item["source"], item["externalId"])
        if key in seen:
            raise SystemExit(f"duplicate identity: {key}")
        seen.add(key)
        if item["classification"] not in VALID_CLASSES:
            raise SystemExit(f"invalid classification: {item}")
        if item["classification"] in {"DIRECT", "CONTEXTUAL"} and not item["suggestedName"].strip():
            raise SystemExit(f"empty suggestedName: {item['externalId']}")
        if re.search(r"\bpuxada\b", item["suggestedName"], re.I) and re.search(
            r"\bpulldown", item["originalName"], re.I
        ):
            raise SystemExit(f"Pulldown became Puxada: {item['externalId']}")

    expected = {(r.get("source") or "", r.get("externalId") or "") for r in importable_rows(catalog)}
    missing = expected - seen
    extra = seen - expected
    if missing:
        raise SystemExit(f"missing importable: {sorted(missing)[:5]} count={len(missing)}")
    if extra:
        raise SystemExit(f"unexpected identities: {sorted(extra)[:5]} count={len(extra)}")

    rejected = rejected_ids(catalog)
    leaked = seen & rejected
    if leaked:
        raise SystemExit(f"rejected included: {sorted(leaked)[:5]}")

    by_id = {(r.get("source") or "", r.get("externalId") or ""): r for r in catalog}
    for item in exercises:
        origin = by_id[(item["source"], item["externalId"])]
        if origin.get("externalId") != item["externalId"]:
            raise SystemExit("externalId altered")
        if origin.get("name") != item["originalName"]:
            raise SystemExit(f"originalName mismatch: {item['externalId']}")


def pattern_counts(names: list[str]) -> list[dict]:
    counts: list[dict] = []
    for label, regex in PATTERN_SPECS:
        n = sum(1 for name in names if regex.search(name))
        counts.append({"pattern": label, "count": n})
    counts.sort(key=lambda row: (-row["count"], row["pattern"]))
    return counts


def token_frequency(names: list[str]) -> list[dict]:
    counter: Counter[str] = Counter()
    for name in names:
        for token in tokenize(name):
            counter[token] += 1
    return [{"token": token, "count": count} for token, count in counter.most_common(80)]


def build_report(catalog: list[dict], glossary: dict) -> dict:
    rows = importable_rows(catalog)
    exercises: list[dict] = []
    class_counts: Counter[str] = Counter()
    uncovered: Counter[str] = Counter()

    for row in rows:
        audit = audit_name(row["name"], glossary, row.get("muscleGroup"))
        class_counts[audit["classification"]] += 1
        for token in audit["uncoveredTokens"]:
            uncovered[token] += 1
        exercises.append(
            OrderedDict(
                [
                    ("source", row.get("source") or "free-exercise-db"),
                    ("externalId", row["externalId"]),
                    ("originalName", row["name"]),
                    ("classification", audit["classification"]),
                    ("suggestedName", audit["suggestedName"]),
                    ("reason", audit["reason"]),
                    ("matchedGlossaryTerms", audit["matchedGlossaryTerms"]),
                    ("uncoveredTokens", audit["uncoveredTokens"]),
                ]
            )
        )

    names = [row["name"] for row in rows]
    payload = OrderedDict(
        [
            ("source", "free-exercise-db"),
            ("totalImportable", len(exercises)),
            (
                "classificationCounts",
                OrderedDict(
                    [
                        ("DIRECT", int(class_counts.get("DIRECT", 0))),
                        ("CONTEXTUAL", int(class_counts.get("CONTEXTUAL", 0))),
                        ("REVIEW", int(class_counts.get("REVIEW", 0))),
                    ]
                ),
            ),
            ("patterns", pattern_counts(names)),
            (
                "uncoveredTokenFrequency",
                [
                    {"token": token, "count": count}
                    for token, count in uncovered.most_common(60)
                ],
            ),
            ("frequentOriginalTokens", token_frequency(names)),
            ("exercises", exercises),
        ]
    )
    validate(catalog, exercises)
    return payload


def write_markdown(payload: dict) -> None:
    counts = payload["classificationCounts"]
    review = [e for e in payload["exercises"] if e["classification"] == "REVIEW"]
    pulldowns = [
        e
        for e in payload["exercises"]
        if re.search(r"\bpulldowns?\b", e["originalName"], re.I)
    ]
    lines = [
        "# Auditoria de tradução pt-BR (Etapa 4.3)",
        "",
        "Somente análise. Não altera `gymtrack-exercises.json`, o asset Android, Room nem o importador.",
        "",
        "## Totais",
        "",
        f"- Importáveis auditados: **{payload['totalImportable']}**",
        f"- DIRECT: **{counts['DIRECT']}**",
        f"- CONTEXTUAL: **{counts['CONTEXTUAL']}**",
        f"- REVIEW: **{counts['REVIEW']}**",
        f"- Soma: **{counts['DIRECT'] + counts['CONTEXTUAL'] + counts['REVIEW']}**",
        "",
        "## Pulldown",
        "",
        "Termo técnico; **não** traduzido como Puxada.",
        "",
        f"Ocorrências com Pulldown no nome original: **{len(pulldowns)}**.",
        "",
    ]
    for item in pulldowns:
        lines.append(
            f"- `{item['externalId']}`: {item['originalName']} → {item['suggestedName']} ({item['classification']})"
        )
    lines.extend(["", "## Padrões recorrentes", ""])
    lines.append("| Padrão | Quantidade |")
    lines.append("| --- | ---: |")
    for row in payload["patterns"]:
        lines.append(f"| {row['pattern']} | {row['count']} |")
    lines.extend(["", "## Tokens não cobertos (frequência)", ""])
    if not payload["uncoveredTokenFrequency"]:
        lines.append("Nenhum token residual.")
    else:
        lines.append("| Token | Quantidade |")
        lines.append("| --- | ---: |")
        for row in payload["uncoveredTokenFrequency"]:
            lines.append(f"| `{row['token']}` | {row['count']} |")
    lines.extend(["", "## Casos REVIEW (lista completa)", ""])
    lines.append(f"Total REVIEW: **{len(review)}**.")
    lines.append("")
    for item in review:
        extra = ", ".join(item.get("uncoveredTokens") or []) or "—"
        lines.append(
            f"- `{item['externalId']}` — {item['originalName']} → _{item['suggestedName']}_ (tokens: {extra})"
        )
    lines.append("")
    OUT_MD.write_text("\n".join(lines), encoding="utf-8")


def dumps_deterministic(payload: dict) -> str:
    return json.dumps(payload, ensure_ascii=False, indent=2) + "\n"


def main() -> None:
    catalog = json.loads(NORMALIZED.read_text(encoding="utf-8"))
    glossary = _load_glossary()
    payload = build_report(catalog, glossary)
    text = dumps_deterministic(payload)
    OUT_JSON.write_text(text, encoding="utf-8")
    write_markdown(payload)
    counts = payload["classificationCounts"]
    print(
        json.dumps(
            {
                "totalImportable": payload["totalImportable"],
                "DIRECT": counts["DIRECT"],
                "CONTEXTUAL": counts["CONTEXTUAL"],
                "REVIEW": counts["REVIEW"],
            },
            ensure_ascii=False,
        )
    )


if __name__ == "__main__":
    main()
