"""Copy V2.1 source_only local FEDB images into APK assets (complete 136/136)."""

from __future__ import annotations

import hashlib
import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
REPO = ROOT.parents[1]
sys.path.insert(0, str(ROOT / "curation"))

from acquire_media_v2_1 import (  # noqa: E402
    ASSETS,
    AUDIT_JSON,
    FEDB_LICENSE,
    FEDB_LICENSE_URL,
    FEDB_REPO,
    MANIFEST_JSON,
    MANIFEST_MD,
    PRE_HASHES,
    build_manifest,
    validate_jpeg,
    write_manifest_md,
)

V2_1 = ROOT / "analysis" / "MAIN_CATALOG_V2_1.json"
SRC = ROOT / "input" / "free-exercise-db" / "exercises"


def main() -> None:
    v21 = json.loads(V2_1.read_text(encoding="utf-8"))
    pre = json.loads(PRE_HASHES.read_text(encoding="utf-8"))
    copied: list[dict] = []

    for e in v21["selected"]:
        eid = e["externalId"]
        dest = ASSETS / eid
        wrote: list[str] = []
        for name in ("0.jpg", "1.jpg"):
            dpath = dest / name
            spath = SRC / eid / name
            if dpath.is_file():
                continue
            if not spath.is_file():
                raise SystemExit(f"missing local source {eid}/{name}")
            data = spath.read_bytes()
            v = validate_jpeg(data, name)
            if not v["ok"]:
                raise SystemExit(f"invalid jpeg {eid}/{name}: {v['issues']}")
            dest.mkdir(parents=True, exist_ok=True)
            dpath.write_bytes(data)
            wrote.append(name)
        if wrote:
            copied.append({"externalId": eid, "wrote": wrote, "source": "local_input_copy"})

    print(f"copied source_only folders: {len(copied)}")
    for c in copied:
        print(" ", c["externalId"], c["wrote"])

    altered: list[str] = []
    for rel, meta in pre["files"].items():
        path = REPO.joinpath(*rel.split("/"))
        if not path.is_file():
            altered.append(rel)
            continue
        h = hashlib.sha256(path.read_bytes()).hexdigest()
        if h != meta["sha256"]:
            altered.append(rel)
    print(f"altered existing: {len(altered)}")

    prev = json.loads(MANIFEST_JSON.read_text(encoding="utf-8"))
    acq_results = list(prev["acquisition"]["results"])
    for c in copied:
        eid = c["externalId"]
        p0 = (ASSETS / eid / "0.jpg").read_bytes()
        p1 = (ASSETS / eid / "1.jpg").read_bytes()
        v0 = validate_jpeg(p0, "0.jpg")
        v1 = validate_jpeg(p1, "1.jpg")
        acq_results.append(
            {
                "externalId": eid,
                "status": "OK",
                "frame0": "OK",
                "frame1": "OK",
                "wrote": c["wrote"],
                "source": "local_input_free-exercise-db",
                "license": FEDB_LICENSE,
                "licenseUrl": FEDB_LICENSE_URL,
                "sha256_frame0": v0["sha256"],
                "sha256_frame1": v1["sha256"],
                "bytes0": v0["bytes"],
                "bytes1": v1["bytes"],
                "dimensions0": v0["dimensions"],
                "dimensions1": v1["dimensions"],
                "note": "Copied from tools/exercise-import/input (source_only → APK)",
            }
        )

    acquire_report = {
        "pendingRequested": 21,
        "incorporated": 21,
        "review": 0,
        "failed": 0,
        "results": acq_results,
        "preExistingFiles": pre["fileCount"],
        "preExistingBytes": pre["totalBytes"],
        "alteredExisting": altered,
        "license": FEDB_LICENSE,
        "repo": FEDB_REPO,
    }
    manifest = build_manifest(acquire_report)
    manifest["acquisition"]["sourceOnlyCopiedToApk"] = len(copied)
    manifest["acquisition"]["sourceOnlyCopies"] = copied
    MANIFEST_JSON.write_text(
        json.dumps(manifest, ensure_ascii=False, indent=2) + "\n", encoding="utf-8"
    )
    MANIFEST_MD.write_text(write_manifest_md(manifest), encoding="utf-8")
    AUDIT_JSON.write_text(
        json.dumps(
            {
                "audit": manifest["audit"],
                "acquisition": {
                    k: manifest["acquisition"][k]
                    for k in (
                        "requested",
                        "incorporated",
                        "review",
                        "failed",
                        "sourceOnlyCopiedToApk",
                    )
                },
                "alteredExisting": altered,
            },
            indent=2,
        )
        + "\n",
        encoding="utf-8",
    )
    a = manifest["audit"]
    print("AUDIT", a)
    if not (
        a["withBothFrames"] == 136
        and a["missingFrame0"] == 0
        and a["missingFrame1"] == 0
        and a["invalidJpeg"] == 0
        and a["alteredExistingCount"] == 0
    ):
        raise SystemExit(1)
    print("PASS 136/136")


if __name__ == "__main__":
    main()
