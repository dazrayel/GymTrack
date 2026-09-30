"""Normalize Free Exercise DB JSON into GymTrack intermediate format.

Does not touch the Android app or Room. source/externalId exist only in this JSON.

Usage (from repo root or this package):

    py -3 tools/exercise-import/normalize/normalize_free_exercise_db.py
"""

from __future__ import annotations

import argparse
import json
import sys
from collections import Counter, OrderedDict
from pathlib import Path
from typing import Any

from mappings import (
    EQUIPMENT_TYPE_MAP,
    GYMTRACK_EQUIPMENT_TYPES,
    GYMTRACK_MUSCLE_GROUPS,
    MULTIPLE_PRIMARY_RULE,
    MUSCLE_GROUP_MAP,
    SOURCE_NAME,
)

ROOT = Path(__file__).resolve().parents[1]
DEFAULT_INPUT = ROOT / "input" / "free-exercise-db" / "exercises.json"
DEFAULT_OUTPUT = ROOT / "output" / "free-exercise-db" / "gymtrack-exercises.json"
DEFAULT_REPORT = ROOT / "output" / "free-exercise-db" / "normalization-report.json"


def map_muscle(raw: Any) -> str | None:
    if not isinstance(raw, str):
        return None
    key = raw.strip().lower()
    if not key:
        return None
    mapped = MUSCLE_GROUP_MAP.get(key)
    if mapped is None:
        return None
    if mapped not in GYMTRACK_MUSCLE_GROUPS:
        raise RuntimeError(f"MUSCLE_GROUP_MAP target not in catalog: {mapped}")
    return mapped


def map_equipment(raw: Any) -> tuple[str | None, str]:
    """Return (gymtrack_value_or_none, status).

    status: mapped | null | unmapped
    """
    if raw is None:
        return None, "null"
    if not isinstance(raw, str):
        return None, "unmapped"
    key = raw.strip().lower()
    if not key:
        return None, "unmapped"
    mapped = EQUIPMENT_TYPE_MAP.get(key)
    if mapped is None:
        return None, "unmapped"
    if mapped not in GYMTRACK_EQUIPMENT_TYPES:
        raise RuntimeError(f"EQUIPMENT_TYPE_MAP target not in catalog: {mapped}")
    return mapped, "mapped"


def _dedupe_preserve_order(values: list[str]) -> tuple[list[str], bool]:
    seen: set[str] = set()
    out: list[str] = []
    had_dup = False
    for item in values:
        if item in seen:
            had_dup = True
            continue
        seen.add(item)
        out.append(item)
    return out, had_dup


def normalize_exercise(row: dict[str, Any]) -> tuple[dict[str, Any], dict[str, Any]]:
    """Return (record, event dict for the report)."""
    events: dict[str, Any] = {
        "errors": [],
        "warnings": [],
        "mapped_muscles_src": [],
        "unmapped_muscles_src": [],
        "mapped_equipment_src": None,
        "unmapped_equipment_src": None,
        "null_equipment": False,
        "multiple_primary": False,
        "primary_secondary_overlap": False,
        "duplicate_secondary": False,
        "empty_instructions": False,
        "empty_images": False,
        "failed": False,
    }

    issues: list[str] = []
    external_id = row.get("id")
    name = row.get("name")

    if not isinstance(external_id, str) or not external_id.strip():
        events["errors"].append("missing_or_invalid_id")
        events["failed"] = True
        issues.append("missing_or_invalid_id")
        external_id = external_id if isinstance(external_id, str) else ""

    if not isinstance(name, str) or not name.strip():
        events["errors"].append("missing_or_empty_name")
        events["failed"] = True
        issues.append("missing_or_empty_name")
        name = name if isinstance(name, str) else ""

    primaries_src = row.get("primaryMuscles")
    if not isinstance(primaries_src, list) or len(primaries_src) == 0:
        events["errors"].append("empty_primaryMuscles")
        events["failed"] = True
        issues.append("empty_primaryMuscles")
        primaries_src = primaries_src if isinstance(primaries_src, list) else []
    elif len(primaries_src) > 1:
        events["multiple_primary"] = True
        events["warnings"].append("multiple_primaryMuscles")
        issues.append("multiple_primaryMuscles")

    secondaries_src = row.get("secondaryMuscles")
    if secondaries_src is None:
        secondaries_src = []
    if not isinstance(secondaries_src, list):
        events["errors"].append("invalid_secondaryMuscles")
        events["failed"] = True
        issues.append("invalid_secondaryMuscles")
        secondaries_src = []

    overlap_src = [
        m
        for m in primaries_src
        if isinstance(m, str) and m in secondaries_src
    ]
    if overlap_src:
        events["primary_secondary_overlap"] = True
        events["warnings"].append("primary_secondary_overlap")
        issues.append("primary_secondary_overlap")

    mapped_primaries: list[tuple[str, str | None]] = []
    for src in primaries_src:
        mapped = map_muscle(src)
        src_key = src if isinstance(src, str) else str(src)
        if mapped is None:
            events["unmapped_muscles_src"].append(src_key)
            events["errors"].append(f"unmapped_primary:{src_key}")
            events["failed"] = True
            issues.append(f"unmapped_primary:{src_key}")
        else:
            events["mapped_muscles_src"].append(src_key)
        mapped_primaries.append((src_key, mapped))

    muscle_group: str | None = None
    extra_primary_mapped: list[str] = []
    if mapped_primaries:
        _, first_mapped = mapped_primaries[0]
        muscle_group = first_mapped
        for _, mapped in mapped_primaries[1:]:
            if mapped is not None:
                extra_primary_mapped.append(mapped)

    secondary_mapped: list[str] = []
    for src in secondaries_src:
        mapped = map_muscle(src)
        src_key = src if isinstance(src, str) else str(src)
        if mapped is None:
            events["unmapped_muscles_src"].append(src_key)
            events["errors"].append(f"unmapped_secondary:{src_key}")
            events["failed"] = True
            issues.append(f"unmapped_secondary:{src_key}")
            continue
        events["mapped_muscles_src"].append(src_key)
        secondary_mapped.append(mapped)

    combined_secondaries = extra_primary_mapped + secondary_mapped
    combined_secondaries, had_dup = _dedupe_preserve_order(combined_secondaries)
    if had_dup:
        events["duplicate_secondary"] = True
        events["warnings"].append("duplicate_secondary_after_normalization")
        issues.append("duplicate_secondary_after_normalization")

    if muscle_group is not None:
        filtered: list[str] = []
        dropped_overlap = False
        for item in combined_secondaries:
            if item == muscle_group:
                dropped_overlap = True
                continue
            filtered.append(item)
        combined_secondaries = filtered
        if dropped_overlap:
            events["primary_secondary_overlap"] = True
            if "primary_secondary_overlap" not in events["warnings"]:
                events["warnings"].append("primary_secondary_overlap")
            if "primary_secondary_overlap" not in issues:
                issues.append("primary_secondary_overlap")

    equipment_src = row.get("equipment") if "equipment" in row else None
    equipment_type, eq_status = map_equipment(equipment_src)
    if eq_status == "null":
        events["null_equipment"] = True
    elif eq_status == "mapped":
        events["mapped_equipment_src"] = equipment_src
    else:
        events["unmapped_equipment_src"] = equipment_src
        events["errors"].append(f"unmapped_equipment:{equipment_src}")
        events["failed"] = True
        issues.append(f"unmapped_equipment:{equipment_src}")

    instructions = row.get("instructions")
    images = row.get("images")
    if not isinstance(instructions, list) or len(instructions) == 0:
        events["empty_instructions"] = True
        events["warnings"].append("empty_instructions")
    if not isinstance(images, list) or len(images) == 0:
        events["empty_images"] = True
        events["warnings"].append("empty_images")

    source_data = {
        "force": row["force"] if "force" in row else None,
        "level": row["level"] if "level" in row else None,
        "mechanic": row["mechanic"] if "mechanic" in row else None,
        "equipment": row["equipment"] if "equipment" in row else None,
        "primaryMuscles": list(primaries_src),
        "secondaryMuscles": list(secondaries_src),
        "instructions": instructions if isinstance(instructions, list) else [],
        "category": row["category"] if "category" in row else None,
        "images": images if isinstance(images, list) else [],
    }

    record = {
        "source": SOURCE_NAME,
        "externalId": external_id,
        "name": name,
        "muscleGroup": muscle_group,
        "secondaryMuscles": combined_secondaries,
        "equipmentType": equipment_type,
        "issues": issues,
        "sourceData": source_data,
    }
    events["externalId"] = external_id
    events["name"] = name
    return record, events


def normalize_dataset(rows: list[Any]) -> tuple[list[dict[str, Any]], dict[str, Any]]:
    if not isinstance(rows, list):
        raise ValueError("input must be a JSON array")

    errors: list[dict[str, Any]] = []
    warnings: list[dict[str, Any]] = []
    seen_ids: OrderedDict[str, int] = OrderedDict()
    output: list[dict[str, Any]] = []

    mapped_muscles: Counter[str] = Counter()
    unmapped_muscles: Counter[str] = Counter()
    mapped_equipment: Counter[str] = Counter()
    unmapped_equipment: Counter[str] = Counter()
    original_muscles: Counter[str] = Counter()
    original_equipment: Counter[str] = Counter()
    final_muscle_groups: Counter[str] = Counter()
    final_equipment: Counter[str] = Counter()

    multiple_primary: list[dict[str, Any]] = []
    overlap: list[dict[str, Any]] = []
    dup_secondary: list[str] = []
    null_equipment_ids: list[str] = []
    empty_instructions_ids: list[str] = []
    empty_images_ids: list[str] = []
    failed_ids: list[str] = []

    for index, row in enumerate(rows):
        if not isinstance(row, dict):
            errors.append({"index": index, "error": "row_not_object"})
            failed_ids.append(f"index:{index}")
            output.append(
                {
                    "source": SOURCE_NAME,
                    "externalId": "",
                    "name": "",
                    "muscleGroup": None,
                    "secondaryMuscles": [],
                    "equipmentType": None,
                    "issues": ["row_not_object"],
                    "sourceData": {
                        "force": None,
                        "level": None,
                        "mechanic": None,
                        "equipment": None,
                        "primaryMuscles": [],
                        "secondaryMuscles": [],
                        "instructions": [],
                        "category": None,
                        "images": [],
                    },
                }
            )
            continue

        eid = row.get("id")
        if isinstance(eid, str):
            if eid in seen_ids:
                errors.append(
                    {
                        "externalId": eid,
                        "error": "duplicate_id",
                        "firstIndex": seen_ids[eid],
                        "index": index,
                    }
                )
            else:
                seen_ids[eid] = index

        for m in row.get("primaryMuscles") or []:
            if isinstance(m, str):
                original_muscles[m] += 1
        for m in row.get("secondaryMuscles") or []:
            if isinstance(m, str):
                original_muscles[m] += 1
        eq = row.get("equipment")
        original_equipment[str(eq) if eq is not None else "null"] += 1

        record, events = normalize_exercise(row)
        output.append(record)

        for src in events["mapped_muscles_src"]:
            mapped_muscles[src] += 1
        for src in events["unmapped_muscles_src"]:
            unmapped_muscles[src] += 1
        if events["mapped_equipment_src"] is not None:
            mapped_equipment[str(events["mapped_equipment_src"])] += 1
        if events["unmapped_equipment_src"] is not None:
            unmapped_equipment[str(events["unmapped_equipment_src"])] += 1

        mg = record["muscleGroup"]
        final_muscle_groups[mg if mg is not None else "null"] += 1
        et = record["equipmentType"]
        final_equipment[et if et is not None else "null"] += 1

        if events["multiple_primary"]:
            multiple_primary.append(
                {
                    "externalId": events["externalId"],
                    "name": events["name"],
                    "primaryMuscles": list(row.get("primaryMuscles") or []),
                    "rule": MULTIPLE_PRIMARY_RULE,
                    "muscleGroup": record["muscleGroup"],
                }
            )
        if events["primary_secondary_overlap"]:
            overlap.append(
                {
                    "externalId": events["externalId"],
                    "name": events["name"],
                    "primaryMuscles": list(row.get("primaryMuscles") or []),
                    "secondaryMuscles": list(row.get("secondaryMuscles") or []),
                }
            )
        if events["duplicate_secondary"]:
            dup_secondary.append(events["externalId"])
        if events["null_equipment"]:
            null_equipment_ids.append(events["externalId"])
        if events["empty_instructions"]:
            empty_instructions_ids.append(events["externalId"])
        if events["empty_images"]:
            empty_images_ids.append(events["externalId"])
        if events["failed"]:
            failed_ids.append(events["externalId"])
            for err in events["errors"]:
                errors.append({"externalId": events["externalId"], "error": err})
        for warn in events["warnings"]:
            warnings.append({"externalId": events["externalId"], "warning": warn})

    report = {
        "source": SOURCE_NAME,
        "multiplePrimaryRule": MULTIPLE_PRIMARY_RULE,
        "inputExerciseCount": len(rows),
        "outputExerciseCount": len(output),
        "failedExerciseCount": len(failed_ids),
        "mappedMuscles": dict(sorted(mapped_muscles.items())),
        "unmappedMuscles": dict(sorted(unmapped_muscles.items())),
        "mappedEquipment": dict(sorted(mapped_equipment.items())),
        "unmappedEquipment": dict(sorted(unmapped_equipment.items())),
        "nullEquipmentCount": len(null_equipment_ids),
        "nullEquipmentExternalIds": null_equipment_ids,
        "originalMuscleCounts": dict(sorted(original_muscles.items())),
        "originalEquipmentCounts": dict(sorted(original_equipment.items())),
        "finalMuscleGroupCounts": dict(sorted(final_muscle_groups.items())),
        "finalEquipmentTypeCounts": dict(sorted(final_equipment.items())),
        "multiplePrimaryMuscleExercises": multiple_primary,
        "primarySecondaryOverlapExercises": overlap,
        "duplicateSecondaryAfterNormalization": dup_secondary,
        "emptyInstructionsExternalIds": empty_instructions_ids,
        "emptyImagesExternalIds": empty_images_ids,
        "failedExternalIds": failed_ids,
        "warnings": warnings,
        "errors": errors,
    }
    return output, report


def write_json(path: Path, payload: Any) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    text = json.dumps(payload, ensure_ascii=False, indent=2) + "\n"
    path.write_text(text, encoding="utf-8")


def run(input_path: Path, output_path: Path, report_path: Path) -> dict[str, Any]:
    raw = json.loads(input_path.read_text(encoding="utf-8"))
    output, report = normalize_dataset(raw)
    write_json(output_path, output)
    write_json(report_path, report)
    return report


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description="Normalize Free Exercise DB for GymTrack")
    parser.add_argument("--input", type=Path, default=DEFAULT_INPUT)
    parser.add_argument("--output", type=Path, default=DEFAULT_OUTPUT)
    parser.add_argument("--report", type=Path, default=DEFAULT_REPORT)
    args = parser.parse_args(argv)
    report = run(args.input, args.output, args.report)
    print(
        f"input={report['inputExerciseCount']} "
        f"output={report['outputExerciseCount']} "
        f"failed={report['failedExerciseCount']}"
    )
    print(f"wrote {args.output}")
    print(f"wrote {args.report}")
    return 0


if __name__ == "__main__":
    # Allow `py normalize_free_exercise_db.py` from the normalize/ directory.
    sys.path.insert(0, str(Path(__file__).resolve().parent))
    raise SystemExit(main())
