"""Publish Wave 2 V2.3: 172 V2.2 + 11 curated exercises (183 total).

Reads Wave 2 TEMP staging (media + hashes) + full 876 pack + current published 172.
Writes:
  - tools/exercise-import/output/free-exercise-db/gymtrack-exercises.json
  - app/src/main/assets/exercises/gymtrack-exercises.json
  - analysis/MAIN_CATALOG_V2_3.json
  - analysis/PUBLISHED_CATALOG_V2_3.md
  - analysis/MEDIA_MANIFEST_V2_3.md

Copies media from TEMP Wave 2 staging into app assets.
Does NOT rewrite MAIN_CATALOG_V2_2 / PUBLISHED_CATALOG_V2_2 / MEDIA_MANIFEST_V2_2.
Does NOT touch Room/importer logic.
Does NOT include High_Cable_Curls.
"""

from __future__ import annotations

import csv
import hashlib
import json
import shutil
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
V2_2_JSON = ROOT / "analysis" / "MAIN_CATALOG_V2_2.json"
V2_3_JSON = ROOT / "analysis" / "MAIN_CATALOG_V2_3.json"
PUBLISHED_MD = ROOT / "analysis" / "PUBLISHED_CATALOG_V2_3.md"
MEDIA_MD = ROOT / "analysis" / "MEDIA_MANIFEST_V2_3.md"

MEDIA_STAGING = Path(r"C:/Users/danil/AppData/Local/Temp/gymtrack_v22_wave2_media/exercises")
MEDIA_MANIFEST = Path(
    r"C:/Users/danil/AppData/Local/Temp/gymtrack_v22_wave2_media/MEDIA_MANIFEST_V2_2_WAVE2.tsv"
)

SOURCE = "free-exercise-db"
EXPECTED_PREV = 172
EXPECTED_NEW = 11
EXPECTED_TOTAL = 183

WAVE1_PENDING = {
    "Spider_Curl",
    "Dumbbell_Scaption",
    "Machine_Triceps_Extension",
    "Seated_One-arm_Cable_Pulley_Rows",
}
EXCLUDED_WAVE2 = {"High_Cable_Curls"}

# Approved display names + primary groups (curation Onda 2).
WAVE2: dict[str, dict[str, str]] = {
    "Smith_Machine_Incline_Bench_Press": {
        "name": "Supino inclinado no Smith",
        "muscleGroup": "Peitoral",
    },
    "Cable_Internal_Rotation": {
        "name": "Rotação interna na polia",
        "muscleGroup": "Ombros",
    },
    "Standing_Military_Press": {
        "name": "Desenvolvimento militar em pé",
        "muscleGroup": "Ombros",
    },
    "Standing_Low-Pulley_Deltoid_Raise": {
        "name": "Elevação lateral unilateral na polia baixa",
        "muscleGroup": "Ombros",
    },
    "Front_Cable_Raise": {
        "name": "Elevação frontal na polia",
        "muscleGroup": "Ombros",
    },
    "Split_Squat_with_Dumbbells": {
        "name": "Agachamento búlgaro com Halteres",
        "muscleGroup": "Quadríceps",
    },
    "Natural_Glute_Ham_Raise": {
        "name": "Glute-ham raise natural (GHR)",
        "muscleGroup": "Posteriores",
    },
    "Standing_Dumbbell_Triceps_Extension": {
        "name": "Extensão de tríceps acima da cabeça com Halter",
        "muscleGroup": "Tríceps",
    },
    "Cable_One_Arm_Tricep_Extension": {
        "name": "Extensão unilateral de tríceps na polia",
        "muscleGroup": "Tríceps",
    },
    "Zottman_Curl": {
        "name": "Rosca Zottman",
        "muscleGroup": "Bíceps",
    },
    "Reverse_Barbell_Curl": {
        "name": "Rosca inversa com barra",
        "muscleGroup": "Bíceps",
    },
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
    "Smith",
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


def load_manifest_hashes() -> dict[tuple[str, str], str]:
    rows = list(csv.DictReader(MEDIA_MANIFEST.open(encoding="utf-8"), delimiter="\t"))
    out: dict[tuple[str, str], str] = {}
    for r in rows:
        eid = r["externalId"]
        if eid not in WAVE2:
            continue
        if r.get("exerciseDecision") == "REJEITADO":
            raise AssertionError(f"Rejected media in publish set: {eid}")
        out[(eid, r["frame"])] = r["sha256"]
    expected = {(eid, fr) for eid in WAVE2 for fr in ("0", "1")}
    if set(out) != expected:
        raise AssertionError(f"Manifest keys mismatch: missing={expected - set(out)} extra={set(out) - expected}")
    return out


def build_new_row(full_row: dict[str, Any], meta: dict[str, str]) -> dict[str, Any]:
    eid = full_row["externalId"]
    primary = meta["muscleGroup"]
    if primary not in MUSCLE_GROUPS:
        raise ValueError(f"{eid}: invalid primary {primary!r}")
    if (full_row.get("muscleGroup") or "").strip() != primary:
        raise ValueError(
            f"{eid}: full pack muscleGroup {full_row.get('muscleGroup')!r} != curated {primary!r}"
        )
    eq = (full_row.get("equipmentType") or "").strip()
    if eq not in EQUIPMENT_TYPES:
        raise ValueError(f"{eid}: invalid/missing equipment {eq!r}")
    secondaries = [
        g
        for g in (full_row.get("secondaryMuscles") or [])
        if isinstance(g, str) and g.strip() in MUSCLE_GROUPS and g.strip() != primary
    ]
    # dedupe preserve order
    seen: set[str] = set()
    sec_clean: list[str] = []
    for g in secondaries:
        if g not in seen:
            seen.add(g)
            sec_clean.append(g)

    row = dict(full_row)
    row["source"] = SOURCE
    row["externalId"] = eid
    row["name"] = meta["name"]
    row["muscleGroup"] = primary
    row["secondaryMuscles"] = sec_clean
    row["equipmentType"] = eq
    return row


def copy_media(ids: list[str], expected_hashes: dict[tuple[str, str], str]) -> dict[str, Any]:
    copied = 0
    hash_ok = 0
    details: list[dict[str, Any]] = []
    for eid in ids:
        src_dir = MEDIA_STAGING / eid
        dest_dir = ASSETS_DIR / eid
        if dest_dir.exists():
            raise AssertionError(f"Asset folder already exists (collision): {dest_dir}")
        for name in ("0.jpg", "1.jpg"):
            frame = name[0]
            src = src_dir / name
            if not src.is_file():
                raise FileNotFoundError(src)
            data = src.read_bytes()
            if not is_jpeg(data):
                raise AssertionError(f"Invalid JPEG: {src}")
            src_hash = sha256_bytes(data)
            expected = expected_hashes[(eid, frame)]
            if src_hash != expected:
                raise AssertionError(f"Staging hash != manifest for {eid}/{name}")
            dest_dir.mkdir(parents=True, exist_ok=True)
            dest = dest_dir / name
            shutil.copy2(src, dest)
            dest_hash = sha256_file(dest)
            if dest_hash != src_hash:
                raise AssertionError(f"Hash mismatch after copy: {dest}")
            copied += 1
            hash_ok += 1
            details.append(
                {
                    "externalId": eid,
                    "frame": frame,
                    "sha256": dest_hash,
                    "bytes": len(data),
                }
            )
    return {"copied": copied, "hashMatched": hash_ok, "files": details}


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
    if len(WAVE2) != EXPECTED_NEW:
        raise AssertionError(f"WAVE2 map size {len(WAVE2)} != {EXPECTED_NEW}")
    if EXCLUDED_WAVE2 & set(WAVE2):
        raise AssertionError("Excluded IDs present in WAVE2")

    full = json.loads(FULL_876.read_text(encoding="utf-8"))
    if len(full) != 876:
        raise AssertionError(f"Full pack size {len(full)} != 876")
    full_by_id = {r["externalId"]: r for r in full}

    v22 = json.loads(V2_2_JSON.read_text(encoding="utf-8"))
    if v22.get("selectedCount") != EXPECTED_PREV:
        raise AssertionError(f"V2.2 selectedCount {v22.get('selectedCount')} != {EXPECTED_PREV}")

    current_pub = json.loads(ASSET_JSON.read_text(encoding="utf-8"))
    if len(current_pub) != EXPECTED_PREV:
        raise AssertionError(f"Current asset size {len(current_pub)} != {EXPECTED_PREV}")
    current_ids = {r["externalId"] for r in current_pub}
    v22_ids = {e["externalId"] for e in v22["selected"]}
    if current_ids != v22_ids:
        raise AssertionError("Current published IDs differ from MAIN_CATALOG_V2_2")

    new_ids = sorted(WAVE2.keys())
    overlap = set(new_ids) & current_ids
    if overlap:
        raise AssertionError(f"New IDs already published: {sorted(overlap)}")
    for eid in WAVE1_PENDING:
        if eid in new_ids or eid in current_ids:
            # pending must remain absent from published pack
            if eid in new_ids:
                raise AssertionError(f"Wave1 pending must not be published: {eid}")
    if "High_Cable_Curls" in current_ids or "High_Cable_Curls" in new_ids:
        raise AssertionError("High_Cable_Curls must not be published")

    expected_hashes = load_manifest_hashes()

    prev_rows = [dict(r) for r in current_pub]
    new_rows: list[dict[str, Any]] = []
    for eid in new_ids:
        if eid not in full_by_id:
            raise KeyError(eid)
        new_rows.append(build_new_row(full_by_id[eid], WAVE2[eid]))

    published = sorted(
        prev_rows + new_rows,
        key=lambda r: (r.get("source") or SOURCE, r["externalId"]),
    )
    if len(published) != EXPECTED_TOTAL:
        raise AssertionError(f"Published size {len(published)} != {EXPECTED_TOTAL}")
    keys = [(r.get("source"), r["externalId"]) for r in published]
    if len(keys) != len(set(keys)):
        raise AssertionError("Duplicate identities in published pack")

    # Preserve previous 172 identities byte-for-byte on curated fields
    prev_by = {r["externalId"]: r for r in prev_rows}
    for r in published:
        if r["externalId"] in prev_by:
            old = prev_by[r["externalId"]]
            for field in ("name", "muscleGroup", "equipmentType", "secondaryMuscles", "source"):
                if r.get(field) != old.get(field):
                    raise AssertionError(f"Previous record mutated: {r['externalId']}.{field}")

    media_copy = copy_media(new_ids, expected_hashes)

    text = dumps_deterministic(published)
    digest = sha256_bytes(text.encode("utf-8"))
    PUBLISHED_OUT.write_bytes(text.encode("utf-8"))
    ASSET_JSON.write_bytes(text.encode("utf-8"))
    if ASSET_JSON.read_text(encoding="utf-8") != text:
        raise AssertionError("Asset JSON differs from tools output")

    media = validate_all_media(published)
    if media["missing0"] or media["missing1"]:
        raise AssertionError(f"Media incomplete after publish: {media}")

    wave2_prev = {e["externalId"]: e.get("wave", "v2.2") for e in v22["selected"]}
    selected_payload = {
        "version": "V2.3",
        "wave": "wave2",
        "createdAt": datetime.now(timezone.utc).isoformat(),
        "previousVersion": "V2.2",
        "previousCount": EXPECTED_PREV,
        "addedCount": EXPECTED_NEW,
        "selectedCount": EXPECTED_TOTAL,
        "pendingExcludedWave1": sorted(WAVE1_PENDING),
        "excludedWave2": sorted(EXCLUDED_WAVE2),
        "excludedWave2Reasons": {
            "High_Cable_Curls": "Mídia FEDB representa extensão de tríceps deitada, não rosca na polia alta.",
        },
        "selected": [
            {
                "externalSource": SOURCE,
                "externalId": r["externalId"],
                "name": r["name"],
                "muscleGroup": r["muscleGroup"],
                "secondaryMuscles": r.get("secondaryMuscles") or [],
                "equipmentType": r["equipmentType"],
                "wave": (
                    "wave2"
                    if r["externalId"] in WAVE2
                    else wave2_prev.get(r["externalId"], "v2.2")
                ),
            }
            for r in published
        ],
        "addedExternalIds": new_ids,
        "mediaLicense": FEDB_LICENSE,
        "mediaLicenseUrl": FEDB_LICENSE_URL,
        "knownLimitations": [
            "Zottman_Curl: frames não mostram claramente a pronação na fase negativa.",
            "Natural_Glute_Ham_Raise: mídia mostra aparelho GHR dedicado (Hoist); instruções FEDB mencionam improvisação em lat pulldown/preacher.",
        ],
        "notes": [
            "Wave 2 adds 11 FEDB exercises with approved Unlicense media from TEMP staging.",
            "High_Cable_Curls excluded after visual rejection.",
            "Wave 1 media-pending IDs remain excluded.",
            "V2.2 historical reports are preserved unmodified.",
        ],
    }
    V2_3_JSON.write_text(
        json.dumps(selected_payload, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )

    dist = Counter(r.get("muscleGroup") or "?" for r in published)
    added_dist = Counter(r.get("muscleGroup") or "?" for r in new_rows)
    md = [
        "# PUBLISHED_CATALOG V2.3 — Wave 2",
        "",
        f"- Data (UTC): **{datetime.now(timezone.utc).strftime('%Y-%m-%d %H:%M:%S')}**",
        f"- Base anterior: V2.2 (**{EXPECTED_PREV}**)",
        f"- Adicionados nesta onda: **{EXPECTED_NEW}**",
        f"- **Pack publicado: {EXPECTED_TOTAL}**",
        f"- SHA-256: `{digest}`",
        f"- Asset: `app/src/main/assets/exercises/gymtrack-exercises.json`",
        f"- Tools: `output/free-exercise-db/gymtrack-exercises.json`",
        f"- Seleção: `analysis/MAIN_CATALOG_V2_3.json`",
        "",
        "## Contagens",
        "",
        "| Camada | Quantidade |",
        "| ------ | ---------: |",
        "| Free Exercise DB completo | 876 |",
        "| Publicado V2.2 (preservado) | 172 |",
        "| Wave 2 adicionados | 11 |",
        "| Excluído Wave 2 (mídia) | 1 (`High_Cable_Curls`) |",
        "| Pendentes Wave 1 (inalterados) | 4 |",
        f"| **Pack publicado V2.3** | **{EXPECTED_TOTAL}** |",
        "",
        f"- Mídia total: {media['with0']}/{EXPECTED_TOTAL} com `0.jpg`, {media['with1']}/{EXPECTED_TOTAL} com `1.jpg`",
        f"- Novos JPG copiados: **{media_copy['copied']}** (hash match: {media_copy['hashMatched']})",
        f"- Licença mídia: **{FEDB_LICENSE}** ({FEDB_LICENSE_URL})",
        f"- Staging: `%TEMP%\\gymtrack_v22_wave2_media\\exercises`",
        "",
        "## IDs adicionados (11)",
        "",
    ]
    for eid in new_ids:
        row = next(r for r in new_rows if r["externalId"] == eid)
        md.append(
            f"- `{eid}` — {row['name']} | {row['muscleGroup']} | {row['equipmentType']}"
        )
    md += [
        "",
        "## Excluídos desta onda",
        "",
        "- `High_Cable_Curls` — mídia FEDB = extensão de tríceps deitada (não rosca na polia alta)",
        "",
        "## Pendentes Wave 1 (preservados, não publicados)",
        "",
        "- `Spider_Curl`",
        "- `Dumbbell_Scaption`",
        "- `Machine_Triceps_Extension`",
        "- `Seated_One-arm_Cable_Pulley_Rows`",
        "",
        "## Limitações conhecidas",
        "",
        "- **`Zottman_Curl`:** os dois frames não mostram claramente a pronação na fase negativa; início neutro e pico supinado são coerentes, mas a característica completa não foi comprovada visualmente.",
        "- **`Natural_Glute_Ham_Raise`:** a mídia mostra aparelho GHR dedicado (Hoist); as instruções textuais FEDB mencionam improvisação em lat pulldown/preacher. O padrão GHR visual foi aprovado.",
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
        "## Distribuição (somente Wave 2)",
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
        "- Os 172 registros V2.2 foram preservados (identidade e campos do pack anterior).",
        "- Relatórios V2.1/V2.2 históricos não foram reescritos.",
        "- Importador continua por upsert; ausentes do pack não são apagados.",
        "",
    ]
    PUBLISHED_MD.write_text("\n".join(md), encoding="utf-8")

    media_md = [
        "# MEDIA_MANIFEST V2.3 — Wave 2",
        "",
        f"- Pack: **{EXPECTED_TOTAL}** exercícios",
        f"- Com 0.jpg+1.jpg: **{EXPECTED_TOTAL}**",
        f"- Novos nesta onda: **{EXPECTED_NEW}** (**{media_copy['copied']}** JPG)",
        f"- Origem: staging `%TEMP%\\gymtrack_v22_wave2_media\\exercises`",
        f"- Manifesto staging: `%TEMP%\\gymtrack_v22_wave2_media\\MEDIA_MANIFEST_V2_2_WAVE2.tsv`",
        f"- Licença: **{FEDB_LICENSE}** ({FEDB_LICENSE_URL})",
        f"- Copiados com hash verificado: **{media_copy['hashMatched']}/{media_copy['copied']}**",
        "",
        "## Novos IDs (hashes destino = staging)",
        "",
        "| ID | Frame | SHA-256 | Bytes |",
        "| --- | ---: | --- | ---: |",
    ]
    for f in media_copy["files"]:
        media_md.append(
            f"| `{f['externalId']}` | {f['frame']} | `{f['sha256']}` | {f['bytes']} |"
        )
    media_md += [
        "",
        "## Excluídos / não publicados",
        "",
        "- `High_Cable_Curls` — rejeitado na inspeção visual (exercício errado na mídia FEDB)",
        "- Pendentes Wave 1 (sem mídia publicada): Spider_Curl, Dumbbell_Scaption, Machine_Triceps_Extension, Seated_One-arm_Cable_Pulley_Rows",
        "",
        "## Histórico",
        "",
        "- V2.1 / V2.2 manifests históricos permanecem em `MEDIA_MANIFEST_V2_1.*` e `MEDIA_MANIFEST_V2_2.md`.",
        "",
    ]
    MEDIA_MD.write_text("\n".join(media_md), encoding="utf-8")

    return {
        "count": len(published),
        "added": new_ids,
        "sha256": digest,
        "media": media,
        "mediaCopy": {"copied": media_copy["copied"], "hashMatched": media_copy["hashMatched"]},
        "distribution": dict(dist),
    }


def main() -> None:
    result = publish()
    print(f"published={result['count']}")
    print(f"added={len(result['added'])}")
    print(f"sha256={result['sha256']}")
    print(f"media={result['media']}")
    print(f"mediaCopy={result['mediaCopy']}")
    print("addedIds=" + ",".join(result["added"]))


if __name__ == "__main__":
    main()
