"""Analyze Free Exercise DB JSON for GymTrack import planning.

This script does not touch app production code. Run from repo root or this folder:

    python analyze_free_exercise_db.py
"""

from __future__ import annotations

import json
import os
from collections import Counter, defaultdict
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
INPUT = ROOT / "input" / "free-exercise-db" / "exercises.json"
SCHEMA = ROOT / "input" / "free-exercise-db" / "schema.json"
OUT = Path(__file__).resolve().parent / "free-exercise-db-stats.json"

GYMTRACK_MUSCLES = [
    "Peitoral",
    "Costas",
    "Ombros",
    "Bíceps",
    "Tríceps",
    "Antebraço",
    "Quadríceps",
    "Posteriores",
    "Glúteos",
    "Panturrilhas",
    "Lombar",
    "Trapézio",
    "Abdômen",
]

MUSCLE_SYNONYMS = {
    "peito": "Peitoral",
    "chest": "Peitoral",
    "peitoral": "Peitoral",
    "back": "Costas",
    "costas": "Costas",
    "shoulders": "Ombros",
    "ombros": "Ombros",
    "biceps": "Bíceps",
    "bíceps": "Bíceps",
    "triceps": "Tríceps",
    "tríceps": "Tríceps",
    "forearm": "Antebraço",
    "forearms": "Antebraço",
    "antebraço": "Antebraço",
    "antebraços": "Antebraço",
    "quadriceps": "Quadríceps",
    "quads": "Quadríceps",
    "quadríceps": "Quadríceps",
    "hamstrings": "Posteriores",
    "posteriores": "Posteriores",
    "posteriores de coxa": "Posteriores",
    "glutes": "Glúteos",
    "glúteos": "Glúteos",
    "calves": "Panturrilhas",
    "panturrilhas": "Panturrilhas",
    "lower back": "Lombar",
    "lombar": "Lombar",
    "traps": "Trapézio",
    "trapézio": "Trapézio",
    "abs": "Abdômen",
    "abdomen": "Abdômen",
    "abdômen": "Abdômen",
}

GYMTRACK_EQUIPMENT = [
    "Barra",
    "Halteres",
    "Máquina",
    "Smith",
    "Cabos",
    "Peso corporal",
    "Kettlebell",
    "Elástico",
    "Outro",
]

EQUIPMENT_SYNONYMS = {
    "barra": "Barra",
    "barbell": "Barra",
    "halteres": "Halteres",
    "dumbbell": "Halteres",
    "dumbbells": "Halteres",
    "máquina": "Máquina",
    "maquina": "Máquina",
    "machine": "Máquina",
    "smith": "Smith",
    "smith machine": "Smith",
    "cabo": "Cabos",
    "cabos": "Cabos",
    "cable": "Cabos",
    "cables": "Cabos",
    "peso corporal": "Peso corporal",
    "bodyweight": "Peso corporal",
    "kettlebell": "Kettlebell",
    "elástico": "Elástico",
    "elastico": "Elástico",
    "band": "Elástico",
    "bands": "Elástico",
    "outro": "Outro",
    "other": "Outro",
}

# Extra FEDB equipment keys not in GymTrack synonym map (documented, not applied in production).
FEDB_EQUIPMENT_PROPOSED = {
    "body only": "Peso corporal",
    "kettlebells": "Kettlebell",
    "e-z curl bar": "Barra",
    "medicine ball": "Outro",
    "foam roll": "Outro",
    "exercise ball": "Outro",
}


def is_empty_list(value) -> bool:
    return not isinstance(value, list) or len(value) == 0


def main() -> None:
    data = json.loads(INPUT.read_text(encoding="utf-8"))
    schema = json.loads(SCHEMA.read_text(encoding="utf-8"))
    required = schema.get("required", [])
    properties = list(schema.get("properties", {}).keys())

    assert isinstance(data, list)

    total = len(data)
    ids = [row.get("id") for row in data]
    names = [row.get("name") for row in data]
    id_counts = Counter(ids)
    name_counts = Counter(names)
    dup_ids = {k: v for k, v in id_counts.items() if k is not None and v > 1}
    dup_names = {k: v for k, v in name_counts.items() if k is not None and v > 1}

    keys = set()
    for row in data:
        keys.update(row.keys())

    empty_name = 0
    equipment_null = 0
    force_null = 0
    mechanic_null = 0
    primary_empty = 0
    secondary_empty = 0
    instructions_empty = 0
    images_empty = 0
    missing_required = Counter()
    multi_primary = 0
    primary_eq_secondary = 0
    collision_examples = []
    image_refs = 0
    dup_images_in_row = 0
    rows_dup_images = []
    id_pattern_ok = 0
    id_pattern_bad = []

    levels = Counter()
    forces = Counter()
    mechanics = Counter()
    equipment = Counter()
    categories = Counter()
    primary_muscles = Counter()
    secondary_muscles = Counter()

    import re

    id_re = re.compile(r"^[0-9a-zA-Z_-]+$")

    for row in data:
        for field in required:
            if field not in row:
                missing_required[field] += 1
        eid = row.get("id")
        if isinstance(eid, str) and id_re.match(eid):
            id_pattern_ok += 1
        else:
            id_pattern_bad.append(eid)
        name = row.get("name")
        if not isinstance(name, str) or not name.strip():
            empty_name += 1
        if row.get("equipment") is None:
            equipment_null += 1
        if row.get("force") is None:
            force_null += 1
        if row.get("mechanic") is None:
            mechanic_null += 1
        pm = row.get("primaryMuscles") or []
        sm = row.get("secondaryMuscles") or []
        if is_empty_list(pm):
            primary_empty += 1
        if is_empty_list(sm):
            secondary_empty += 1
        if is_empty_list(row.get("instructions")):
            instructions_empty += 1
        imgs = row.get("images") or []
        if is_empty_list(imgs):
            images_empty += 1
        if isinstance(imgs, list):
            image_refs += len(imgs)
            if len(imgs) != len(set(imgs)):
                dup_images_in_row += 1
                rows_dup_images.append(eid)
        if isinstance(pm, list) and len(pm) > 1:
            multi_primary += 1
        if isinstance(pm, list) and isinstance(sm, list):
            overlap = set(pm) & set(sm)
            if overlap:
                primary_eq_secondary += 1
                if len(collision_examples) < 8:
                    collision_examples.append(
                        {"id": eid, "name": name, "overlap": sorted(overlap)},
                    )
        levels[row.get("level")] += 1
        forces[row.get("force")] += 1
        mechanics[row.get("mechanic")] += 1
        equipment[row.get("equipment")] += 1
        categories[row.get("category")] += 1
        if isinstance(pm, list):
            for m in pm:
                primary_muscles[m] += 1
        if isinstance(sm, list):
            for m in sm:
                secondary_muscles[m] += 1

    all_source_muscles = sorted(set(primary_muscles) | set(secondary_muscles))

    muscle_map = {}
    for m in all_source_muscles:
        mapped = MUSCLE_SYNONYMS.get(m.lower())
        if mapped:
            muscle_map[m] = {"gymtrack": mapped, "status": "mapeavel_por_sinonimo_existente"}
        else:
            muscle_map[m] = {"gymtrack": None, "status": "sem_sinonimo_no_GymTrack"}

    gymtrack_not_in_source = []
    for g in GYMTRACK_MUSCLES:
        reverse = [src for src, info in muscle_map.items() if info["gymtrack"] == g]
        if not reverse:
            gymtrack_not_in_source.append(g)

    equipment_map = {}
    for eq, count in equipment.items():
        if eq is None:
            equipment_map["null"] = {
                "count": count,
                "gymtrack": None,
                "status": "null_na_fonte",
            }
            continue
        syn = EQUIPMENT_SYNONYMS.get(eq.lower())
        proposed = FEDB_EQUIPMENT_PROPOSED.get(eq.lower())
        if syn:
            equipment_map[eq] = {
                "count": count,
                "gymtrack": syn,
                "status": "mapeavel_por_sinonimo_existente",
            }
        elif proposed:
            equipment_map[eq] = {
                "count": count,
                "gymtrack": proposed,
                "status": "precisa_novo_sinonimo_nao_implementado",
            }
        else:
            equipment_map[eq] = {
                "count": count,
                "gymtrack": None,
                "status": "sem_equivalente",
            }

    gymtrack_eq_unmapped = []
    mapped_targets = {v["gymtrack"] for v in equipment_map.values() if v.get("gymtrack")}
    for g in GYMTRACK_EQUIPMENT:
        if g not in mapped_targets:
            gymtrack_eq_unmapped.append(g)

    stats = {
        "file_bytes": os.path.getsize(INPUT),
        "total_exercises": total,
        "distinct_ids": len(id_counts),
        "duplicate_ids": dup_ids,
        "duplicate_names": dup_names,
        "fields_present_in_data": sorted(keys),
        "schema_properties": properties,
        "schema_required": required,
        "empty_name": empty_name,
        "equipment_null": equipment_null,
        "force_null": force_null,
        "mechanic_null": mechanic_null,
        "primaryMuscles_empty": primary_empty,
        "secondaryMuscles_empty": secondary_empty,
        "instructions_empty": instructions_empty,
        "images_empty": images_empty,
        "missing_required_fields": dict(missing_required),
        "ids_matching_schema_pattern": id_pattern_ok,
        "ids_not_matching_schema_pattern": id_pattern_bad[:20],
        "level_values": dict(sorted(levels.items(), key=lambda x: str(x[0]))),
        "force_values": dict(sorted(forces.items(), key=lambda x: str(x[0]))),
        "mechanic_values": dict(sorted(mechanics.items(), key=lambda x: str(x[0]))),
        "equipment_values": dict(sorted(equipment.items(), key=lambda x: str(x[0]))),
        "category_values": dict(sorted(categories.items(), key=lambda x: str(x[0]))),
        "primaryMuscles_values": dict(sorted(primary_muscles.items())),
        "secondaryMuscles_values": dict(sorted(secondary_muscles.items())),
        "multi_primary_count": multi_primary,
        "primary_secondary_overlap_count": primary_eq_secondary,
        "primary_secondary_overlap_examples": collision_examples,
        "image_reference_count": image_refs,
        "rows_with_duplicate_images": dup_images_in_row,
        "duplicate_image_row_ids": rows_dup_images[:20],
        "muscle_mapping": muscle_map,
        "gymtrack_muscles_without_source_value": gymtrack_not_in_source,
        "equipment_mapping": equipment_map,
        "gymtrack_equipment_without_source_value": gymtrack_eq_unmapped,
    }
    OUT.write_text(json.dumps(stats, ensure_ascii=False, indent=2), encoding="utf-8")
    print(f"Wrote {OUT}")
    print(f"total={total} distinct_ids={stats['distinct_ids']} bytes={stats['file_bytes']}")


if __name__ == "__main__":
    main()
