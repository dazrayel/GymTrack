"""Etapa 4.5 — Deterministic main catalog curation (~100–150 exercises).

Does NOT modify gymtrack-exercises.json, translations.json, Room, or Android logic.
Outputs analysis/MAIN_CATALOG.json (+ drives image audit/copy).
"""

from __future__ import annotations

import hashlib
import json
import sys
from dataclasses import dataclass
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
REPO = ROOT.parents[1]
sys.path.insert(0, str(Path(__file__).resolve().parent))
sys.path.insert(0, str(ROOT / "analysis"))

from classify_importability import classify as classify_importability  # noqa: E402

from catalog_paths import analysis_catalog_path  # noqa: E402

NORMALIZED = analysis_catalog_path()
TRANSLATIONS = ROOT / "translate" / "translations.json"
OUT_JSON = ROOT / "analysis" / "MAIN_CATALOG.json"

IMPORTABLE = frozenset({"IMPORTABLE", "IMPORTABLE_WITH_FALLBACK"})
FILL_CATEGORIES = frozenset({"strength", "powerlifting"})
FILL_ID_FRAGMENTS_BLOCK = (
    "Stretch",
    "_SMR",
    "Balance_Board",
    "Around_The_Worlds",
    "Downward_Facing",
    "Atlas_Stone",
    "Backward_Drag",
    "Anterior_Tibialis",
)
TARGET_MIN = 102
TARGET_MAX = 130

# Ordered canonical slots: (slot_id, externalId)
# Covers movement patterns from Etapa 4.5 spec; first match wins on dedupe.
CANONICAL_SLOTS: list[tuple[str, str]] = [
    # Peito
    ("chest_flat_barbell", "Barbell_Bench_Press_-_Medium_Grip"),
    ("chest_incline_db", "Incline_Dumbbell_Press"),
    ("chest_incline_barbell", "Barbell_Incline_Bench_Press_-_Medium_Grip"),
    ("chest_decline_barbell", "Decline_Barbell_Bench_Press"),
    ("chest_fly_db", "Dumbbell_Flyes"),
    ("chest_cable_crossover", "Cable_Crossover"),
    ("chest_machine", "Machine_Bench_Press"),
    ("chest_bodyweight", "Pushups"),
    ("chest_dips", "Dips_-_Chest_Version"),
    # Costas
    ("back_pulldown", "Wide-Grip_Lat_Pulldown"),
    ("back_pullup", "Pullups"),
    ("back_chinup", "Chin-Up"),
    ("back_row_barbell", "Bent_Over_Barbell_Row"),
    ("back_row_db_unilateral", "One-Arm_Dumbbell_Row"),
    ("back_row_cable", "Seated_Cable_Rows"),
    ("back_row_tbar", "T-Bar_Row_with_Handle"),
    ("back_face_pull", "Face_Pull"),
    # Ombros
    ("shoulder_press_barbell", "Barbell_Shoulder_Press"),
    ("shoulder_press_db", "Dumbbell_Shoulder_Press"),
    ("shoulder_lateral", "Side_Lateral_Raise"),
    ("shoulder_rear_delt", "Bent_Over_Dumbbell_Rear_Delt_Raise_With_Head_On_Bench"),
    ("shoulder_upright_row", "Upright_Barbell_Row"),
    # Bíceps
    ("biceps_barbell", "Barbell_Curl"),
    ("biceps_db", "Dumbbell_Bicep_Curl"),
    ("biceps_hammer", "Alternate_Hammer_Curl"),
    ("biceps_preacher", "Machine_Preacher_Curls"),
    # Tríceps
    ("triceps_pushdown", "Triceps_Pushdown"),
    ("triceps_overhead_cable", "Cable_Rope_Overhead_Triceps_Extension"),
    ("triceps_dips", "Dips_-_Triceps_Version"),
    ("triceps_close_grip_bench", "Close-Grip_Barbell_Bench_Press"),
    ("triceps_skull", "EZ-Bar_Skullcrusher"),
    # Quadríceps
    ("quad_squat", "Barbell_Squat"),
    ("quad_front_squat", "Front_Squat_Clean_Grip"),
    ("quad_leg_press", "Leg_Press"),
    ("quad_leg_extension", "Leg_Extensions"),
    ("quad_lunge", "Dumbbell_Lunges"),
    ("quad_hack", "Hack_Squat"),
    ("quad_goblet", "Goblet_Squat"),
    ("quad_step_up", "Barbell_Step_Ups"),
    # Posteriores / hinge
    ("ham_romanian", "Romanian_Deadlift"),
    ("ham_leg_curl", "Lying_Leg_Curls"),
    ("ham_stiff_deadlift", "Stiff-Legged_Barbell_Deadlift"),
    ("ham_good_morning", "Good_Morning"),
    ("ham_deadlift", "Barbell_Deadlift"),
    # Glúteos
    ("glute_hip_thrust", "Barbell_Hip_Thrust"),
    ("glute_bridge", "Barbell_Glute_Bridge"),
    ("glute_cable_kickback", "One-Legged_Cable_Kickback"),
    ("glute_pull_through", "Pull_Through"),
    # Panturrilhas
    ("calf_standing", "Standing_Calf_Raises"),
    ("calf_seated", "Seated_Calf_Raise"),
    ("calf_donkey", "Donkey_Calf_Raises"),
    # Abdômen
    ("abs_crunch", "Crunches"),
    ("abs_plank", "Plank"),
    ("abs_hanging_leg_raise", "Hanging_Leg_Raise"),
    ("abs_russian_twist", "Russian_Twist"),
    ("abs_cable_crunch", "Cable_Crunch"),
    ("abs_ab_roller", "Ab_Roller"),
    ("abs_cross_body", "Cross-Body_Crunch"),
    # Compostos / utilidade extra
    ("full_clean", "Power_Clean"),
    ("full_kettlebell_thruster", "Kettlebell_Thruster"),
    ("full_farmers_walk", "Farmers_Walk"),
    ("full_lat_pulldown_underhand", "Underhand_Cable_Pulldowns"),
    ("full_chest_supported_row", "Smith_Machine_Bent_Over_Row"),
    ("full_pec_deck_fly", "Reverse_Flyes"),
    ("full_shrug", "Barbell_Shrug"),
    ("full_wrist_curl", "Seated_Palm-Up_Barbell_Wrist_Curl"),
    ("full_hyperextension", "Hyperextensions_Back_Extensions"),
    ("full_side_plank", "Side_Bridge"),
    ("full_wood_chop", "Standing_Cable_Wood_Chop"),
]

# Minimum coverage per muscle group (canonical GymTrack muscleGroup values)
MUSCLE_MINIMUMS: dict[str, int] = {
    "Peitoral": 6,
    "Costas": 6,
    "Ombros": 5,
    "Bíceps": 3,
    "Tríceps": 4,
    "Quadríceps": 6,
    "Posteriores": 4,
    "Glúteos": 3,
    "Panturrilhas": 2,
    "Abdômen": 5,
}

# Soft caps so general fill does not flood one muscle (deterministic).
MUSCLE_MAXIMUMS: dict[str, int] = {
    "Peitoral": 13,
    "Costas": 13,
    "Ombros": 10,
    "Bíceps": 6,
    "Tríceps": 8,
    "Quadríceps": 13,
    "Posteriores": 8,
    "Glúteos": 8,
    "Panturrilhas": 5,
    "Abdômen": 10,
    "Lombar": 3,
    "Trapézio": 2,
    "Antebraço": 3,
}

DEFAULT_MUSCLE_MAX = 8

# Fillers: prefer these equipment diversity per muscle when below minimum
FILLER_PRIORITY: dict[str, list[str]] = {
    "Peitoral": ["Cabos", "Máquina", "Halteres", "Barra", "Peso corporal"],
    "Costas": ["Cabos", "Barra", "Halteres", "Peso corporal", "Máquina"],
    "Ombros": ["Halteres", "Barra", "Cabos", "Máquina"],
    "Bíceps": ["Barra", "Halteres", "Máquina", "Cabos"],
    "Tríceps": ["Cabos", "Barra", "Peso corporal", "Máquina"],
    "Quadríceps": ["Barra", "Máquina", "Halteres", "Peso corporal"],
    "Posteriores": ["Barra", "Máquina", "Peso corporal"],
    "Glúteos": ["Barra", "Cabos", "Halteres"],
    "Panturrilhas": ["Máquina", "Barra", "Peso corporal"],
    "Abdômen": ["Peso corporal", "Cabos", "Máquina", "Outro"],
}


@dataclass(frozen=True)
class ExerciseRow:
    source: str
    external_id: str
    name: str
    muscle_group: str | None
    equipment_type: str | None
    image_paths: tuple[str, ...]
    translation_name: str
    name_status: str
    category: str | None


def _load_importable() -> list[dict]:
    catalog = json.loads(NORMALIZED.read_text(encoding="utf-8"))
    rows = [r for r in catalog if classify_importability(r) in IMPORTABLE]
    rows.sort(key=lambda r: (r.get("source") or "", r.get("externalId") or ""))
    if len(rows) != 777:
        raise AssertionError(f"Expected 777 importable, got {len(rows)}")
    return rows


def _load_translations() -> dict[tuple[str, str], dict]:
    data = json.loads(TRANSLATIONS.read_text(encoding="utf-8"))
    out: dict[tuple[str, str], dict] = {}
    for t in data["translations"]:
        key = (t.get("source") or "free-exercise-db", t["externalId"])
        out[key] = t
    return out


def _row_model(raw: dict, trans: dict[tuple[str, str], dict]) -> ExerciseRow:
    source = raw.get("source") or "free-exercise-db"
    eid = raw["externalId"]
    t = trans.get((source, eid), {})
    images = tuple((raw.get("sourceData") or {}).get("images") or [])
    return ExerciseRow(
        source=source,
        external_id=eid,
        name=raw.get("name") or "",
        muscle_group=raw.get("muscleGroup"),
        equipment_type=raw.get("equipmentType"),
        image_paths=images,
        translation_name=t.get("name") or raw.get("name") or "",
        name_status=t.get("status") or "UNKNOWN",
        category=(raw.get("sourceData") or {}).get("category"),
    )


def _image_count(row: ExerciseRow) -> int:
    return len(row.image_paths)


def _fill_eligible(row: ExerciseRow) -> bool:
    if row.category not in FILL_CATEGORIES:
        return False
    eid = row.external_id
    return not any(frag in eid for frag in FILL_ID_FRAGMENTS_BLOCK)


def _filler_score(row: ExerciseRow, muscle: str) -> tuple:
    """Deterministic sort key: prefer 2 images, then equipment priority, then name."""
    eq_order = FILLER_PRIORITY.get(muscle, [])
    eq_rank = eq_order.index(row.equipment_type) if row.equipment_type in eq_order else 99
    return (-_image_count(row), eq_rank, row.external_id)


def _muscle_counts(selected_ids: list[str], models: dict[str, ExerciseRow]) -> dict[str, int]:
    counts: dict[str, int] = {}
    for eid in selected_ids:
        mg = models[eid].muscle_group or ""
        counts[mg] = counts.get(mg, 0) + 1
    return counts


def _muscle_max(muscle: str) -> int:
    return MUSCLE_MAXIMUMS.get(muscle, DEFAULT_MUSCLE_MAX)


def _general_top_up_score(row: ExerciseRow, counts: dict[str, int]) -> tuple:
    mg = row.muscle_group or ""
    current = counts.get(mg, 0)
    outlier = 1 if row.equipment_type == "Outro" else 0
    return (current, outlier, -_image_count(row), row.external_id)


def curate() -> dict[str, Any]:
    raw_rows = _load_importable()
    by_id = {r["externalId"]: r for r in raw_rows}
    trans = _load_translations()

    selected_slots: list[dict[str, str]] = []
    selected_ids: list[str] = []
    seen: set[str] = set()

    for slot_id, eid in CANONICAL_SLOTS:
        if eid in seen:
            continue
        if eid not in by_id:
            raise KeyError(f"Canonical slot {slot_id} references missing externalId: {eid}")
        seen.add(eid)
        selected_ids.append(eid)
        selected_slots.append({"slot": slot_id, "externalId": eid})

    models = {r["externalId"]: _row_model(r, trans) for r in raw_rows}
    selected_set = set(selected_ids)

    # Fill muscle minimums
    for muscle, minimum in sorted(MUSCLE_MINIMUMS.items()):
        current = sum(1 for eid in selected_ids if models[eid].muscle_group == muscle)
        if current >= minimum:
            continue
        candidates = [
            m for eid, m in models.items()
            if eid not in selected_set and m.muscle_group == muscle and _fill_eligible(m)
        ]
        candidates.sort(key=lambda m: _filler_score(m, muscle))
        for m in candidates:
            if current >= minimum:
                break
            if len(selected_ids) >= TARGET_MAX:
                break
            selected_ids.append(m.external_id)
            selected_set.add(m.external_id)
            selected_slots.append({"slot": f"fill_{muscle}_{current}", "externalId": m.external_id})
            current += 1

    # Top up toward TARGET_MIN: balance muscle groups, then prefer 2 images
    while len(selected_ids) < TARGET_MIN:
        counts = _muscle_counts(selected_ids, models)
        candidates = [
            m for eid, m in models.items()
            if eid not in selected_set
            and _fill_eligible(m)
            and counts.get(m.muscle_group or "", 0) < _muscle_max(m.muscle_group or "")
        ]
        if not candidates:
            break
        candidates.sort(key=lambda m: _general_top_up_score(m, counts))
        m = candidates[0]
        selected_ids.append(m.external_id)
        selected_set.add(m.external_id)
        selected_slots.append({"slot": "fill_general", "externalId": m.external_id})
        if len(selected_ids) >= TARGET_MAX:
            break

    # If caps block reaching TARGET_MIN, fill only among muscles still under their soft cap.
    while len(selected_ids) < TARGET_MIN:
        counts = _muscle_counts(selected_ids, models)
        candidates = [
            m for eid, m in models.items()
            if eid not in selected_set
            and _fill_eligible(m)
            and counts.get(m.muscle_group or "", 0) < _muscle_max(m.muscle_group or "")
        ]
        if not candidates:
            break
        candidates.sort(key=lambda m: _general_top_up_score(m, counts))
        m = candidates[0]
        selected_ids.append(m.external_id)
        selected_set.add(m.external_id)
        selected_slots.append({"slot": "fill_overflow", "externalId": m.external_id})
        if len(selected_ids) >= TARGET_MAX:
            break

    if len(selected_ids) > TARGET_MAX:
        raise AssertionError(f"Selection {len(selected_ids)} exceeds TARGET_MAX {TARGET_MAX}")

    selected_ids.sort()
    selected_entries = []
    for eid in selected_ids:
        m = models[eid]
        slot = next((s["slot"] for s in selected_slots if s["externalId"] == eid), "unknown")
        selected_entries.append({
            "slot": slot,
            "source": m.source,
            "externalId": m.external_id,
            "originalName": m.name,
            "translatedName": m.translation_name,
            "nameStatus": m.name_status,
            "muscleGroup": m.muscle_group,
            "equipmentType": m.equipment_type,
            "imagePaths": list(m.image_paths),
            "imageCount": _image_count(m),
        })

    audit_matrix = []
    for r in raw_rows:
        eid = r["externalId"]
        m = models[eid]
        audit_matrix.append({
            "source": m.source,
            "externalId": eid,
            "selected": eid in selected_set,
            "slot": next((s["slot"] for s in selected_slots if s["externalId"] == eid), None),
            "muscleGroup": m.muscle_group,
            "equipmentType": m.equipment_type,
            "originalName": m.name,
            "translatedName": m.translation_name,
        })

    payload = {
        "version": 1,
        "stage": "4.5",
        "sourceCatalog": "free-exercise-db",
        "totalImportable": 777,
        "selectedCount": len(selected_ids),
        "removedCount": 777 - len(selected_ids),
        "reductionPercent": round((777 - len(selected_ids)) / 777 * 100, 2),
        "targetRange": [TARGET_MIN, TARGET_MAX],
        "selected": selected_entries,
        "auditMatrix": audit_matrix,
        "muscleMinimums": MUSCLE_MINIMUMS,
        "muscleMaximums": MUSCLE_MAXIMUMS,
        "canonicalSlotCount": len(CANONICAL_SLOTS),
    }
    return payload


def dumps_deterministic(payload: dict) -> str:
    return json.dumps(payload, ensure_ascii=False, indent=2, sort_keys=False) + "\n"


def main() -> None:
    payload = curate()
    text = dumps_deterministic(payload)
    OUT_JSON.write_bytes(text.encode("utf-8"))
    summary = {
        "selected": payload["selectedCount"],
        "removed": payload["removedCount"],
        "sha16": hashlib.sha256(text.encode("utf-8")).hexdigest()[:16],
    }
    print(json.dumps(summary, ensure_ascii=False))


if __name__ == "__main__":
    main()
