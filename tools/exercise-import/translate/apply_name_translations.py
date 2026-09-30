"""Apply approved pt-BR names from translations.json onto the normalized catalog.

Etapa 4.8 — publishes presentation names only.

- Reads translations.json (official name translations).
- Updates top-level `name` for the 777 importable exercises.
- Leaves rejected exercises unchanged.
- Never modifies sourceData, identity, muscleGroup, equipmentType, or secondaryMuscles.
- Does not alter normalization maps or importability rules.

Usage (repo root):

    py -3 tools/exercise-import/translate/apply_name_translations.py
    py -3 tools/exercise-import/translate/apply_name_translations.py --copy-asset
"""

from __future__ import annotations

import argparse
import hashlib
import json
import shutil
import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "curation"))
sys.path.insert(0, str(ROOT / "analysis"))

from catalog_paths import (  # noqa: E402
    FULL_876_CATALOG,
    PUBLISHED_CATALOG,
    analysis_catalog_path,
)
from classify_importability import classify as classify_importability  # noqa: E402

NORMALIZED = analysis_catalog_path()
TRANSLATIONS = ROOT / "translate" / "translations.json"
ASSET = ROOT.parents[1] / "app" / "src" / "main" / "assets" / "exercises" / "gymtrack-exercises.json"

IMPORTABLE = frozenset({"IMPORTABLE", "IMPORTABLE_WITH_FALLBACK"})
IMMUTABLE_FIELDS = (
    "source",
    "externalId",
    "muscleGroup",
    "secondaryMuscles",
    "equipmentType",
    "issues",
    "sourceData",
)


def dumps_deterministic(payload: Any) -> str:
    return json.dumps(payload, ensure_ascii=False, indent=2) + "\n"


def load_translations(path: Path = TRANSLATIONS) -> dict[tuple[str, str], dict]:
    data = json.loads(path.read_text(encoding="utf-8"))
    out: dict[tuple[str, str], dict] = {}
    for t in data["translations"]:
        key = (t.get("source") or "free-exercise-db", t["externalId"])
        out[key] = t
    return out


def translation_status_counts(path: Path = TRANSLATIONS) -> dict[str, int]:
    data = json.loads(path.read_text(encoding="utf-8"))
    return dict(data["statusCounts"])


def apply_names(
    catalog: list[dict],
    translations: dict[tuple[str, str], dict],
) -> tuple[list[dict], dict[str, int]]:
    """Return a new catalog list with presentation names applied."""
    missing: list[str] = []
    changed = 0
    unchanged_name = 0
    skipped_rejected = 0

    result: list[dict] = []
    for row in catalog:
        new_row = json.loads(json.dumps(row, ensure_ascii=False))  # deep copy via JSON
        label = classify_importability(row)
        if label not in IMPORTABLE:
            skipped_rejected += 1
            result.append(new_row)
            continue

        key = (row.get("source") or "free-exercise-db", row["externalId"])
        t = translations.get(key)
        if t is None:
            missing.append(row["externalId"])
            result.append(new_row)
            continue

        translated = t.get("name") or ""
        if not translated.strip():
            raise ValueError(f"Empty translated name for {key}")

        if new_row.get("name") == translated:
            unchanged_name += 1
        else:
            new_row["name"] = translated
            changed += 1
        result.append(new_row)

    if missing:
        raise SystemExit(
            f"Missing translations for {len(missing)} importable exercise(s): "
            f"{missing[:5]}..."
        )

    stats = {
        "total": len(catalog),
        "importable": sum(
            1 for r in catalog if classify_importability(r) in IMPORTABLE
        ),
        "rejected": skipped_rejected,
        "namesChanged": changed,
        "namesAlreadyApplied": unchanged_name,
        "translations": len(translations),
    }
    return result, stats


def assert_only_name_changed(before: list[dict], after: list[dict]) -> None:
    if len(before) != len(after):
        raise AssertionError(f"Catalog length changed: {len(before)} -> {len(after)}")
    for old, new in zip(before, after):
        for field in IMMUTABLE_FIELDS:
            if old.get(field) != new.get(field):
                raise AssertionError(
                    f"Field {field} changed for {old.get('externalId')}"
                )
        if set(old.keys()) != set(new.keys()):
            raise AssertionError(f"Keys changed for {old.get('externalId')}")


def assert_no_pulldown_as_puxada(catalog: list[dict]) -> None:
    for row in catalog:
        name = row.get("name") or ""
        # Presentation names that still contain Pulldown must not say Puxada.
        if "pulldown" in name.lower() and "puxada" in name.lower():
            raise AssertionError(f"Pulldown translated as Puxada: {row.get('externalId')}")
        # Also forbid any importable name that replaced Pulldown with Puxada only
        eid = row.get("externalId") or ""
        if "Pulldown" in eid or "pulldown" in eid.lower():
            if "puxada" in name.lower() and "pulldown" not in name.lower():
                raise AssertionError(
                    f"Pulldown exercise lost Pulldown term: {eid} -> {name}"
                )


def write_catalog(path: Path, catalog: list[dict]) -> str:
    text = dumps_deterministic(catalog)
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(text.encode("utf-8"))
    return hashlib.sha256(text.encode("utf-8")).hexdigest()


def copy_to_asset(src: Path = NORMALIZED, dest: Path = ASSET) -> None:
    if FULL_876_CATALOG.exists() and src.resolve() == FULL_876_CATALOG.resolve():
        raise SystemExit(
            "After Stage 4.9 the Android asset is the curated V2.1 pack (136). "
            "Do not copy the full 876 archive to assets. "
            "Run: py -3 tools/exercise-import/curation/publish_main_catalog_v2_1.py"
        )
    if FULL_876_CATALOG.exists() and dest.resolve() == ASSET.resolve():
        # Published pack path may still be the curated JSON; refuse blind overwrite.
        published = json.loads(PUBLISHED_CATALOG.read_text(encoding="utf-8"))
        if len(published) != 876:
            raise SystemExit(
                "Android asset is curated (not 876). Use publish_main_catalog_v2_1.py "
                "to refresh the published pack after name changes on the full archive."
            )
    dest.parent.mkdir(parents=True, exist_ok=True)
    shutil.copy2(src, dest)


def main(argv: list[str] | None = None) -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--copy-asset",
        action="store_true",
        help="Copy output catalog to app/src/main/assets/exercises/",
    )
    parser.add_argument(
        "--catalog",
        type=Path,
        default=NORMALIZED,
        help="Path to gymtrack-exercises.json",
    )
    args = parser.parse_args(argv)

    before = json.loads(args.catalog.read_text(encoding="utf-8"))
    translations = load_translations()
    after, stats = apply_names(before, translations)
    assert_only_name_changed(before, after)
    assert_no_pulldown_as_puxada(after)

    sha = write_catalog(args.catalog, after)
    if args.copy_asset:
        copy_to_asset(args.catalog, ASSET)

    status_counts = translation_status_counts()
    summary = {
        "stats": stats,
        "statusCounts": status_counts,
        "sha256": sha,
        "assetCopied": bool(args.copy_asset),
    }
    print(json.dumps(summary, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
