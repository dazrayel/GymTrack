"""Publish Wave 1 V2.2: 136 V2.1 + 36 curated exercises (172 total).

Reads approved TEMP artifacts + full 876 pack. Writes:
  - tools/exercise-import/output/free-exercise-db/gymtrack-exercises.json
  - app/src/main/assets/exercises/gymtrack-exercises.json
  - analysis/MAIN_CATALOG_V2_2.json
  - analysis/PUBLISHED_CATALOG_V2_2.md
  - analysis/MEDIA_MANIFEST_V2_2.md

Copies media from TEMP final staging into app assets.
Does NOT modify MAIN_CATALOG_V2_1.* historical reports.
Does NOT touch Room/importer logic.
"""

from __future__ import annotations

import csv
import hashlib
import json
import shutil
import struct
from collections import Counter
from datetime import datetime, timezone
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
REPO = ROOT.parents[1]

FULL_876 = ROOT / "output" / "free-exercise-db" / "gymtrack-exercises-full-876.json"
PUBLISHED_OUT = ROOT / "output" / "free-exercise-db" / "gymtrack-exercises.json"
ASSET_JSON = REPO / "app" / "src" / "main" / "assets" / "exercises" / "gymtrack-exercises.json"
ASSETS_DIR = REPO / "app" / "src" / "main" / "assets" / "exercises"
V2_1_JSON = ROOT / "analysis" / "MAIN_CATALOG_V2_1.json"
V2_2_JSON = ROOT / "analysis" / "MAIN_CATALOG_V2_2.json"
PUBLISHED_MD = ROOT / "analysis" / "PUBLISHED_CATALOG_V2_2.md"
MEDIA_MD = ROOT / "analysis" / "MEDIA_MANIFEST_V2_2.md"

WAVE_TSV = Path(r"C:/Users/danil/AppData/Local/Temp/gymtrack_v22_wave1_final.tsv")
MEDIA_MANIFEST = Path(r"C:/Users/danil/AppData/Local/Temp/gymtrack_v22_wave1_media_final_manifest.tsv")
MEDIA_STAGING = Path(r"C:/Users/danil/AppData/Local/Temp/gymtrack_v22_wave1_media_final/exercises")

SOURCE = "free-exercise-db"
EXPECTED_PREV = 136
EXPECTED_NEW = 36
EXPECTED_TOTAL = 172
PENDING = {
    "Spider_Curl",
    "Dumbbell_Scaption",
    "Machine_Triceps_Extension",
    "Seated_One-arm_Cable_Pulley_Rows",
}
# Equipment gaps in FEDB (null) resolved for publication with documented evidence.
EQUIPMENT_RESOLUTIONS = {
    "Mountain_Climbers": (
        "Peso corporal",
        "Movimento corporal no solo; mídia aprovada sem aparelho; FEDB equipment=null.",
    ),
    "Inverted_Row": (
        "Barra",
        "Instruções FEDB e mídia aprovada mostram barra em rack; FEDB equipment=null.",
    ),
}
MUSCLE_GROUPS = {
    "Abdômen",
    "Antebraço",
    "Bíceps",
    "Costas",
    "Glúteos",
    "Lombar",
    "Ombros",
    "Panturrilhas",
    "Peitoral",
    "Posteriores",
    "Quadríceps",
    "Trapézio",
    "Tríceps",
}
EQUIPMENT_TYPES = {
    "Barra",
    "Halteres",
    "Peso corporal",
    "Cabos",
    "Máquina",
    "Outro",
    "Kettlebell",
    "Elástico",
}
FEDB_LICENSE = "Unlicense (public domain)"
FEDB_LICENSE_URL = "https://github.com/yuhonas/free-exercise-db/blob/main/LICENSE.md"


def sha256_file(path: Path) -> str:
    h = hashlib.sha256()
    with path.open("rb") as f:
        for chunk in iter(lambda: f.read(65536), b""):
            h.update(chunk)
    return h.hexdigest()


def sha256_bytes(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def dumps_deterministic(rows: list[dict[str, Any]]) -> str:
    return json.dumps(rows, ensure_ascii=False, indent=2) + "\n"


def is_jpeg(data: bytes) -> bool:
    return len(data) >= 4 and data[:2] == b"\xff\xd8" and data[-2:] == b"\xff\xd9"


def parse_secondaries(raw: str, primary: str) -> list[str]:
    out: list[str] = []
    for part in (raw or "").split(","):
        g = part.strip()
        if not g or g == primary:
            continue
        if g not in MUSCLE_GROUPS:
            raise ValueError(f"Invalid secondary muscle {g!r} for primary {primary}")
        if g not in out:
            out.append(g)
    return out


def load_wave() -> dict[str, dict[str, str]]:
    rows = list(csv.DictReader(WAVE_TSV.open(encoding="utf-8"), delimiter="\t"))
    return {r["externalId"]: r for r in rows}


def load_media_ids() -> list[str]:
    rows = list(csv.DictReader(MEDIA_MANIFEST.open(encoding="utf-8"), delimiter="\t"))
    ids = [r["externalId"] for r in rows]
    if len(ids) != EXPECTED_NEW or len(set(ids)) != EXPECTED_NEW:
        raise AssertionError(f"Media manifest expected {EXPECTED_NEW} unique, got {len(ids)}")
    bad = set(ids) & PENDING
    if bad:
        raise AssertionError(f"Pending IDs in media manifest: {bad}")
    return sorted(ids)


def build_new_row(full_row: dict[str, Any], wave: dict[str, str]) -> dict[str, Any]:
    eid = full_row["externalId"]
    primary = (wave.get("muscleGroup_primary") or "").strip()
    if primary not in MUSCLE_GROUPS:
        raise ValueError(f"{eid}: invalid primary {primary!r}")
    name = (wave.get("name_display_pt") or "").strip()
    if not name:
        raise ValueError(f"{eid}: missing display name")
    eq = (wave.get("equipmentType") or "").strip()
    if not eq and eid in EQUIPMENT_RESOLUTIONS:
        eq, _ = EQUIPMENT_RESOLUTIONS[eid]
    if eq not in EQUIPMENT_TYPES:
        raise ValueError(f"{eid}: invalid/missing equipment {eq!r}")
    secondaries = parse_secondaries(wave.get("muscleGroup_secondary") or "", primary)

    row = dict(full_row)
    row["source"] = SOURCE
    row["externalId"] = eid
    row["name"] = name
    row["muscleGroup"] = primary
    row["secondaryMuscles"] = secondaries
    row["equipmentType"] = eq
    # Preserve sourceData / issues from full pack
    return row


def copy_media(ids: list[str]) -> dict[str, Any]:
    copied = 0
    hash_ok = 0
    for eid in ids:
        src_dir = MEDIA_STAGING / eid
        dest_dir = ASSETS_DIR / eid
        if dest_dir.exists():
            raise AssertionError(f"Asset folder already exists (collision): {dest_dir}")
        for name in ("0.jpg", "1.jpg"):
            src = src_dir / name
            if not src.is_file():
                raise FileNotFoundError(src)
            data = src.read_bytes()
            if not is_jpeg(data):
                raise AssertionError(f"Invalid JPEG: {src}")
            dest_dir.mkdir(parents=True, exist_ok=True)
            dest = dest_dir / name
            shutil.copy2(src, dest)
            copied += 1
            if sha256_file(dest) == sha256_file(src):
                hash_ok += 1
            else:
                raise AssertionError(f"Hash mismatch after copy: {dest}")
    return {"copied": copied, "hashMatched": hash_ok}


def validate_all_media(rows: list[dict[str, Any]]) -> dict[str, Any]:
    missing0 = missing1 = 0
    for r in rows:
        eid = r["externalId"]
        if not (ASSETS_DIR / eid / "0.jpg").is_file():
            missing0 += 1
        if not (ASSETS_DIR / eid / "1.jpg").is_file():
            missing1 += 1
    return {
        "exercises": len(rows),
        "with0": len(rows) - missing0,
        "with1": len(rows) - missing1,
        "missing0": missing0,
        "missing1": missing1,
    }


def publish() -> dict[str, Any]:
    full = json.loads(FULL_876.read_text(encoding="utf-8"))
    if len(full) != 876:
        raise AssertionError(f"Full pack size {len(full)} != 876")
    full_by_id = {r["externalId"]: r for r in full}

    v21 = json.loads(V2_1_JSON.read_text(encoding="utf-8"))
    prev_ids = [e["externalId"] for e in v21["selected"]]
    if len(prev_ids) != EXPECTED_PREV:
        raise AssertionError(f"V2.1 selected {len(prev_ids)} != {EXPECTED_PREV}")

    # Current published must still be the 136 V2.1 set
    current_pub = json.loads(ASSET_JSON.read_text(encoding="utf-8"))
    if len(current_pub) != EXPECTED_PREV:
        raise AssertionError(f"Current asset size {len(current_pub)} != {EXPECTED_PREV}")
    current_ids = {r["externalId"] for r in current_pub}
    if current_ids != set(prev_ids):
        raise AssertionError("Current published IDs differ from MAIN_CATALOG_V2_1")

    wave = load_wave()
    new_ids = load_media_ids()
    overlap = set(new_ids) & current_ids
    if overlap:
        raise AssertionError(f"New IDs already published: {sorted(overlap)}")
    for eid in PENDING:
        if eid in new_ids:
            raise AssertionError(f"Pending ID must not be published: {eid}")

    # Build previous rows in stable sorted order (same as V2.1 publish)
    prev_rows = [
        dict(full_by_id[eid])
        for eid in sorted(prev_ids)
    ]
    # Ensure names/groups from current published are preserved (already curated)
    pub_by_id = {r["externalId"]: r for r in current_pub}
    for i, row in enumerate(prev_rows):
        curated = pub_by_id[row["externalId"]]
        prev_rows[i] = dict(curated)

    new_rows: list[dict[str, Any]] = []
    equipment_notes: list[str] = []
    for eid in new_ids:
        if eid not in full_by_id:
            raise KeyError(eid)
        if eid not in wave:
            raise KeyError(f"Missing wave metadata for {eid}")
        if eid in EQUIPMENT_RESOLUTIONS:
            equipment_notes.append(f"{eid}: {EQUIPMENT_RESOLUTIONS[eid][1]}")
        new_rows.append(build_new_row(full_by_id[eid], wave[eid]))

    published = sorted(prev_rows + new_rows, key=lambda r: (r.get("source") or SOURCE, r["externalId"]))
    if len(published) != EXPECTED_TOTAL:
        raise AssertionError(f"Published size {len(published)} != {EXPECTED_TOTAL}")
    keys = [(r.get("source"), r["externalId"]) for r in published]
    if len(keys) != len(set(keys)):
        raise AssertionError("Duplicate identities in published pack")

    # Media first (fail before writing JSON if media incomplete)
    media_copy = copy_media(new_ids)

    text = dumps_deterministic(published)
    digest = sha256_bytes(text.encode("utf-8"))
    PUBLISHED_OUT.write_bytes(text.encode("utf-8"))
    ASSET_JSON.write_bytes(text.encode("utf-8"))
    if ASSET_JSON.read_text(encoding="utf-8") != text:
        raise AssertionError("Asset JSON differs from tools output")

    media = validate_all_media(published)
    if media["missing0"] or media["missing1"]:
        raise AssertionError(f"Media incomplete after publish: {media}")

    # Write V2.2 selection catalog
    selected_payload = {
        "version": "V2.2",
        "wave": "wave1",
        "createdAt": datetime.now(timezone.utc).isoformat(),
        "previousVersion": "V2.1",
        "previousCount": EXPECTED_PREV,
        "addedCount": EXPECTED_NEW,
        "selectedCount": EXPECTED_TOTAL,
        "pendingExcluded": sorted(PENDING),
        "equipmentResolutions": {
            k: {"equipmentType": v[0], "reason": v[1]} for k, v in EQUIPMENT_RESOLUTIONS.items()
        },
        "selected": [
            {
                "externalSource": SOURCE,
                "externalId": r["externalId"],
                "name": r["name"],
                "muscleGroup": r["muscleGroup"],
                "secondaryMuscles": r.get("secondaryMuscles") or [],
                "equipmentType": r["equipmentType"],
                "wave": "v2.1" if r["externalId"] in current_ids else "wave1",
            }
            for r in published
        ],
        "addedExternalIds": new_ids,
        "mediaLicense": FEDB_LICENSE,
        "mediaLicenseUrl": FEDB_LICENSE_URL,
        "notes": [
            "Wave 1 adds 36 FEDB exercises with approved Unlicense media.",
            "Four wave1 candidates remain pending media and are excluded.",
            "V2.1 historical reports (MAIN_CATALOG_V2_1, PUBLISHED_CATALOG_V2_1) are preserved.",
        ],
    }
    V2_2_JSON.write_text(json.dumps(selected_payload, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

    dist = Counter(r.get("muscleGroup") or "?" for r in published)
    added_dist = Counter(r.get("muscleGroup") or "?" for r in new_rows)
    md = [
        "# PUBLISHED_CATALOG V2.2 — Wave 1",
        "",
        f"- Data (UTC): **{datetime.now(timezone.utc).strftime('%Y-%m-%d %H:%M:%S')}**",
        f"- Base anterior: V2.1 (**{EXPECTED_PREV}**)",
        f"- Adicionados nesta onda: **{EXPECTED_NEW}**",
        f"- **Pack publicado: {EXPECTED_TOTAL}**",
        f"- SHA-256: `{digest}`",
        f"- Asset: `app/src/main/assets/exercises/gymtrack-exercises.json`",
        f"- Tools: `output/free-exercise-db/gymtrack-exercises.json`",
        f"- Seleção: `analysis/MAIN_CATALOG_V2_2.json`",
        "",
        "## Contagens",
        "",
        "| Camada | Quantidade |",
        "| ------ | ---------: |",
        "| Free Exercise DB completo | 876 |",
        "| Publicado V2.1 (preservado) | 136 |",
        "| Wave 1 adicionados | 36 |",
        "| Pendentes excluídos (mídia) | 4 |",
        f"| **Pack publicado V2.2** | **{EXPECTED_TOTAL}** |",
        "",
        f"- Mídia total: {media['with0']}/{EXPECTED_TOTAL} com `0.jpg`, {media['with1']}/{EXPECTED_TOTAL} com `1.jpg`",
        f"- Novos JPG copiados: **{media_copy['copied']}** (hash match: {media_copy['hashMatched']})",
        f"- Licença mídia: **{FEDB_LICENSE}** ({FEDB_LICENSE_URL})",
        "",
        "## IDs adicionados (36)",
        "",
    ]
    for eid in new_ids:
        row = next(r for r in new_rows if r["externalId"] == eid)
        md.append(
            f"- `{eid}` — {row['name']} | {row['muscleGroup']} | {row['equipmentType']}"
        )
    md += [
        "",
        "## Pendentes excluídos",
        "",
        "- `Spider_Curl` — apenas 1 frame válido; 2º frame é preacher; CC BY-SA não aprovada",
        "- `Dumbbell_Scaption` — mídia não representa scaption",
        "- `Machine_Triceps_Extension` — mídia é peck deck",
        "- `Seated_One-arm_Cable_Pulley_Rows` — mídia é remada bilateral",
        "",
        "## Resolução de equipamento (2 IDs)",
        "",
    ]
    for eid, (eq, reason) in EQUIPMENT_RESOLUTIONS.items():
        md.append(f"- `{eid}` → **{eq}**: {reason}")
    md += [
        "",
        "## Distribuição (pack completo)",
        "",
        "| Grupo | Qtd |",
        "| ----- | --: |",
    ]
    for g, n in sorted(dist.items()):
        md.append(f"| {g} | {n} |")
    md += [
        "",
        "## Distribuição (somente Wave 1)",
        "",
        "| Grupo | Qtd |",
        "| ----- | --: |",
    ]
    for g, n in sorted(added_dist.items()):
        md.append(f"| {g} | {n} |")
    md += [
        "",
        "## Notas",
        "",
        "- Os 136 registros V2.1 foram preservados (identidade e campos do pack anterior).",
        "- Relatórios V2.1 históricos não foram reescritos.",
        "- Importador continua por upsert; ausentes do pack não são apagados.",
        "",
    ]
    PUBLISHED_MD.write_text("\n".join(md), encoding="utf-8")

    media_md = [
        "# MEDIA_MANIFEST V2.2 — Wave 1",
        "",
        f"- Pack: **{EXPECTED_TOTAL}** exercícios",
        f"- Com 0.jpg+1.jpg: **{EXPECTED_TOTAL}**",
        f"- Novos nesta onda: **{EXPECTED_NEW}** (72 JPG)",
        f"- Origem: staging final `%TEMP%\\gymtrack_v22_wave1_media_final\\exercises`",
        f"- Licença: **{FEDB_LICENSE}**",
        f"- Copiados com hash verificado: **{media_copy['hashMatched']}/{media_copy['copied']}**",
        "",
        "## Pendentes sem mídia publicada",
        "",
        "- Spider_Curl, Dumbbell_Scaption, Machine_Triceps_Extension, Seated_One-arm_Cable_Pulley_Rows",
        "",
    ]
    MEDIA_MD.write_text("\n".join(media_md), encoding="utf-8")

    return {
        "count": len(published),
        "added": new_ids,
        "sha256": digest,
        "media": media,
        "mediaCopy": media_copy,
        "distribution": dict(dist),
        "equipmentNotes": equipment_notes,
    }


def main() -> None:
    result = publish()
    print(f"published={result['count']}")
    print(f"added={len(result['added'])}")
    print(f"sha256={result['sha256']}")
    print(f"media={result['media']}")
    print(f"mediaCopy={result['mediaCopy']}")
    for note in result["equipmentNotes"]:
        print(f"equipmentNote: {note}")


if __name__ == "__main__":
    main()
