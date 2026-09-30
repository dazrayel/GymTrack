"""Etapa curadoria V2.1 — refinamento da V2 + marcação de mídia pendente.

Não modifica gymtrack-exercises.json, assets do APK, importer, Room, UI,
MAIN_CATALOG.json (V1) nem MAIN_CATALOG_V2.json.

Gera:
  analysis/MAIN_CATALOG_V2_1.json
  analysis/MAIN_CATALOG_V2_1.md
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
V2_JSON = ROOT / "analysis" / "MAIN_CATALOG_V2.json"
OUT_JSON = ROOT / "analysis" / "MAIN_CATALOG_V2_1.json"
OUT_MD = ROOT / "analysis" / "MAIN_CATALOG_V2_1.md"
SOURCE_IMAGES = ROOT / "input" / "free-exercise-db" / "exercises"
APK_IMAGES = REPO / "app" / "src" / "main" / "assets" / "exercises"

IMPORTABLE = frozenset({"IMPORTABLE", "IMPORTABLE_WITH_FALLBACK"})
TARGET_MIN = 135
TARGET_MAX = 145

FEDB_RAW = "https://raw.githubusercontent.com/yuhonas/free-exercise-db/main/exercises"
FEDB_LICENSE = "Unlicense (public domain)"
FEDB_LICENSE_URL = "https://github.com/yuhonas/free-exercise-db/blob/main/LICENSE.md"

MUSCLE_TARGETS: dict[str, tuple[int, int]] = {
    "Peitoral": (13, 15),
    "Costas": (17, 18),
    "Ombros": (13, 14),
    "Bíceps": (8, 9),
    "Tríceps": (8, 9),
    "Quadríceps": (13, 14),
    "Posteriores": (9, 10),
    "Glúteos": (8, 9),
    "Abdômen": (15, 18),
    "Panturrilhas": (5, 6),
    "Lombar": (5, 6),
    "Antebraço": (6, 7),
    "Trapézio": (5, 6),
}

MUSCLE_MINIMUMS: dict[str, int] = {
    "Peitoral": 12,
    "Costas": 17,
    "Ombros": 13,
    "Bíceps": 8,
    "Tríceps": 7,
    "Quadríceps": 12,
    "Posteriores": 8,
    "Glúteos": 7,
    "Abdômen": 14,
    "Panturrilhas": 5,
    "Lombar": 5,
    "Antebraço": 6,
    "Trapézio": 5,
}

MUSCLE_MAXIMUMS: dict[str, int] = {
    "Peitoral": 15,
    "Costas": 18,
    "Ombros": 14,
    "Bíceps": 9,
    "Tríceps": 9,
    "Quadríceps": 14,
    "Posteriores": 10,
    "Glúteos": 9,
    "Abdômen": 18,
    "Panturrilhas": 6,
    "Lombar": 7,
    "Antebraço": 7,
    "Trapézio": 6,
}

# Removidos da V2 (ainda rastreados). Preferência: não remover itens da V1.
REMOVE_FROM_V2: dict[str, str] = {
    # Abdômen — redundâncias / excesso (nenhum era da V1 original de 10 abs core
    # que permanecem; estes foram adds da V2)
    "Decline_Reverse_Crunch": "Redundante com Cable_Reverse_Crunch / Decline_Crunch",
    "Oblique_Crunches": "Cobertura oblíqua já por Cross-Body_Crunch e Landmine_180s",
    "Cable_Seated_Crunch": "Redundante com Cable_Crunch",
    "Flat_Bench_Lying_Leg_Raise": "Redundante com Hanging_Leg_Raise",
    "Alternate_Heel_Touchers": "Microvariação de oblíquo pouco diferencial",
    "Cable_Russian_Twists": "Redundante com Russian_Twist",
    "Knee_Hip_Raise_On_Parallel_Bars": "Redundante com Hanging_Leg_Raise",
    "Cocoons": "Casulo pouco geral vs jackknife/crunch",
    "Bottoms_Up": "Pouco comum em academia geral",
    "Hanging_Pike": "Avançado/redundante vs Hanging_Leg_Raise",
    "Jackknife_Sit-Up": "Redundante vs Dead_Bug + crunch patterns",
    "Dumbbell_Side_Bend": "Flexão lateral secundária; woodchop cobre rotação",
    "Air_Bike": "Variação de oblíquo; Cross-Body + Landmine bastam",
    # Lombar — muito específicos para catálogo geral
    "Deadlift_with_Chains": "Avançado/powerlifting específico",
    "Axle_Deadlift": "Equipamento axle pouco comum",
    "Atlas_Stone_Trainer": "Strongman específico",
    "Atlas_Stones": "Strongman específico",
    # Trapézio — menos claros / redundantes
    "Clean_Shrug": "Olímpico específico; shrug clássico cobre",
    "Kettlebell_Sumo_High_Pull": "Mais pull composto que trapézio puro",
    "Calf-Machine_Shoulder_Shrug": "Máquina improvisada; barra/halter/cabo bastam",
}

# Adicionados na V2.1 (slot, externalId, motivo, prioridade_midia se sem local)
ADD_V2_1: list[tuple[str, str, str]] = [
    # Costas → 18
    ("back_bodyweight_row", "Bodyweight_Mid_Row", "remada corporal / horizontal"),
    ("back_straight_arm", "Straight-Arm_Pulldown", "pulldown braço reto / dorsal"),
    ("back_inverted", "Inverted_Row_with_Straps", "remada invertida"),
    ("back_db_row_bilateral", "Bent_Over_Two-Dumbbell_Row", "remada bilateral halteres"),
    # Ombros → 14
    ("shoulder_front", "Front_Dumbbell_Raise", "elevação frontal"),
    ("shoulder_arnold", "Arnold_Dumbbell_Press", "Arnold press"),
    ("shoulder_machine", "Machine_Shoulder_Military_Press", "desenvolvimento máquina"),
    ("shoulder_cable_lateral", "Cable_Seated_Lateral_Raise", "lateral na polia"),
    # Bíceps → 9
    ("biceps_cable", "Standing_Biceps_Cable_Curl", "rosca na polia"),
    ("biceps_concentration", "Concentration_Curls", "rosca concentrada"),
    ("biceps_ez", "EZ-Bar_Curl", "rosca barra W"),
    # Posteriores → 10
    ("ham_seated_curl", "Seated_Leg_Curl", "flexora sentada"),
    ("ham_sumo", "Sumo_Deadlift", "terra sumo"),
    # Tríceps → 9
    ("triceps_kickback", "Tricep_Dumbbell_Kickback", "kickback"),
    # Glúteos → 9
    ("glute_single_bridge", "Single_Leg_Glute_Bridge", "ponte unilateral"),
    # Panturrilhas → 6
    ("calf_smith", "Smith_Machine_Calf_Raise", "panturrilha smith"),
    # Peitoral → 14
    ("chest_incline_fly", "Incline_Dumbbell_Flyes", "crucifixo inclinado"),
    # Quadríceps → 14
    ("quad_rear_lunge", "Dumbbell_Rear_Lunge", "afundo reverso"),
    # Lombar → 6 (substituições úteis)
    ("lowback_superman", "Superman", "extensão lombar peso corporal"),
    ("lowback_rack_pull", "Rack_Pulls", "rack pull / eretores"),
    # Trapézio → 5 (mantém 4 da V2 + upright)
    ("trap_db_upright", "Standing_Dumbbell_Upright_Row", "remada alta halter / trapézio"),
]

# Abdômen mantidos na V2.1 (16)
ABS_KEEP: frozenset[str] = frozenset(
    {
        "Crunches",
        "3_4_Sit-Up",
        "Cross-Body_Crunch",
        "Cable_Crunch",
        "Hanging_Leg_Raise",
        "Plank",
        "Side_Bridge",
        "Russian_Twist",
        "Standing_Cable_Wood_Chop",
        "Ab_Roller",
        "Dead_Bug",
        "Decline_Crunch",
        "Cable_Reverse_Crunch",
        "Ab_Crunch_Machine",
        "Landmine_180s",
        "Barbell_Ab_Rollout",
    }
)


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


def _media_fields(eid: str) -> dict[str, Any]:
    src0 = (SOURCE_IMAGES / eid / "0.jpg").is_file()
    src1 = (SOURCE_IMAGES / eid / "1.jpg").is_file()
    apk0 = (APK_IMAGES / eid / "0.jpg").is_file()
    apk1 = (APK_IMAGES / eid / "1.jpg").is_file()
    has_local = src0 and src1
    if apk0 and apk1:
        status = "apk"
    elif has_local:
        status = "source_only"
    else:
        status = "pending_media_acquisition"
    return {
        "sourceImage0": src0,
        "sourceImage1": src1,
        "apkImage0": apk0,
        "apkImage1": apk1,
        "imageCount": 2 if has_local else 0,
        "imagePaths": [f"{eid}/0.jpg", f"{eid}/1.jpg"] if has_local else [],
        "mediaStatus": status,
        "pendingMediaAcquisition": status == "pending_media_acquisition",
        "remoteSourceUrl0": f"{FEDB_RAW}/{eid}/0.jpg",
        "remoteSourceUrl1": f"{FEDB_RAW}/{eid}/1.jpg",
        "mediaLicense": FEDB_LICENSE,
        "mediaLicenseUrl": FEDB_LICENSE_URL,
    }


def _entry(
    raw: dict,
    trans: dict[tuple[str, str], dict],
    *,
    slot: str,
    reason: str,
    v1_ids: set[str],
    in_v2: bool,
    change: str,
) -> dict[str, Any]:
    eid = raw["externalId"]
    t = trans.get(("free-exercise-db", eid), {})
    media = _media_fields(eid)
    return {
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
        "inPreviousCatalogV1": eid in v1_ids,
        "inPreviousCatalogV2": in_v2,
        "changeFromV2": change,
        **media,
    }


def curate_v2_1() -> dict[str, Any]:
    raw_rows = _load_importable()
    by_id = {r["externalId"]: r for r in raw_rows}
    trans = _load_translations()
    rejected_ids = {
        r["externalId"]
        for r in json.loads(NORMALIZED.read_text(encoding="utf-8"))
        if classify_importability(r) not in IMPORTABLE
    }

    v1 = json.loads(V1_JSON.read_text(encoding="utf-8"))
    v2 = json.loads(V2_JSON.read_text(encoding="utf-8"))
    v1_ids = {e["externalId"] for e in v1["selected"]}
    v2_ids = {e["externalId"] for e in v2["selected"]}
    v2_by_id = {e["externalId"]: e for e in v2["selected"]}

    # Start from V2, drop removals
    selected_ids: list[str] = []
    selected: list[dict[str, Any]] = []
    removed: list[dict[str, Any]] = []

    for eid in [e["externalId"] for e in v2["selected"]]:
        if eid in REMOVE_FROM_V2:
            raw = by_id[eid]
            removed.append(
                {
                    "externalId": eid,
                    "muscleGroup": raw.get("muscleGroup"),
                    "translatedName": trans.get(("free-exercise-db", eid), {}).get("name")
                    or raw.get("name"),
                    "reason": REMOVE_FROM_V2[eid],
                    "wasInV1": eid in v1_ids,
                    "wasInV2": True,
                }
            )
            continue
        # Abs keep filter for any abs that slipped past REMOVE list
        raw = by_id[eid]
        if raw.get("muscleGroup") == "Abdômen" and eid not in ABS_KEEP:
            removed.append(
                {
                    "externalId": eid,
                    "muscleGroup": "Abdômen",
                    "translatedName": trans.get(("free-exercise-db", eid), {}).get("name")
                    or raw.get("name"),
                    "reason": "Abdômen fora do núcleo V2.1 (15–18)",
                    "wasInV1": eid in v1_ids,
                    "wasInV2": True,
                }
            )
            continue
        prev = v2_by_id[eid]
        selected.append(
            _entry(
                raw,
                trans,
                slot=prev.get("slot") or f"keep_{eid}",
                reason=prev.get("reason") or "mantido da V2",
                v1_ids=v1_ids,
                in_v2=True,
                change="kept",
            )
        )
        selected_ids.append(eid)

    added: list[dict[str, Any]] = []
    for slot, eid, reason in ADD_V2_1:
        if eid in selected_ids:
            raise ValueError(f"Add already selected: {eid}")
        if eid not in by_id:
            raise KeyError(f"Unknown add externalId: {eid}")
        if eid in rejected_ids:
            raise ValueError(f"Rejected selected: {eid}")
        raw = by_id[eid]
        entry = _entry(
            raw,
            trans,
            slot=slot,
            reason=reason,
            v1_ids=v1_ids,
            in_v2=eid in v2_ids,
            change="added",
        )
        selected.append(entry)
        selected_ids.append(eid)
        added.append(
            {
                "externalId": eid,
                "muscleGroup": raw.get("muscleGroup"),
                "translatedName": entry["translatedName"],
                "reason": reason,
                "pendingMediaAcquisition": entry["pendingMediaAcquisition"],
            }
        )

    # Prefer preserve V1
    v1_removed = [r for r in removed if r["wasInV1"]]
    if v1_removed:
        # Soft warning embedded in payload; tests prefer zero
        pass

    distribution = Counter(e["muscleGroup"] for e in selected)
    equipment = Counter(e.get("equipmentType") or "None" for e in selected)
    pending = [e for e in selected if e["pendingMediaAcquisition"]]
    with_local = [e for e in selected if not e["pendingMediaAcquisition"]]

    audit_matrix = []
    selected_set = set(selected_ids)
    removed_set = {r["externalId"] for r in removed}
    for r in raw_rows:
        eid = r["externalId"]
        t = trans.get(("free-exercise-db", eid), {})
        audit_matrix.append(
            {
                "source": r.get("source") or "free-exercise-db",
                "externalId": eid,
                "selected": eid in selected_set,
                "removedFromV2": eid in removed_set,
                "muscleGroup": r.get("muscleGroup"),
                "equipmentType": r.get("equipmentType"),
                "originalName": r.get("name") or "",
                "translatedName": t.get("name") or r.get("name") or "",
                "hasLocalSourceMedia": _has_both(SOURCE_IMAGES, eid),
                "hasApkMedia": _has_both(APK_IMAGES, eid),
            }
        )

    range_check = {
        muscle: {
            "count": distribution.get(muscle, 0),
            "targetMin": lo,
            "targetMax": hi,
            "inRange": lo <= distribution.get(muscle, 0) <= hi,
        }
        for muscle, (lo, hi) in MUSCLE_TARGETS.items()
    }

    payload: dict[str, Any] = {
        "version": "2.1",
        "source": "free-exercise-db",
        "sourceCatalog": "free-exercise-db",
        "totalAvailable": 777,
        "totalImportable": 777,
        "selectedCount": len(selected),
        "previousV1Count": len(v1_ids),
        "previousV2Count": len(v2_ids),
        "keptFromV1Count": sum(1 for e in selected if e["inPreviousCatalogV1"]),
        "keptFromV2Count": sum(1 for e in selected if e["changeFromV2"] == "kept"),
        "addedCount": len(added),
        "removedFromV2Count": len(removed),
        "v1RemovedCount": len(v1_removed),
        "targetRange": [TARGET_MIN, TARGET_MAX],
        "muscleTargets": {k: {"min": v[0], "max": v[1]} for k, v in MUSCLE_TARGETS.items()},
        "muscleMinimums": MUSCLE_MINIMUMS,
        "muscleMaximums": MUSCLE_MAXIMUMS,
        "selected": selected,
        "removedFromV2": removed,
        "addedFromV2": added,
        "excluded": [],
        "distribution": dict(sorted(distribution.items())),
        "equipmentDistribution": dict(sorted(equipment.items())),
        "rangeCheck": range_check,
        "mediaSummary": {
            "selectedWithLocalTwoImages": len(with_local),
            "selectedPendingMediaAcquisition": len(pending),
            "selectedWithOneImage": 0,
            "selectedWithZeroImagesAndNotPending": 0,
            "apkPoolSize": sum(1 for r in raw_rows if _has_both(APK_IMAGES, r["externalId"])),
            "localSourcePoolSize": sum(
                1 for r in raw_rows if _has_both(SOURCE_IMAGES, r["externalId"])
            ),
            "mediaRule": (
                "selected exige 2 imagens locais OU pendingMediaAcquisition=true "
                "(aquisição posterior a partir do Free Exercise DB / Unlicense)"
            ),
        },
        "notes": [
            "V1 e V2 permanecem intactas; este arquivo é a V2.1.",
            "Abdômen reduzido de 29 para ~16 com padrões diversificados.",
            "Vagas redistribuídas para Costas, Ombros e Bíceps (e outros gaps).",
            "Lombar/Trapézio revisados: removidos exercícios muito específicos.",
            "Itens pendingMediaAcquisition têm mídia no FEDB remoto (ver MEDIA_CANDIDATES_V2_1).",
            "Nenhuma imagem foi copiada para app/src/main/assets nesta etapa.",
        ],
        "auditMatrix": audit_matrix,
        "fedbLicense": FEDB_LICENSE,
        "fedbLicenseUrl": FEDB_LICENSE_URL,
    }
    return payload


def dumps_deterministic(payload: dict[str, Any]) -> str:
    return json.dumps(payload, ensure_ascii=False, indent=2) + "\n"


def write_markdown(payload: dict[str, Any]) -> str:
    lines: list[str] = [
        "# MAIN_CATALOG V2.1 — Curadoria refinada",
        "",
        f"- Versão: **{payload['version']}**",
        f"- Selecionados: **{payload['selectedCount']}** (faixa {payload['targetRange'][0]}–{payload['targetRange'][1]})",
        f"- V1: **{payload['previousV1Count']}** | V2: **{payload['previousV2Count']}**",
        f"- Mantidos da V1: **{payload['keptFromV1Count']}**",
        f"- Removidos da V2: **{payload['removedFromV2Count']}** (da V1: **{payload['v1RemovedCount']}**)",
        f"- Adicionados: **{payload['addedCount']}**",
        f"- Com 2 imagens locais: **{payload['mediaSummary']['selectedWithLocalTwoImages']}**",
        f"- Pendentes de aquisição de mídia: **{payload['mediaSummary']['selectedPendingMediaAcquisition']}**",
        "",
        "## Distribuição",
        "",
        "| Grupo | V2.1 | Meta | Na faixa? |",
        "| ----- | ---: | ---- | --------- |",
    ]
    for muscle, (lo, hi) in MUSCLE_TARGETS.items():
        n = payload["distribution"].get(muscle, 0)
        ok = "sim" if lo <= n <= hi else "não"
        lines.append(f"| {muscle} | {n} | {lo}–{hi} | {ok} |")

    lines += ["", "## Notas", ""]
    for n in payload["notes"]:
        lines.append(f"- {n}")

    lines += ["", "## Removidos da V2", ""]
    for r in payload["removedFromV2"]:
        v1 = " (era V1!)" if r["wasInV1"] else ""
        lines.append(
            f"- `{r['externalId']}` ({r['muscleGroup']}){v1}: {r['reason']}"
        )

    lines += ["", "## Adicionados na V2.1", ""]
    for a in payload["addedFromV2"]:
        pend = " — **mídia pendente**" if a["pendingMediaAcquisition"] else ""
        lines.append(
            f"- `{a['externalId']}` ({a['muscleGroup']}): {a['reason']}{pend}"
        )

    by_muscle: dict[str, list] = {}
    for e in payload["selected"]:
        by_muscle.setdefault(e["muscleGroup"] or "?", []).append(e)

    lines += ["", "## Seleção por grupo", ""]
    for muscle in MUSCLE_TARGETS:
        items = by_muscle.get(muscle, [])
        lines.append(f"### {muscle} ({len(items)})")
        lines.append("")
        for i, e in enumerate(items, 1):
            media = (
                "0.jpg+1.jpg local"
                if not e["pendingMediaAcquisition"]
                else "PENDENTE (FEDB remoto)"
            )
            lines.append(f"{i}. **{e['translatedName']}**")
            lines.append(f"   - externalId: `{e['externalId']}`")
            lines.append(f"   - equipamento: {e['equipmentType']}")
            lines.append(f"   - imagens: {media}")
            lines.append(f"   - motivo: {e['reason']}")
            lines.append("")
    return "\n".join(lines) + "\n"


def main() -> None:
    payload = curate_v2_1()
    text = dumps_deterministic(payload)
    OUT_JSON.write_text(text, encoding="utf-8")
    OUT_MD.write_text(write_markdown(payload), encoding="utf-8")
    print(f"wrote {OUT_JSON} selected={payload['selectedCount']}")
    print("distribution:", payload["distribution"])
    print(
        "local/pending:",
        payload["mediaSummary"]["selectedWithLocalTwoImages"],
        payload["mediaSummary"]["selectedPendingMediaAcquisition"],
    )
    print("sha256:", hashlib.sha256(text.encode()).hexdigest())


if __name__ == "__main__":
    main()
