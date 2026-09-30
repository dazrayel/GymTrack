"""Stage 4.9 — Publish MAIN_CATALOG_V2_1 (136) as the GymTrack catalog pack.

Preserves the full Free Exercise DB normalized pack as:
  output/free-exercise-db/gymtrack-exercises-full-876.json

Writes the published pack (exactly the 136 V2.1 IDs) to:
  output/free-exercise-db/gymtrack-exercises.json
  app/src/main/assets/exercises/gymtrack-exercises.json

Does NOT modify Room, importer, translations, or MAIN_CATALOG_V2_1.json.
Does NOT download images — only validates existing APK media.
"""

from __future__ import annotations

import hashlib
import json
import sys
from collections import Counter
from datetime import datetime, timezone
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
REPO = ROOT.parents[1]

V2_1_JSON = ROOT / "analysis" / "MAIN_CATALOG_V2_1.json"
PUBLISHED_MD = ROOT / "analysis" / "PUBLISHED_CATALOG_V2_1.md"
OUT_DIR = ROOT / "output" / "free-exercise-db"
FULL_876 = OUT_DIR / "gymtrack-exercises-full-876.json"
PUBLISHED = OUT_DIR / "gymtrack-exercises.json"
ASSET = REPO / "app" / "src" / "main" / "assets" / "exercises" / "gymtrack-exercises.json"
ASSETS_DIR = REPO / "app" / "src" / "main" / "assets" / "exercises"
TEST_FIXTURE_DIR = REPO / "app" / "src" / "test" / "resources" / "exercises"
TEST_FIXTURE_FULL = TEST_FIXTURE_DIR / "gymtrack-exercises-full-876.json"

EXPECTED = 136
SOURCE = "free-exercise-db"


def sha256_bytes(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def dumps_deterministic(rows: list[dict[str, Any]]) -> str:
    return json.dumps(rows, ensure_ascii=False, indent=2) + "\n"


def load_v2_1_ids() -> list[tuple[str, str]]:
    payload = json.loads(V2_1_JSON.read_text(encoding="utf-8"))
    selected = payload["selected"]
    if len(selected) != EXPECTED:
        raise AssertionError(f"V2.1 selectedCount expected {EXPECTED}, got {len(selected)}")
    ids = [(e.get("externalSource") or e.get("source") or SOURCE, e["externalId"]) for e in selected]
    if len(ids) != len(set(ids)):
        raise AssertionError("Duplicate identities in MAIN_CATALOG_V2_1")
    # Deterministic publish order
    return sorted(ids, key=lambda t: (t[0], t[1]))


def ensure_full_archive(current: list[dict[str, Any]]) -> None:
    """Keep the full 876 pack for Stage 2.5 / curation analysis."""
    if len(current) != 876 and not FULL_876.exists():
        raise AssertionError(
            f"Current catalog has {len(current)} rows; expected 876 before first publish "
            f"or an existing {FULL_876.name}"
        )
    if len(current) == 876:
        text = dumps_deterministic(current)
        raw = text.encode("utf-8")
        if not FULL_876.exists() or FULL_876.read_bytes() != raw:
            FULL_876.write_bytes(raw)
        TEST_FIXTURE_DIR.mkdir(parents=True, exist_ok=True)
        if not TEST_FIXTURE_FULL.exists() or TEST_FIXTURE_FULL.read_bytes() != raw:
            TEST_FIXTURE_FULL.write_bytes(raw)
    elif FULL_876.exists():
        # Already published previously; archive must stay intact.
        archived = json.loads(FULL_876.read_text(encoding="utf-8"))
        if len(archived) != 876:
            raise AssertionError(f"Corrupt full archive size={len(archived)}")
    else:
        raise AssertionError("Cannot publish: full 876 archive missing and current pack is not 876")


def filter_published(
    full: list[dict[str, Any]],
    ordered_ids: list[tuple[str, str]],
) -> list[dict[str, Any]]:
    by_id = {(r.get("source") or SOURCE, r["externalId"]): r for r in full}
    out: list[dict[str, Any]] = []
    for key in ordered_ids:
        row = by_id.get(key)
        if row is None:
            raise KeyError(f"Selected exercise missing from full catalog: {key}")
        # Preserve the record object fields as-is (shallow copy of dict).
        out.append(dict(row))
    if len(out) != EXPECTED:
        raise AssertionError(f"Published size {len(out)} != {EXPECTED}")
    return out


def validate_media(rows: list[dict[str, Any]]) -> dict[str, Any]:
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
        "jpgFilesExpected": len(rows) * 2,
    }


def write_report(
    rows: list[dict[str, Any]],
    digest: str,
    media: dict[str, Any],
) -> None:
    dist = Counter(r.get("muscleGroup") or "?" for r in rows)
    lines = [
        "# PUBLISHED_CATALOG V2.1 — Stage 4.9",
        "",
        f"- Data (UTC): **{datetime.now(timezone.utc).strftime('%Y-%m-%d %H:%M:%S')}**",
        f"- Fonte de seleção: `analysis/MAIN_CATALOG_V2_1.json`",
        f"- Pack publicado: `output/free-exercise-db/gymtrack-exercises.json`",
        f"- Asset Android: `app/src/main/assets/exercises/gymtrack-exercises.json`",
        f"- Arquivo completo preservado: `output/free-exercise-db/gymtrack-exercises-full-876.json`",
        "",
        "## Contagens",
        "",
        "| Camada | Quantidade |",
        "| ------ | ---------: |",
        "| Free Exercise DB original | 876 |",
        "| Stage 2.5 importáveis | 777 |",
        "| Stage 2.5 rejeitados | 99 |",
        "| Curadoria V2.1 selecionados | 136 |",
        "| Importáveis não selecionados | 641 |",
        "| **Pack publicado (esta etapa)** | **136** |",
        "",
        f"- SHA-256 do pack publicado: `{digest}`",
        f"- Mídia: {media['with0']}/136 com `0.jpg`, {media['with1']}/136 com `1.jpg`",
        f"- Arquivos JPG esperados: **{media['jpgFilesExpected']}**",
        "",
        "## Distribuição (publicado)",
        "",
        "| Grupo | Qtd |",
        "| ----- | --: |",
    ]
    for muscle, n in sorted(dist.items()):
        lines.append(f"| {muscle} | {n} |")
    lines += [
        "",
        "## Notas",
        "",
        "- Os 641 importáveis não selecionados **não** são rejeitados; ficam fora do pacote.",
        "- Os 99 rejeitados Stage 2.5 continuam no arquivo completo de 876; não entram no pack publicado.",
        "- Identidade `(source, externalId)` e `sourceData` preservados byte-a-campo a partir do pack completo.",
        "- O importer **não** apaga exercícios ausentes do pack em instalações existentes.",
        "",
    ]
    PUBLISHED_MD.write_text("\n".join(lines), encoding="utf-8")


def publish() -> dict[str, Any]:
    ordered_ids = load_v2_1_ids()

    # Prefer full archive if already present; else current published path must be 876.
    if FULL_876.exists():
        full = json.loads(FULL_876.read_text(encoding="utf-8"))
    else:
        full = json.loads(PUBLISHED.read_text(encoding="utf-8"))
    ensure_full_archive(full if len(full) == 876 else json.loads(FULL_876.read_text(encoding="utf-8")))
    full = json.loads(FULL_876.read_text(encoding="utf-8"))

    published_rows = filter_published(full, ordered_ids)
    text = dumps_deterministic(published_rows)
    digest = sha256_bytes(text.encode("utf-8"))

    PUBLISHED.write_bytes(text.encode("utf-8"))
    ASSET.parent.mkdir(parents=True, exist_ok=True)
    ASSET.write_bytes(text.encode("utf-8"))

    # Identity checks
    keys = [(r.get("source"), r.get("externalId")) for r in published_rows]
    if any(s is None or not eid for s, eid in keys):
        raise AssertionError("Published row missing externalSource/externalId")
    if len(keys) != len(set(keys)):
        raise AssertionError("Duplicate identity in published pack")

    selected_set = set(ordered_ids)
    published_set = {(r.get("source") or SOURCE, r["externalId"]) for r in published_rows}
    if selected_set != published_set:
        raise AssertionError("Published IDs != V2.1 selected IDs")

    media = validate_media(published_rows)
    if media["missing0"] or media["missing1"]:
        raise AssertionError(f"Media incomplete: {media}")

    # Asset must match tools output exactly
    if ASSET.read_text(encoding="utf-8") != text:
        raise AssertionError("Asset JSON differs from tools output")

    write_report(published_rows, digest, media)
    return {
        "count": len(published_rows),
        "sha256": digest,
        "media": media,
        "distribution": dict(Counter(r.get("muscleGroup") for r in published_rows)),
    }


def main() -> None:
    result = publish()
    print(f"published={result['count']}")
    print(f"sha256={result['sha256']}")
    print(f"media={result['media']}")
    print(f"distribution={result['distribution']}")
    # Determinism check in-process
    again = publish()
    if again["sha256"] != result["sha256"]:
        raise SystemExit("Non-deterministic publish SHA")
    print("deterministic=OK")


if __name__ == "__main__":
    main()
