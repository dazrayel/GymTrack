"""Etapa curadoria V2 — catálogo principal ~135–150 com 2 imagens locais.

Não modifica gymtrack-exercises.json, assets do app, importer, Room ou UI.
Gera analysis/MAIN_CATALOG_V2.json e MAIN_CATALOG_V2.md.

Elegibilidade de mídia (obrigatória para selected):
  tools/exercise-import/input/free-exercise-db/exercises/<id>/{0,1}.jpg
  (fonte local do Free Exercise DB; cópia para o APK é etapa futura)

Exercícios excelentes sem essas duas imagens → candidatesWithoutMedia.
"""

from __future__ import annotations

import hashlib
import json
import sys
from collections import Counter
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
V1_JSON = ROOT / "analysis" / "MAIN_CATALOG.json"
OUT_JSON = ROOT / "analysis" / "MAIN_CATALOG_V2.json"
OUT_MD = ROOT / "analysis" / "MAIN_CATALOG_V2.md"
SOURCE_IMAGES = ROOT / "input" / "free-exercise-db" / "exercises"
APK_IMAGES = REPO / "app" / "src" / "main" / "assets" / "exercises"

IMPORTABLE = frozenset({"IMPORTABLE", "IMPORTABLE_WITH_FALLBACK"})
TARGET_MIN = 135
TARGET_MAX = 150

# Metas de distribuição (soft).
MUSCLE_TARGETS: dict[str, int] = {
    "Peitoral": 15,
    "Costas": 18,
    "Ombros": 15,
    "Bíceps": 10,
    "Tríceps": 10,
    "Quadríceps": 15,
    "Posteriores": 12,
    "Glúteos": 10,
    "Abdômen": 10,
    "Panturrilhas": 7,
    "Lombar": 7,
    "Antebraço": 6,
    "Trapézio": 6,
}

MUSCLE_MINIMUMS: dict[str, int] = {
    "Peitoral": 10,
    "Costas": 12,
    "Ombros": 8,
    "Bíceps": 5,
    "Tríceps": 6,
    "Quadríceps": 10,
    "Posteriores": 6,
    "Glúteos": 6,
    "Abdômen": 8,
    "Panturrilhas": 4,
    "Lombar": 6,
    "Antebraço": 5,
    "Trapézio": 5,
}

MUSCLE_MAXIMUMS: dict[str, int] = {
    "Peitoral": 15,
    "Costas": 16,
    "Ombros": 12,
    "Bíceps": 8,
    "Tríceps": 10,
    "Quadríceps": 15,
    "Posteriores": 10,
    "Glúteos": 10,
    "Abdômen": 30,  # pool de mídia local fortemente enviesado para abdômen
    "Panturrilhas": 7,
    "Lombar": 8,
    "Antebraço": 8,
    "Trapézio": 8,
}

# Ordem canônica V2: (slot, externalId, motivo curto).
# Preferência: manter V1 quando relevante; acrescentar Lombar/Antebraço/Trapézio;
# preencher Abdômen com padrões úteis (mídia local concentrada nesse grupo).
CANONICAL_V2: list[tuple[str, str, str]] = [
    # --- Peitoral (13) — meta 15; só 13 com mídia local útil ---
    ("chest_flat_barbell", "Barbell_Bench_Press_-_Medium_Grip", "empurrada horizontal básica"),
    ("chest_incline_db", "Incline_Dumbbell_Press", "inclinado com halteres"),
    ("chest_incline_barbell", "Barbell_Incline_Bench_Press_-_Medium_Grip", "inclinado com barra"),
    ("chest_decline_barbell", "Decline_Barbell_Bench_Press", "declinado"),
    ("chest_fly_db", "Dumbbell_Flyes", "crucifixo / adução"),
    ("chest_cable_crossover", "Cable_Crossover", "crossover na polia"),
    ("chest_machine", "Machine_Bench_Press", "máquina estável"),
    ("chest_bodyweight", "Pushups", "peso corporal"),
    ("chest_dips", "Dips_-_Chest_Version", "paralelas ênfase peito"),
    ("chest_pullover_db", "Bent-Arm_Dumbbell_Pullover", "pullover / expansão"),
    ("chest_floor_press", "Alternating_Floor_Press", "press no chão (amplitude limitada)"),
    ("chest_guillotine", "Barbell_Guillotine_Bench_Press", "ênfase peitoral superior"),
    ("chest_bands", "Bench_Press_-_With_Bands", "resistência acomodante"),
    # --- Costas (14) — meta 18; teto de mídia local ---
    ("back_pulldown", "Wide-Grip_Lat_Pulldown", "puxada vertical"),
    ("back_pullup", "Pullups", "barra fixa pronada"),
    ("back_chinup", "Chin-Up", "barra fixa supinada"),
    ("back_row_barbell", "Bent_Over_Barbell_Row", "remada horizontal barra"),
    ("back_row_db", "One-Arm_Dumbbell_Row", "remada unilateral"),
    ("back_row_cable", "Seated_Cable_Rows", "remada sentada polia"),
    ("back_row_tbar", "T-Bar_Row_with_Handle", "remada T-bar"),
    ("back_pullover", "Bent-Arm_Barbell_Pullover", "pullover costas"),
    ("back_underhand_pulldown", "Underhand_Cable_Pulldowns", "puxada supinada"),
    ("back_smith_row", "Smith_Machine_Bent_Over_Row", "remada smith"),
    ("back_kb_row", "Alternating_Kettlebell_Row", "remada kettlebell"),
    ("back_renegade", "Alternating_Renegade_Row", "remada + anti-rotação"),
    ("back_one_arm_bar", "Bent_Over_One-Arm_Long_Bar_Row", "remada unilateral barra"),
    ("back_band_pullup", "Band_Assisted_Pull-Up", "barra assistida"),
    # --- Ombros (10) ---
    ("shoulder_press_barbell", "Barbell_Shoulder_Press", "desenvolvimento barra"),
    ("shoulder_press_db", "Dumbbell_Shoulder_Press", "desenvolvimento halteres"),
    ("shoulder_press_cable", "Alternating_Cable_Shoulder_Press", "desenvolvimento polia"),
    ("shoulder_lateral", "Side_Lateral_Raise", "elevação lateral"),
    ("shoulder_deltoid_alt", "Alternating_Deltoid_Raise", "elevação deltóide alternada"),
    ("shoulder_rear", "Bent_Over_Dumbbell_Rear_Delt_Raise_With_Head_On_Bench", "deltóide posterior"),
    ("shoulder_reverse_fly", "Reverse_Flyes", "crucifixo reverso"),
    ("shoulder_face_pull", "Face_Pull", "face pull / saúde do ombro"),
    ("shoulder_upright", "Upright_Barbell_Row", "remada alta"),
    ("shoulder_thruster", "Kettlebell_Thruster", "composto ombro+pernas"),
    # --- Bíceps (6) ---
    ("biceps_barbell", "Barbell_Curl", "rosca barra"),
    ("biceps_db", "Dumbbell_Bicep_Curl", "rosca halteres"),
    ("biceps_hammer", "Alternate_Hammer_Curl", "rosca martelo"),
    ("biceps_incline", "Alternate_Incline_Dumbbell_Curl", "rosca inclinada"),
    ("biceps_preacher", "Machine_Preacher_Curls", "rosca scott"),
    ("biceps_lying_incline", "Barbell_Curls_Lying_Against_An_Incline", "rosca no banco inclinado"),
    # --- Tríceps (8) ---
    ("triceps_pushdown", "Triceps_Pushdown", "pushdown"),
    ("triceps_overhead", "Cable_Rope_Overhead_Triceps_Extension", "extensão acima da cabeça"),
    ("triceps_dips", "Dips_-_Triceps_Version", "paralelas tríceps"),
    ("triceps_bench_dips", "Bench_Dips", "paralelas no banco"),
    ("triceps_close_grip", "Close-Grip_Barbell_Bench_Press", "supino pegada fechada"),
    ("triceps_skull", "EZ-Bar_Skullcrusher", "skull crusher"),
    ("triceps_band_skull", "Band_Skull_Crusher", "skull com elástico"),
    ("triceps_power_bench", "Bench_Press_-_Powerlifting", "press força (tríceps/peito)"),
    # --- Quadríceps (13) ---
    ("quad_squat", "Barbell_Squat", "agachamento"),
    ("quad_full_squat", "Barbell_Full_Squat", "agachamento completo"),
    ("quad_front", "Front_Squat_Clean_Grip", "agachamento frontal"),
    ("quad_goblet", "Goblet_Squat", "goblet"),
    ("quad_hack_machine", "Hack_Squat", "hack machine"),
    ("quad_hack_barbell", "Barbell_Hack_Squat", "hack com barra"),
    ("quad_leg_press", "Leg_Press", "leg press"),
    ("quad_extension", "Leg_Extensions", "extensão de joelhos"),
    ("quad_lunge_db", "Dumbbell_Lunges", "afundo halteres"),
    ("quad_lunge_barbell", "Barbell_Lunge", "afundo barra"),
    ("quad_step_up", "Barbell_Step_Ups", "step-up"),
    ("quad_split", "Barbell_Side_Split_Squat", "agachamento lateral"),
    ("quad_to_bench", "Barbell_Squat_To_A_Bench", "agachamento ao banco"),
    # --- Posteriores (8) ---
    ("ham_rdl", "Romanian_Deadlift", "RDL / hinge"),
    ("ham_stiff", "Stiff-Legged_Barbell_Deadlift", "stiff"),
    ("ham_curl", "Lying_Leg_Curls", "mesa flexora"),
    ("ham_ball_curl", "Ball_Leg_Curl", "flexora com bola"),
    ("ham_good_morning", "Good_Morning", "good morning"),
    ("ham_band_gm", "Band_Good_Morning", "good morning elástico"),
    ("ham_power_clean", "Power_Clean", "clean olímpico"),
    ("ham_hang_clean", "Alternating_Hang_Clean", "hang clean alternado"),
    # --- Glúteos (8) ---
    ("glute_thrust", "Barbell_Hip_Thrust", "hip thrust"),
    ("glute_bridge_bar", "Barbell_Glute_Bridge", "ponte com barra"),
    ("glute_bridge_bw", "Butt_Lift_Bridge", "ponte peso corporal"),
    ("glute_kickback_cable", "One-Legged_Cable_Kickback", "kickback polia"),
    ("glute_kickback_bw", "Glute_Kickback", "coice glúteo"),
    ("glute_pull_through", "Pull_Through", "pull-through"),
    ("glute_band_ext", "Hip_Extension_with_Bands", "extensão com elástico"),
    ("glute_flutter", "Flutter_Kicks", "flutter kicks"),
    # --- Panturrilhas (5) ---
    ("calf_standing", "Standing_Calf_Raises", "joelho estendido"),
    ("calf_seated", "Seated_Calf_Raise", "joelho flexionado máquina"),
    ("calf_seated_bar", "Barbell_Seated_Calf_Raise", "joelho flexionado barra"),
    ("calf_press", "Calf_Press", "press panturrilha"),
    ("calf_donkey", "Donkey_Calf_Raises", "donkey"),
    # --- Lombar (7) — prioridade V2 ---
    ("lowback_deadlift", "Barbell_Deadlift", "terra convencional / eretores"),
    ("lowback_hyper", "Hyperextensions_Back_Extensions", "hiperextensão"),
    ("lowback_bands", "Deadlift_with_Bands", "terra com bandas"),
    ("lowback_chains", "Deadlift_with_Chains", "terra com correntes"),
    ("lowback_axle", "Axle_Deadlift", "terra axle / grip+lombar"),
    ("lowback_atlas_trainer", "Atlas_Stone_Trainer", "levantamento pedra (treino)"),
    ("lowback_atlas", "Atlas_Stones", "atlas stones"),
    # --- Antebraço (7) ---
    ("fore_wrist_up", "Seated_Palm-Up_Barbell_Wrist_Curl", "flexão de punho"),
    ("fore_farmers", "Farmers_Walk", "grip / carregamento"),
    ("fore_cable_wrist", "Cable_Wrist_Curl", "flexão punho polia"),
    ("fore_finger", "Finger_Curls", "rosca de dedos / grip"),
    ("fore_pronation", "Dumbbell_Lying_Pronation", "pronação"),
    ("fore_supination", "Dumbbell_Lying_Supination", "supinação"),
    ("fore_bottoms_up", "Bottoms-Up_Clean_From_The_Hang_Position", "grip instável kettlebell"),
    # --- Trapézio (7) ---
    ("trap_shrug_bar", "Barbell_Shrug", "encolhimento barra"),
    ("trap_shrug_behind", "Barbell_Shrug_Behind_The_Back", "encolhimento atrás"),
    ("trap_shrug_db", "Dumbbell_Shrug", "encolhimento halteres"),
    ("trap_shrug_cable", "Cable_Shrugs", "encolhimento polia"),
    ("trap_shrug_machine", "Calf-Machine_Shoulder_Shrug", "encolhimento máquina"),
    ("trap_clean_shrug", "Clean_Shrug", "clean shrug"),
    ("trap_sumo_high_pull", "Kettlebell_Sumo_High_Pull", "high pull / trapézio"),
    # --- Abdômen (núcleo V1 + padrões extras; pool local concentrado aqui) ---
    ("abs_crunch", "Crunches", "flexão de tronco"),
    ("abs_situp", "3_4_Sit-Up", "abdominal clássico"),
    ("abs_cross", "Cross-Body_Crunch", "oblíquo cruzado"),
    ("abs_cable", "Cable_Crunch", "abdominal polia"),
    ("abs_hanging", "Hanging_Leg_Raise", "elevação de pernas"),
    ("abs_plank", "Plank", "anti-extensão"),
    ("abs_side_plank", "Side_Bridge", "anti-lateral"),
    ("abs_russian", "Russian_Twist", "rotação"),
    ("abs_woodchop", "Standing_Cable_Wood_Chop", "rotação cabo"),
    ("abs_roller", "Ab_Roller", "anti-extensão roller"),
    ("abs_dead_bug", "Dead_Bug", "anti-extensão controlada"),
    ("abs_decline", "Decline_Crunch", "abdominal declinado"),
    ("abs_reverse_cable", "Cable_Reverse_Crunch", "abdominal reverso polia"),
    ("abs_machine", "Ab_Crunch_Machine", "máquina"),
    ("abs_jackknife", "Jackknife_Sit-Up", "canivete"),
    ("abs_hanging_pike", "Hanging_Pike", "pike pendurado"),
    ("abs_landmine", "Landmine_180s", "rotação landmine"),
    ("abs_side_bend", "Dumbbell_Side_Bend", "flexão lateral"),
    ("abs_decline_rev", "Decline_Reverse_Crunch", "reverso declinado"),
    ("abs_oblique", "Oblique_Crunches", "oblíquo"),
    ("abs_seated_cable", "Cable_Seated_Crunch", "abdominal sentado polia"),
    ("abs_leg_raise_bench", "Flat_Bench_Lying_Leg_Raise", "elevação de pernas banco"),
    ("abs_air_bike", "Air_Bike", "bicicleta / oblíquo"),
    ("abs_heel_touch", "Alternate_Heel_Touchers", "toque calcanhar"),
    ("abs_rollout_bar", "Barbell_Ab_Rollout", "rollout barra"),
    ("abs_russian_cable", "Cable_Russian_Twists", "rotação cabo sentado"),
    ("abs_parallel_raise", "Knee_Hip_Raise_On_Parallel_Bars", "elevação nas paralelas"),
    ("abs_cocoons", "Cocoons", "casulo / flexão composta"),
    ("abs_bottoms_up", "Bottoms_Up", "bottoms-up / core"),
]

# Candidatos sem mídia local (0.jpg+1.jpg na pasta input) — preenchem gaps da meta.
# Escolhidos entre os 777 importáveis; não entram em selected.
CANDIDATES_WITHOUT_MEDIA: list[tuple[str, str, str]] = [
    ("chest_gap_incline_fly", "Incline_Dumbbell_Flyes", "Peitoral — crucifixo inclinado"),
    ("chest_gap_decline_db", "Decline_Dumbbell_Bench_Press", "Peitoral — declinado halteres"),
    ("back_gap_bodyweight_row", "Bodyweight_Mid_Row", "Costas — remada corporal"),
    ("back_gap_kb_row", "One-Arm_Kettlebell_Row", "Costas — remada kettlebell adicional"),
    ("back_gap_straight_arm", "Straight-Arm_Pulldown", "Costas — pulldown braço reto"),
    ("back_gap_inverted", "Inverted_Row_with_Straps", "Costas — remada invertida"),
    ("shoulder_gap_front", "Front_Dumbbell_Raise", "Ombros — elevação frontal"),
    ("shoulder_gap_arnold", "Arnold_Dumbbell_Press", "Ombros — Arnold press"),
    ("shoulder_gap_machine_press", "Machine_Shoulder_Military_Press", "Ombros — desenvolvimento máquina"),
    ("shoulder_gap_cable_lateral", "Cable_Seated_Lateral_Raise", "Ombros — lateral na polia"),
    ("biceps_gap_cable", "Standing_Biceps_Cable_Curl", "Bíceps — rosca polia"),
    ("biceps_gap_concentration", "Concentration_Curls", "Bíceps — concentrada"),
    ("biceps_gap_ez", "EZ-Bar_Curl", "Bíceps — barra W"),
    ("biceps_gap_spider", "Spider_Curl", "Bíceps — spider"),
    ("triceps_gap_oh_db", "Dumbbell_One-Arm_Triceps_Extension", "Tríceps — extensão unilateral"),
    ("triceps_gap_kickback", "Tricep_Dumbbell_Kickback", "Tríceps — kickback"),
    ("ham_gap_seated_curl", "Seated_Leg_Curl", "Posteriores — flexora sentada"),
    ("ham_gap_nordic", "Natural_Glute_Ham_Raise", "Posteriores — GHR/nórdico"),
    ("ham_gap_rdl_deficit", "Romanian_Deadlift_from_Deficit", "Posteriores — RDL deficit"),
    ("ham_gap_sumo", "Sumo_Deadlift", "Posteriores — terra sumo"),
    ("glute_gap_single_bridge", "Single_Leg_Glute_Bridge", "Glúteos — ponte unilateral"),
    ("glute_gap_lying", "Lying_Glute", "Glúteos — variação deitada"),
    ("quad_gap_rear_lunge", "Dumbbell_Rear_Lunge", "Quadríceps — afundo reverso"),
    ("quad_gap_pistol", "Kettlebell_Pistol_Squat", "Quadríceps — pistol"),
    ("calf_gap_db_one_leg", "Dumbbell_Seated_One-Leg_Calf_Raise", "Panturrilhas — unilateral"),
    ("calf_gap_smith", "Smith_Machine_Calf_Raise", "Panturrilhas — smith"),
]


def _has_both(folder: Path, external_id: str) -> bool:
    d = folder / external_id
    return (d / "0.jpg").is_file() and (d / "1.jpg").is_file()


def _load_importable() -> list[dict]:
    catalog = json.loads(NORMALIZED.read_text(encoding="utf-8"))
    rows = [r for r in catalog if classify_importability(r) in IMPORTABLE]
    rows.sort(key=lambda r: (r.get("source") or "", r.get("externalId") or ""))
    if len(rows) != 777:
        raise AssertionError(f"Expected 777 importable, got {len(rows)}")
    return rows


def _load_translations() -> dict[tuple[str, str], dict]:
    data = json.loads(TRANSLATIONS.read_text(encoding="utf-8"))
    return {
        (t.get("source") or "free-exercise-db", t["externalId"]): t
        for t in data["translations"]
    }


def _load_v1_ids() -> set[str]:
    if not V1_JSON.exists():
        return set()
    payload = json.loads(V1_JSON.read_text(encoding="utf-8"))
    return {e["externalId"] for e in payload["selected"]}


def curate_v2() -> dict[str, Any]:
    raw_rows = _load_importable()
    by_id = {r["externalId"]: r for r in raw_rows}
    trans = _load_translations()
    v1_ids = _load_v1_ids()
    rejected_ids = {
        r["externalId"]
        for r in json.loads(NORMALIZED.read_text(encoding="utf-8"))
        if classify_importability(r) not in IMPORTABLE
    }

    selected: list[dict[str, Any]] = []
    seen: set[str] = set()
    removals_from_v1: list[dict[str, str]] = []

    for slot, eid, reason in CANONICAL_V2:
        if eid in seen:
            raise ValueError(f"Duplicate slot id in CANONICAL_V2: {eid}")
        if eid not in by_id:
            raise KeyError(f"Unknown externalId in V2 curation: {eid}")
        if eid in rejected_ids:
            raise ValueError(f"Rejected exercise selected: {eid}")
        if not _has_both(SOURCE_IMAGES, eid):
            raise ValueError(f"Selected without local source media: {eid}")
        raw = by_id[eid]
        t = trans.get(("free-exercise-db", eid), {})
        apk0 = (APK_IMAGES / eid / "0.jpg").is_file()
        apk1 = (APK_IMAGES / eid / "1.jpg").is_file()
        selected.append(
            {
                "slot": slot,
                "source": raw.get("source") or "free-exercise-db",
                "externalId": eid,
                "externalSource": raw.get("source") or "free-exercise-db",
                "originalName": raw.get("name") or "",
                "translatedName": t.get("name") or raw.get("name") or "",
                "nameStatus": t.get("status") or "UNKNOWN",
                "muscleGroup": raw.get("muscleGroup"),
                "equipmentType": raw.get("equipmentType"),
                "secondaryMuscles": list(raw.get("secondaryMuscles") or []),
                "reason": reason,
                "inPreviousCatalog": eid in v1_ids,
                "sourceImage0": True,
                "sourceImage1": True,
                "apkImage0": apk0,
                "apkImage1": apk1,
                "imagePaths": [f"{eid}/0.jpg", f"{eid}/1.jpg"],
                "imageCount": 2,
                "mediaStatus": "apk" if (apk0 and apk1) else "source_only",
            }
        )
        seen.add(eid)

    for eid in sorted(v1_ids - seen):
        raw = by_id.get(eid)
        removals_from_v1.append(
            {
                "externalId": eid,
                "muscleGroup": (raw or {}).get("muscleGroup") or "",
                "reason": "Removido na V2 por redundância ou menor prioridade frente à nova distribuição",
            }
        )

    candidates: list[dict[str, Any]] = []
    cand_seen: set[str] = set()
    for slot, eid, note in CANDIDATES_WITHOUT_MEDIA:
        if eid in seen or eid in cand_seen:
            continue
        if eid not in by_id:
            # Skip unknown ids quietly but keep deterministic output
            continue
        if eid in rejected_ids:
            continue
        if _has_both(SOURCE_IMAGES, eid):
            # Has media — should have been considered for selected; keep out of this list
            continue
        raw = by_id[eid]
        t = trans.get(("free-exercise-db", eid), {})
        candidates.append(
            {
                "slot": slot,
                "source": raw.get("source") or "free-exercise-db",
                "externalId": eid,
                "externalSource": raw.get("source") or "free-exercise-db",
                "originalName": raw.get("name") or "",
                "translatedName": t.get("name") or raw.get("name") or "",
                "muscleGroup": raw.get("muscleGroup"),
                "equipmentType": raw.get("equipmentType"),
                "secondaryMuscles": list(raw.get("secondaryMuscles") or []),
                "note": note,
                "sourceImage0": (SOURCE_IMAGES / eid / "0.jpg").is_file(),
                "sourceImage1": (SOURCE_IMAGES / eid / "1.jpg").is_file(),
                "apkImage0": (APK_IMAGES / eid / "0.jpg").is_file(),
                "apkImage1": (APK_IMAGES / eid / "1.jpg").is_file(),
            }
        )
        cand_seen.add(eid)

    distribution = Counter(e["muscleGroup"] for e in selected)
    equipment = Counter(e["equipmentType"] or "None" for e in selected)
    media_apk = sum(1 for e in selected if e["mediaStatus"] == "apk")
    media_source_only = sum(1 for e in selected if e["mediaStatus"] == "source_only")

    audit_matrix = []
    for r in raw_rows:
        eid = r["externalId"]
        t = trans.get(("free-exercise-db", eid), {})
        audit_matrix.append(
            {
                "source": r.get("source") or "free-exercise-db",
                "externalId": eid,
                "selected": eid in seen,
                "candidateWithoutMedia": eid in cand_seen,
                "muscleGroup": r.get("muscleGroup"),
                "equipmentType": r.get("equipmentType"),
                "originalName": r.get("name") or "",
                "translatedName": t.get("name") or r.get("name") or "",
                "hasLocalSourceMedia": _has_both(SOURCE_IMAGES, eid),
                "hasApkMedia": _has_both(APK_IMAGES, eid),
            }
        )

    shortfalls = {
        muscle: max(0, target - distribution.get(muscle, 0))
        for muscle, target in MUSCLE_TARGETS.items()
        if distribution.get(muscle, 0) < target
    }

    payload: dict[str, Any] = {
        "version": 2,
        "source": "free-exercise-db",
        "sourceCatalog": "free-exercise-db",
        "totalAvailable": 777,
        "totalImportable": 777,
        "selectedCount": len(selected),
        "previousSelectedCount": len(v1_ids),
        "addedFromV1Count": sum(1 for e in selected if e["inPreviousCatalog"]),
        "removedFromV1Count": len(removals_from_v1),
        "candidatesWithoutMediaCount": len(candidates),
        "targetRange": [TARGET_MIN, TARGET_MAX],
        "muscleTargets": MUSCLE_TARGETS,
        "muscleMinimums": MUSCLE_MINIMUMS,
        "muscleMaximums": MUSCLE_MAXIMUMS,
        "selected": selected,
        "excluded": [],
        "candidatesWithoutMedia": candidates,
        "removedFromPreviousCatalog": removals_from_v1,
        "distribution": dict(sorted(distribution.items())),
        "equipmentDistribution": dict(sorted(equipment.items())),
        "mediaSummary": {
            "selectedWithTwoImages": len(selected),
            "selectedWithOneImage": 0,
            "selectedWithZeroImages": 0,
            "selectedMediaInApk": media_apk,
            "selectedMediaSourceOnly": media_source_only,
            "localSourcePoolSize": sum(
                1 for r in raw_rows if _has_both(SOURCE_IMAGES, r["externalId"])
            ),
            "apkPoolSize": sum(1 for r in raw_rows if _has_both(APK_IMAGES, r["externalId"])),
            "mediaRule": (
                "selected exige 0.jpg+1.jpg em "
                "tools/exercise-import/input/free-exercise-db/exercises/"
            ),
        },
        "shortfallsVsTarget": shortfalls,
        "notes": [
            "Pool local com 2 imagens: 171 importáveis (fonte input/). APK atual: 102.",
            "Abdômen excede a meta porque a mídia local está concentrada nesse grupo; "
            "sem isso o total ficaria abaixo de 135.",
            "Costas/Ombros/Bíceps/Peitoral/Posteriores ficam abaixo da meta por falta "
            "de mídia local — ver candidatesWithoutMedia.",
            "MAIN_CATALOG.json (V1) permanece intacto para comparação.",
        ],
        "auditMatrix": audit_matrix,
        "redundancyAvoided": [
            {
                "theme": "Supino",
                "kept": [
                    "Barbell_Bench_Press_-_Medium_Grip",
                    "Incline_Dumbbell_Press",
                    "Decline_Barbell_Bench_Press",
                ],
                "avoidedExtra": "Não expandir com dezenas de variações de pegada/smith quase idênticas sem mídia",
            },
            {
                "theme": "Agachamento",
                "kept": ["Barbell_Squat", "Front_Squat_Clean_Grip", "Goblet_Squat", "Hack_Squat"],
                "avoidedExtra": "Alongamentos e arrastos (All_Fours_Quad_Stretch, Backward_Drag)",
            },
            {
                "theme": "Rosca",
                "kept": ["Barbell_Curl", "Dumbbell_Bicep_Curl", "Alternate_Hammer_Curl"],
                "avoidedExtra": "SMR (Brachialis-SMR) e excesso de variants sem mídia",
            },
            {
                "theme": "Abdômen",
                "keptPattern": "crunch, elevação de pernas, plank, rotação, roller, máquina",
                "avoidedExtra": "Windmills avançados, stretches e gadgets pouco gerais",
            },
        ],
    }
    return payload


def dumps_deterministic(payload: dict[str, Any]) -> str:
    return json.dumps(payload, ensure_ascii=False, indent=2, sort_keys=False) + "\n"


def write_markdown(payload: dict[str, Any]) -> str:
    lines: list[str] = [
        "# MAIN_CATALOG V2 — Curadoria do catálogo principal",
        "",
        f"- Versão: **{payload['version']}**",
        f"- Fonte: `{payload['source']}`",
        f"- Importáveis: **{payload['totalImportable']}**",
        f"- Selecionados: **{payload['selectedCount']}** (faixa alvo {payload['targetRange'][0]}–{payload['targetRange'][1]})",
        f"- Catálogo anterior (V1): **{payload['previousSelectedCount']}**",
        f"- Mantidos da V1: **{payload['addedFromV1Count']}**",
        f"- Removidos da V1: **{payload['removedFromV1Count']}**",
        f"- Candidatos sem mídia local: **{payload['candidatesWithoutMediaCount']}**",
        "",
        "## Mídia",
        "",
        f"- Selecionados com 2 imagens (fonte local): **{payload['mediaSummary']['selectedWithTwoImages']}** (100%)",
        f"- Já no APK (`app/src/main/assets/exercises`): **{payload['mediaSummary']['selectedMediaInApk']}**",
        f"- Apenas na fonte `input/` (ainda não copiados ao APK): **{payload['mediaSummary']['selectedMediaSourceOnly']}**",
        f"- Pool fonte local: **{payload['mediaSummary']['localSourcePoolSize']}** | Pool APK: **{payload['mediaSummary']['apkPoolSize']}**",
        "",
        "## Distribuição",
        "",
        "| Grupo | Selecionados | Meta | Δ |",
        "| ----- | -----------: | ---: | -: |",
    ]
    for muscle, target in MUSCLE_TARGETS.items():
        got = payload["distribution"].get(muscle, 0)
        delta = got - target
        sign = f"+{delta}" if delta > 0 else str(delta)
        lines.append(f"| {muscle} | {got} | {target} | {sign} |")
    lines += ["", "## Notas", ""]
    for note in payload["notes"]:
        lines.append(f"- {note}")

    by_muscle: dict[str, list[dict]] = {}
    for e in payload["selected"]:
        by_muscle.setdefault(e["muscleGroup"] or "?", []).append(e)

    lines += ["", "## Seleção por grupo muscular", ""]
    for muscle in MUSCLE_TARGETS:
        items = by_muscle.get(muscle, [])
        lines.append(f"### {muscle} ({len(items)})")
        lines.append("")
        for i, e in enumerate(items, 1):
            media = "0.jpg + 1.jpg"
            where = "APK" if e["mediaStatus"] == "apk" else "fonte input/ (pendente APK)"
            v1 = "sim" if e["inPreviousCatalog"] else "não"
            lines.append(f"{i}. **{e['translatedName']}**")
            lines.append(f"   - externalId: `{e['externalId']}`")
            lines.append(f"   - equipamento: {e['equipmentType']}")
            lines.append(f"   - imagens: {media} ({where})")
            lines.append(f"   - na V1: {v1}")
            lines.append(f"   - motivo: {e['reason']}")
            lines.append("")
    # any leftover muscles
    for muscle, items in sorted(by_muscle.items()):
        if muscle in MUSCLE_TARGETS:
            continue
        lines.append(f"### {muscle} ({len(items)})")
        lines.append("")
        for i, e in enumerate(items, 1):
            lines.append(f"{i}. **{e['translatedName']}** (`{e['externalId']}`)")
        lines.append("")

    lines += ["## Removidos da V1", ""]
    if not payload["removedFromPreviousCatalog"]:
        lines.append("_Nenhum._")
    else:
        for r in payload["removedFromPreviousCatalog"]:
            lines.append(
                f"- `{r['externalId']}` ({r['muscleGroup']}): {r['reason']}"
            )
    lines.append("")

    lines += ["## Candidatos sem mídia local", ""]
    lines.append(
        "Exercícios desejáveis para fechar gaps da meta, mas **sem** `0.jpg`+`1.jpg` "
        "em `tools/exercise-import/input/free-exercise-db/exercises/`."
    )
    lines.append("")
    for c in payload["candidatesWithoutMedia"]:
        lines.append(
            f"- **{c['translatedName']}** (`{c['externalId']}`) — {c['muscleGroup']} — {c['note']}"
        )
    lines.append("")

    lines += ["## Redundâncias evitadas", ""]
    for block in payload["redundancyAvoided"]:
        lines.append(f"### {block['theme']}")
        if "kept" in block:
            lines.append("- Mantidos: " + ", ".join(f"`{x}`" for x in block["kept"]))
        if "keptPattern" in block:
            lines.append(f"- Padrões mantidos: {block['keptPattern']}")
        lines.append(f"- Evitado: {block['avoidedExtra']}")
        lines.append("")

    lines += [
        "## Shortfalls vs meta",
        "",
    ]
    if not payload["shortfallsVsTarget"]:
        lines.append("_Nenhum._")
    else:
        for muscle, n in payload["shortfallsVsTarget"].items():
            lines.append(f"- **{muscle}**: faltam **{n}** (limitação de mídia local)")
    lines.append("")
    return "\n".join(lines) + "\n"


def main() -> None:
    payload = curate_v2()
    text = dumps_deterministic(payload)
    OUT_JSON.write_text(text, encoding="utf-8")
    OUT_MD.write_text(write_markdown(payload), encoding="utf-8")
    print(f"wrote {OUT_JSON} selected={payload['selectedCount']}")
    print(f"wrote {OUT_MD}")
    print("distribution:", payload["distribution"])
    print("media apk/source_only:", payload["mediaSummary"]["selectedMediaInApk"],
          payload["mediaSummary"]["selectedMediaSourceOnly"])
    print("sha256:", hashlib.sha256(text.encode()).hexdigest())


if __name__ == "__main__":
    main()
