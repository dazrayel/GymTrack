"""Audit V2.1 media integrity under app/src/main/assets/exercises/."""

from __future__ import annotations

import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
REPO = ROOT.parents[1]
V2_1_JSON = ROOT / "analysis" / "MAIN_CATALOG_V2_1.json"
ASSETS = REPO / "app" / "src" / "main" / "assets" / "exercises"
MANIFEST = ROOT / "analysis" / "MEDIA_MANIFEST_V2_1.json"

# Reuse validators
sys.path.insert(0, str(ROOT / "curation"))
from acquire_media_v2_1 import validate_jpeg  # noqa: E402


def audit() -> dict:
    v21 = json.loads(V2_1_JSON.read_text(encoding="utf-8"))
    missing0 = missing1 = invalid = 0
    both = 0
    for ex in v21["selected"]:
        eid = ex["externalId"]
        p0 = ASSETS / eid / "0.jpg"
        p1 = ASSETS / eid / "1.jpg"
        if not p0.is_file():
            missing0 += 1
        else:
            if not validate_jpeg(p0.read_bytes(), "0.jpg")["ok"]:
                invalid += 1
        if not p1.is_file():
            missing1 += 1
        else:
            if not validate_jpeg(p1.read_bytes(), "1.jpg")["ok"]:
                invalid += 1
        if p0.is_file() and p1.is_file():
            both += 1
    dirs = [d.name for d in ASSETS.iterdir() if d.is_dir()] if ASSETS.is_dir() else []
    return {
        "Selected exercises": len(v21["selected"]),
        "With 0.jpg + 1.jpg": both,
        "Missing frame 0": missing0,
        "Missing frame 1": missing1,
        "Invalid JPEG": invalid,
        "Duplicates": len(dirs) - len(set(dirs)),
        "ok": both == len(v21["selected"])
        and missing0 == 0
        and missing1 == 0
        and invalid == 0,
    }


def main() -> None:
    result = audit()
    for k, v in result.items():
        if k != "ok":
            print(f"{k}: {v}")
    if MANIFEST.exists():
        m = json.loads(MANIFEST.read_text(encoding="utf-8"))
        print(f"Manifest statusOk: {m['audit']['statusOk']}")
        print(f"Altered existing: {m['audit']['alteredExistingCount']}")
    if not result["ok"]:
        sys.exit(1)
    print("AUDIT PASS")


if __name__ == "__main__":
    main()
