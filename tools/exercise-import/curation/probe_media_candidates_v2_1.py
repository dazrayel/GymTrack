"""Levantamento de mídia V2.1 — Free Exercise DB (sem download/cópia para o APK).

Verifica existência remota de 0.jpg/1.jpg no repositório oficial
https://github.com/yuhonas/free-exercise-db (Unlicense).

Gera:
  analysis/MEDIA_CANDIDATES_V2_1.json
  analysis/MEDIA_CANDIDATES_V2_1.md
"""

from __future__ import annotations

import hashlib
import json
import sys
import urllib.error
import urllib.request
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
REPO = ROOT.parents[1]
sys.path.insert(0, str(ROOT / "analysis"))
sys.path.insert(0, str(ROOT / "curation"))

from classify_importability import classify as classify_importability  # noqa: E402

from catalog_paths import analysis_catalog_path  # noqa: E402

NORMALIZED = analysis_catalog_path()
TRANSLATIONS = ROOT / "translate" / "translations.json"
V2_1_JSON = ROOT / "analysis" / "MAIN_CATALOG_V2_1.json"
OUT_JSON = ROOT / "analysis" / "MEDIA_CANDIDATES_V2_1.json"
OUT_MD = ROOT / "analysis" / "MEDIA_CANDIDATES_V2_1.md"
SOURCE_IMAGES = ROOT / "input" / "free-exercise-db" / "exercises"
APK_IMAGES = REPO / "app" / "src" / "main" / "assets" / "exercises"

FEDB_RAW = "https://raw.githubusercontent.com/yuhonas/free-exercise-db/main/exercises"
FEDB_REPO = "https://github.com/yuhonas/free-exercise-db"
FEDB_LICENSE = "Unlicense (public domain)"
FEDB_LICENSE_URL = "https://github.com/yuhonas/free-exercise-db/blob/main/LICENSE.md"

IMPORTABLE = frozenset({"IMPORTABLE", "IMPORTABLE_WITH_FALLBACK"})

# (externalId, priority A/B/C, why)
MEDIA_WATCHLIST: list[tuple[str, str, str]] = [
    # Priority A — gaps críticos
    ("Incline_Dumbbell_Flyes", "A", "Peitoral — crucifixo inclinado"),
    ("Arnold_Dumbbell_Press", "A", "Ombros — Arnold press"),
    ("EZ-Bar_Curl", "A", "Bíceps — barra W"),
    ("Sumo_Deadlift", "A", "Posteriores — terra sumo"),
    ("Front_Dumbbell_Raise", "A", "Ombros — elevação frontal"),
    ("Standing_Biceps_Cable_Curl", "A", "Bíceps — rosca polia"),
    ("Straight-Arm_Pulldown", "A", "Costas — pulldown braço reto"),
    ("Bodyweight_Mid_Row", "A", "Costas — remada corporal"),
    ("Bent_Over_Two-Dumbbell_Row", "A", "Costas — remada bilateral"),
    ("Inverted_Row_with_Straps", "A", "Costas — remada invertida"),
    ("Cable_Seated_Lateral_Raise", "A", "Ombros — lateral cabo"),
    ("Machine_Shoulder_Military_Press", "A", "Ombros — desenvolvimento máquina"),
    ("Concentration_Curls", "A", "Bíceps — concentrada"),
    ("Seated_Leg_Curl", "A", "Posteriores — flexora sentada"),
    ("Superman", "A", "Lombar — extensão corporal"),
    ("Rack_Pulls", "A", "Lombar — rack pull"),
    # Priority B — úteis
    ("Tricep_Dumbbell_Kickback", "B", "Tríceps — kickback"),
    ("Single_Leg_Glute_Bridge", "B", "Glúteos — ponte unilateral"),
    ("Dumbbell_Rear_Lunge", "B", "Quadríceps — afundo reverso"),
    ("Smith_Machine_Calf_Raise", "B", "Panturrilhas — smith"),
    ("Standing_Dumbbell_Upright_Row", "B", "Trapézio — remada alta"),
    ("Spider_Curl", "B", "Bíceps — spider (alternativa)"),
    ("Natural_Glute_Ham_Raise", "B", "Posteriores — GHR"),
    ("Decline_Dumbbell_Bench_Press", "B", "Peitoral — declinado DB"),
    ("One-Arm_Kettlebell_Row", "B", "Costas — remada KB extra"),
    ("Seated_Palms-Down_Barbell_Wrist_Curl", "B", "Antebraço — wrist curl reverse"),
    # Priority C — específicos / baixa urgência (não buscar ativamente além de HEAD)
    ("Romanian_Deadlift_from_Deficit", "C", "Posteriores — RDL deficit"),
    ("Lying_Glute", "C", "Glúteos — variação deitada"),
    ("Kettlebell_Pistol_Squat", "C", "Quadríceps — pistol avançado"),
    ("Dumbbell_Seated_One-Leg_Calf_Raise", "C", "Panturrilhas — unilateral DB"),
    ("Dumbbell_One-Arm_Triceps_Extension", "C", "Tríceps — OH unilateral"),
]


def _head_ok(url: str, timeout: float = 15.0) -> bool:
    req = urllib.request.Request(url, method="HEAD")
    try:
        with urllib.request.urlopen(req, timeout=timeout) as resp:
            return 200 <= getattr(resp, "status", 200) < 300
    except urllib.error.HTTPError as e:
        return False
    except Exception:
        return False


def _local_pair(folder: Path, eid: str) -> tuple[bool, bool]:
    return (
        (folder / eid / "0.jpg").is_file(),
        (folder / eid / "1.jpg").is_file(),
    )


def build_media_report(*, check_remote: bool = True) -> dict[str, Any]:
    catalog = {
        r["externalId"]: r
        for r in json.loads(NORMALIZED.read_text(encoding="utf-8"))
        if classify_importability(r) in IMPORTABLE
    }
    trans_raw = json.loads(TRANSLATIONS.read_text(encoding="utf-8"))
    tmap = {
        (t.get("source") or "free-exercise-db", t["externalId"]): t
        for t in trans_raw["translations"]
    }

    v2_1_selected: set[str] = set()
    pending_selected: set[str] = set()
    if V2_1_JSON.exists():
        v21 = json.loads(V2_1_JSON.read_text(encoding="utf-8"))
        for e in v21["selected"]:
            v2_1_selected.add(e["externalId"])
            if e.get("pendingMediaAcquisition"):
                pending_selected.add(e["externalId"])

    candidates: list[dict[str, Any]] = []
    seen: set[str] = set()

    for eid, priority, note in MEDIA_WATCHLIST:
        if eid in seen:
            continue
        seen.add(eid)
        raw = catalog.get(eid)
        if raw is None:
            candidates.append(
                {
                    "externalId": eid,
                    "name": eid,
                    "priority": priority,
                    "note": note,
                    "status": "NOT_FOUND",
                    "detail": "externalId ausente dos 777 importáveis",
                }
            )
            continue

        t = tmap.get(("free-exercise-db", eid), {})
        name = t.get("name") or raw.get("name") or eid
        local0, local1 = _local_pair(SOURCE_IMAGES, eid)
        apk0, apk1 = _local_pair(APK_IMAGES, eid)
        url0 = f"{FEDB_RAW}/{eid}/0.jpg"
        url1 = f"{FEDB_RAW}/{eid}/1.jpg"

        if local0 and local1:
            status = "ALREADY_AVAILABLE"
            found0 = found1 = True
            remote_checked = False
        elif priority == "C" and not check_remote:
            status = "NOT_FOUND"
            found0 = found1 = False
            remote_checked = False
        else:
            remote_checked = check_remote
            if check_remote:
                found0 = _head_ok(url0)
                found1 = _head_ok(url1)
                if found0 and found1:
                    status = "FOUND"
                elif not found0 and not found1:
                    status = "NOT_FOUND"
                else:
                    status = "INVALID_MEDIA"
            else:
                found0 = found1 = False
                status = "NOT_FOUND"

        candidates.append(
            {
                "externalId": eid,
                "name": name,
                "translatedName": name,
                "originalName": raw.get("name") or "",
                "muscleGroup": raw.get("muscleGroup"),
                "equipmentType": raw.get("equipmentType"),
                "priority": priority,
                "note": note,
                "inV21Selection": eid in v2_1_selected,
                "pendingInV21": eid in pending_selected,
                "currentMedia": {
                    "frame0": local0,
                    "frame1": local1,
                    "apkFrame0": apk0,
                    "apkFrame1": apk1,
                },
                "foundMedia": {
                    "frame0": found0 if remote_checked or (local0 and local1) else local0,
                    "frame1": found1 if remote_checked or (local0 and local1) else local1,
                    "remoteChecked": remote_checked or (local0 and local1),
                },
                "source": "free-exercise-db",
                "sourceRepo": FEDB_REPO,
                "sourceUrl": f"{FEDB_REPO}/tree/main/exercises/{eid}",
                "sourceUrl0": url0,
                "sourceUrl1": url1,
                "license": FEDB_LICENSE,
                "licenseUrl": FEDB_LICENSE_URL,
                "status": status,
                "copiedToApk": False,
                "downloadedLocally": False,
            }
        )

    # Ensure every V2.1 pending item appears even if missing from watchlist
    for eid in sorted(pending_selected - seen):
        raw = catalog[eid]
        t = tmap.get(("free-exercise-db", eid), {})
        name = t.get("name") or raw.get("name") or eid
        local0, local1 = _local_pair(SOURCE_IMAGES, eid)
        url0 = f"{FEDB_RAW}/{eid}/0.jpg"
        url1 = f"{FEDB_RAW}/{eid}/1.jpg"
        found0 = _head_ok(url0) if check_remote else False
        found1 = _head_ok(url1) if check_remote else False
        status = (
            "FOUND"
            if found0 and found1
            else ("ALREADY_AVAILABLE" if local0 and local1 else "NOT_FOUND")
        )
        candidates.append(
            {
                "externalId": eid,
                "name": name,
                "translatedName": name,
                "originalName": raw.get("name") or "",
                "muscleGroup": raw.get("muscleGroup"),
                "equipmentType": raw.get("equipmentType"),
                "priority": "A",
                "note": "Incluído na V2.1 com pendingMediaAcquisition",
                "inV21Selection": True,
                "pendingInV21": True,
                "currentMedia": {
                    "frame0": local0,
                    "frame1": local1,
                    "apkFrame0": (APK_IMAGES / eid / "0.jpg").is_file(),
                    "apkFrame1": (APK_IMAGES / eid / "1.jpg").is_file(),
                },
                "foundMedia": {
                    "frame0": found0,
                    "frame1": found1,
                    "remoteChecked": check_remote,
                },
                "source": "free-exercise-db",
                "sourceRepo": FEDB_REPO,
                "sourceUrl": f"{FEDB_REPO}/tree/main/exercises/{eid}",
                "sourceUrl0": url0,
                "sourceUrl1": url1,
                "license": FEDB_LICENSE,
                "licenseUrl": FEDB_LICENSE_URL,
                "status": status,
                "copiedToApk": False,
                "downloadedLocally": False,
            }
        )

    by_status = {}
    for c in candidates:
        by_status[c["status"]] = by_status.get(c["status"], 0) + 1

    return {
        "version": "2.1",
        "source": "free-exercise-db",
        "license": FEDB_LICENSE,
        "licenseUrl": FEDB_LICENSE_URL,
        "repo": FEDB_REPO,
        "checkRemote": check_remote,
        "copiedToApk": False,
        "candidateCount": len(candidates),
        "statusCounts": by_status,
        "candidates": candidates,
        "notes": [
            "Nenhuma imagem foi baixada ou copiada para app/src/main/assets.",
            "FOUND = 0.jpg e 1.jpg existem no repositório oficial (HEAD HTTP 200).",
            "Licença Unlicense (domínio público) — compatível com uso no GymTrack.",
            "Incorporação de mídia fica para etapa futura após revisão humana.",
        ],
    }


def dumps_deterministic(payload: dict[str, Any]) -> str:
    return json.dumps(payload, ensure_ascii=False, indent=2) + "\n"


def write_markdown(payload: dict[str, Any]) -> str:
    lines = [
        "# MEDIA_CANDIDATES V2.1",
        "",
        f"- Fonte: [{payload['repo']}]({payload['repo']})",
        f"- Licença: **{payload['license']}** (`{payload['licenseUrl']}`)",
        f"- Candidatos: **{payload['candidateCount']}**",
        f"- Copiado para APK: **{payload['copiedToApk']}**",
        f"- Status: `{payload['statusCounts']}`",
        "",
        "## Tabela",
        "",
        "| Exercício | Prioridade | Mídia encontrada | Fonte | Licença | Status |",
        "| --------- | ---------- | ---------------- | ----- | ------- | ------ |",
    ]
    for c in payload["candidates"]:
        found = (
            "sim"
            if c.get("foundMedia", {}).get("frame0") and c.get("foundMedia", {}).get("frame1")
            else "não"
        )
        lines.append(
            f"| {c.get('translatedName') or c['externalId']} (`{c['externalId']}`) | "
            f"{c['priority']} | {found} | free-exercise-db | Unlicense | {c['status']} |"
        )

    for prio in ("A", "B", "C"):
        lines += ["", f"## Prioridade {prio}", ""]
        rows = [c for c in payload["candidates"] if c["priority"] == prio]
        for c in rows:
            lines.append(
                f"- **{c.get('translatedName') or c['externalId']}** — {c['status']} — {c.get('note','')}"
            )
    lines.append("")
    for n in payload["notes"]:
        lines.append(f"- {n}")
    lines.append("")
    return "\n".join(lines)


def main() -> None:
    payload = build_media_report(check_remote=True)
    text = dumps_deterministic(payload)
    OUT_JSON.write_text(text, encoding="utf-8")
    OUT_MD.write_text(write_markdown(payload), encoding="utf-8")
    print(f"wrote {OUT_JSON} candidates={payload['candidateCount']}")
    print("status:", payload["statusCounts"])
    print("sha256:", hashlib.sha256(text.encode()).hexdigest())


if __name__ == "__main__":
    main()
