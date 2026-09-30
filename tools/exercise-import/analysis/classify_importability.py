"""Classify normalized Free Exercise DB rows for GymTrack import policy (Etapa 2.5).

Does not rewrite gymtrack-exercises.json. Writes analysis/importability-stats.json.
"""

from __future__ import annotations

import json
import sys
from collections import Counter
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "curation"))
from catalog_paths import analysis_catalog_path  # noqa: E402

NORMALIZED = analysis_catalog_path()
OUT = Path(__file__).resolve().parent / "importability-stats.json"

UNMAPPED_MUSCLES = frozenset({"neck", "abductors", "adductors"})
EQUIPMENT_FALLBACK_OUTRO = frozenset({"medicine ball", "exercise ball", "foam roll"})
EQUIPMENT_FALLBACK_BARRA = frozenset({"e-z curl bar"})


def _src_list(values) -> list[str]:
    if not isinstance(values, list):
        return []
    return [v for v in values if isinstance(v, str)]


def classify(row: dict) -> str:
    src = row.get("sourceData") or {}
    primaries = _src_list(src.get("primaryMuscles"))
    secondaries = _src_list(src.get("secondaryMuscles"))
    equipment = src.get("equipment")

    unmapped_primary = [m for m in primaries if m in UNMAPPED_MUSCLES]
    unmapped_secondary = [m for m in secondaries if m in UNMAPPED_MUSCLES]

    if unmapped_primary:
        return "REJECTED"
    if equipment is None:
        return "REJECTED"
    if row.get("muscleGroup") is None:
        return "REJECTED"

    fallback_eq = False
    if equipment in EQUIPMENT_FALLBACK_OUTRO or equipment in EQUIPMENT_FALLBACK_BARRA:
        fallback_eq = True

    multiple_primary = len(primaries) > 1
    lost_secondary = bool(unmapped_secondary)

    if fallback_eq or multiple_primary or lost_secondary:
        return "IMPORTABLE_WITH_FALLBACK"
    return "IMPORTABLE"


def overlap_kind(row: dict, muscle_map: dict[str, str]) -> str | None:
    src = row.get("sourceData") or {}
    primaries = _src_list(src.get("primaryMuscles"))
    secondaries = _src_list(src.get("secondaryMuscles"))
    raw = set(primaries) & set(secondaries)

    def mapped(ms: list[str]) -> list[str]:
        out = []
        for m in ms:
            g = muscle_map.get(m.lower())
            if g:
                out.append(g)
        return out

    p_m = mapped(primaries)
    s_m = mapped(secondaries)
    if not p_m:
        return None
    primary_group = p_m[0]
    if raw:
        return "source_string_overlap"
    if primary_group in s_m:
        return "canonical_collapse"
    return None


def main() -> None:
    import sys

    sys.path.insert(0, str(ROOT / "normalize"))
    from mappings import MUSCLE_GROUP_MAP

    rows = json.loads(NORMALIZED.read_text(encoding="utf-8"))
    counts: Counter[str] = Counter()
    overlap_kinds: Counter[str] = Counter()
    rejected_reasons: Counter[str] = Counter()
    fallback_reasons: Counter[str] = Counter()

    for row in rows:
        label = classify(row)
        counts[label] += 1
        src = row.get("sourceData") or {}
        primaries = _src_list(src.get("primaryMuscles"))
        secondaries = _src_list(src.get("secondaryMuscles"))
        equipment = src.get("equipment")
        if label == "REJECTED":
            if any(m in UNMAPPED_MUSCLES for m in primaries):
                rejected_reasons["unmapped_primary"] += 1
            elif equipment is None:
                rejected_reasons["equipment_null"] += 1
            else:
                rejected_reasons["other"] += 1
        if label == "IMPORTABLE_WITH_FALLBACK":
            if equipment in EQUIPMENT_FALLBACK_OUTRO:
                fallback_reasons["equipment_outro"] += 1
            if equipment in EQUIPMENT_FALLBACK_BARRA:
                fallback_reasons["equipment_barra_ez"] += 1
            if any(m in UNMAPPED_MUSCLES for m in secondaries):
                fallback_reasons["dropped_unmapped_secondary"] += 1
            if len(primaries) > 1:
                fallback_reasons["multiple_primary"] += 1

        kind = overlap_kind(row, MUSCLE_GROUP_MAP)
        if kind:
            overlap_kinds[kind] += 1

    payload = {
        "total": len(rows),
        "classification": dict(counts),
        "sum": sum(counts.values()),
        "rejectedReasons": dict(rejected_reasons),
        "fallbackReasons": dict(fallback_reasons),
        "overlapKindsAmongAllRows": dict(overlap_kinds),
        "policy": {
            "REJECTED": [
                "unmapped primary (neck, abductors, adductors)",
                "source equipment is null",
            ],
            "IMPORTABLE_WITH_FALLBACK": [
                "medicine ball / exercise ball / foam roll → Outro (importer)",
                "e-z curl bar → Barra (importer)",
                "unmapped secondary dropped",
                "multiple primary reduced",
            ],
            "IMPORTABLE": "mapped muscleGroup + mapped equipmentType, no dropped muscles",
            "REQUIRES_REVIEW": "unused under this policy (count 0)",
        },
    }
    OUT.write_text(json.dumps(payload, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(payload["classification"], ensure_ascii=False))
    print("sum", payload["sum"])


if __name__ == "__main__":
    main()
