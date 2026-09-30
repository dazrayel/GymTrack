"""Acquire + incorporate V2.1 pending media into app assets (no catalog/importer changes).

Steps:
1. Snapshot SHA-256 of existing assets under app/src/main/assets/exercises/
2. Download 0.jpg/1.jpg for pendingMediaAcquisition exercises from FEDB URLs
3. Validate JPEG, sizes, dimensions
4. Copy only into missing paths (never silent overwrite)
5. Write MEDIA_MANIFEST_V2_1 + audit report
"""

from __future__ import annotations

import hashlib
import io
import json
import struct
import sys
import urllib.error
import urllib.request
from datetime import datetime, timezone
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
REPO = ROOT.parents[1]

V2_1_JSON = ROOT / "analysis" / "MAIN_CATALOG_V2_1.json"
MEDIA_CANDIDATES = ROOT / "analysis" / "MEDIA_CANDIDATES_V2_1.json"
ASSETS = REPO / "app" / "src" / "main" / "assets" / "exercises"
DOWNLOAD_CACHE = ROOT / "analysis" / "_media_download_cache_v2_1"
PRE_HASHES = ROOT / "analysis" / "_assets_pre_hash_v2_1.json"
MANIFEST_JSON = ROOT / "analysis" / "MEDIA_MANIFEST_V2_1.json"
MANIFEST_MD = ROOT / "analysis" / "MEDIA_MANIFEST_V2_1.md"
AUDIT_JSON = ROOT / "analysis" / "MEDIA_AUDIT_V2_1.json"

FEDB_LICENSE = "Unlicense (public domain)"
FEDB_LICENSE_URL = "https://github.com/yuhonas/free-exercise-db/blob/main/LICENSE.md"
FEDB_REPO = "https://github.com/yuhonas/free-exercise-db"

MIN_BYTES = 512
USER_AGENT = "GymTrack-MediaAcquire/2.1 (+local-dev; Unlicense FEDB)"


def sha256_bytes(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def sha256_file(path: Path) -> str:
    h = hashlib.sha256()
    with path.open("rb") as f:
        for chunk in iter(lambda: f.read(65536), b""):
            h.update(chunk)
    return h.hexdigest()


def is_jpeg(data: bytes) -> bool:
    return len(data) >= 4 and data[:2] == b"\xff\xd8" and data[-2:] == b"\xff\xd9"


def jpeg_dimensions(data: bytes) -> tuple[int, int] | None:
    """Parse SOF0/SOF2 for width/height without Pillow."""
    if not data.startswith(b"\xff\xd8"):
        return None
    i = 2
    while i + 9 < len(data):
        if data[i] != 0xFF:
            i += 1
            continue
        marker = data[i + 1]
        if marker in (0xD8, 0xD9):
            i += 2
            continue
        if marker == 0x01 or 0xD0 <= marker <= 0xD7:
            i += 2
            continue
        if i + 4 > len(data):
            return None
        length = struct.unpack(">H", data[i + 2 : i + 4])[0]
        if marker in (0xC0, 0xC1, 0xC2):
            if i + 9 > len(data):
                return None
            height, width = struct.unpack(">HH", data[i + 5 : i + 9])
            return width, height
        i += 2 + length
    return None


def validate_jpeg(data: bytes, label: str) -> dict[str, Any]:
    issues: list[str] = []
    if len(data) < MIN_BYTES:
        issues.append(f"{label}: too small ({len(data)} bytes)")
    if data.lstrip().startswith((b"<!DOCTYPE", b"<html", b"{", b"[")):
        issues.append(f"{label}: looks like HTML/JSON, not JPEG")
    if not is_jpeg(data):
        issues.append(f"{label}: missing JPEG SOI/EOI markers")
    dims = jpeg_dimensions(data)
    if dims is None:
        issues.append(f"{label}: could not read JPEG dimensions")
    elif dims[0] < 32 or dims[1] < 32:
        issues.append(f"{label}: dimensions too small {dims}")
    return {
        "ok": not issues,
        "issues": issues,
        "bytes": len(data),
        "sha256": sha256_bytes(data),
        "dimensions": dims,
    }


def snapshot_existing_assets() -> dict[str, Any]:
    files: dict[str, dict[str, Any]] = {}
    total = 0
    if ASSETS.is_dir():
        for d in sorted(ASSETS.iterdir()):
            if not d.is_dir():
                continue
            for name in ("0.jpg", "1.jpg"):
                path = d / name
                if path.is_file():
                    data = path.read_bytes()
                    total += len(data)
                    rel = f"app/src/main/assets/exercises/{d.name}/{name}"
                    files[rel] = {
                        "sha256": sha256_bytes(data),
                        "bytes": len(data),
                    }
    payload = {
        "createdAt": datetime.now(timezone.utc).isoformat(),
        "fileCount": len(files),
        "totalBytes": total,
        "files": files,
    }
    PRE_HASHES.write_text(json.dumps(payload, indent=2) + "\n", encoding="utf-8")
    return payload


def download(url: str) -> bytes:
    req = urllib.request.Request(url, headers={"User-Agent": USER_AGENT})
    with urllib.request.urlopen(req, timeout=60) as resp:
        if getattr(resp, "status", 200) != 200:
            raise RuntimeError(f"HTTP {resp.status} for {url}")
        return resp.read()


def acquire() -> dict[str, Any]:
    v21 = json.loads(V2_1_JSON.read_text(encoding="utf-8"))
    candidates = {
        c["externalId"]: c
        for c in json.loads(MEDIA_CANDIDATES.read_text(encoding="utf-8"))["candidates"]
    }
    pending = [e for e in v21["selected"] if e.get("pendingMediaAcquisition")]
    if len(pending) != 21:
        raise AssertionError(f"Expected 21 pending, got {len(pending)}")

    pre = snapshot_existing_assets()
    DOWNLOAD_CACHE.mkdir(parents=True, exist_ok=True)
    ASSETS.mkdir(parents=True, exist_ok=True)

    results: list[dict[str, Any]] = []
    incorporated = 0
    review = 0
    failed = 0

    for ex in pending:
        eid = ex["externalId"]
        cand = candidates.get(eid)
        if cand is None:
            results.append(
                {
                    "externalId": eid,
                    "status": "REVIEW_MEDIA",
                    "reason": "missing from MEDIA_CANDIDATES_V2_1.json",
                }
            )
            review += 1
            continue
        if cand.get("status") != "FOUND":
            results.append(
                {
                    "externalId": eid,
                    "status": "REVIEW_MEDIA",
                    "reason": f"candidate status={cand.get('status')}",
                }
            )
            review += 1
            continue
        if "Unlicense" not in (cand.get("license") or ""):
            results.append(
                {
                    "externalId": eid,
                    "status": "REVIEW_MEDIA",
                    "reason": f"license not Unlicense: {cand.get('license')}",
                }
            )
            review += 1
            continue

        url0 = cand["sourceUrl0"]
        url1 = cand["sourceUrl1"]
        dest_dir = ASSETS / eid
        dest0 = dest_dir / "0.jpg"
        dest1 = dest_dir / "1.jpg"

        try:
            data0 = download(url0)
            data1 = download(url1)
        except Exception as e:
            results.append(
                {
                    "externalId": eid,
                    "status": "FAILED_DOWNLOAD",
                    "reason": str(e),
                    "url0": url0,
                    "url1": url1,
                }
            )
            failed += 1
            continue

        v0 = validate_jpeg(data0, "0.jpg")
        v1 = validate_jpeg(data1, "1.jpg")
        if not v0["ok"] or not v1["ok"]:
            results.append(
                {
                    "externalId": eid,
                    "status": "REVIEW_MEDIA",
                    "reason": "invalid jpeg",
                    "issues": v0["issues"] + v1["issues"],
                }
            )
            review += 1
            continue

        # Same-demo heuristic: both frames valid JPEG with similar size class
        ratio = max(v0["bytes"], v1["bytes"]) / max(1, min(v0["bytes"], v1["bytes"]))
        if ratio > 20:
            results.append(
                {
                    "externalId": eid,
                    "status": "REVIEW_MEDIA",
                    "reason": f"frame size mismatch ratio={ratio:.1f}",
                }
            )
            review += 1
            continue

        # Cache downloads
        cache_dir = DOWNLOAD_CACHE / eid
        cache_dir.mkdir(parents=True, exist_ok=True)
        (cache_dir / "0.jpg").write_bytes(data0)
        (cache_dir / "1.jpg").write_bytes(data1)

        conflicts: list[str] = []
        for dest, data, label in ((dest0, data0, "0.jpg"), (dest1, data1, "1.jpg")):
            if dest.is_file():
                existing = dest.read_bytes()
                if sha256_bytes(existing) != sha256_bytes(data):
                    conflicts.append(
                        f"{label} exists with different hash "
                        f"(existing={sha256_bytes(existing)[:12]} new={sha256_bytes(data)[:12]})"
                    )
                # identical hash → leave as-is
        if conflicts:
            results.append(
                {
                    "externalId": eid,
                    "status": "REVIEW_MEDIA",
                    "reason": "conflict with existing asset",
                    "conflicts": conflicts,
                }
            )
            review += 1
            continue

        dest_dir.mkdir(parents=True, exist_ok=True)
        wrote = []
        if not dest0.is_file():
            dest0.write_bytes(data0)
            wrote.append("0.jpg")
        if not dest1.is_file():
            dest1.write_bytes(data1)
            wrote.append("1.jpg")

        results.append(
            {
                "externalId": eid,
                "name": ex.get("translatedName") or ex.get("originalName") or eid,
                "status": "OK",
                "frame0": "OK",
                "frame1": "OK",
                "wrote": wrote,
                "alreadyPresent": [x for x in ("0.jpg", "1.jpg") if x not in wrote],
                "source": "free-exercise-db",
                "sourceUrl0": url0,
                "sourceUrl1": url1,
                "license": FEDB_LICENSE,
                "licenseUrl": FEDB_LICENSE_URL,
                "sha256_frame0": v0["sha256"],
                "sha256_frame1": v1["sha256"],
                "bytes0": v0["bytes"],
                "bytes1": v1["bytes"],
                "dimensions0": v0["dimensions"],
                "dimensions1": v1["dimensions"],
            }
        )
        incorporated += 1

    # Verify pre hashes unchanged for previously existing files
    altered = []
    for rel, meta in pre["files"].items():
        path = REPO / rel.replace("/", "\\") if sys.platform == "win32" else REPO / rel
        # normalize
        path = REPO.joinpath(*rel.split("/"))
        if not path.is_file():
            altered.append({"path": rel, "issue": "missing_after"})
            continue
        now = sha256_file(path)
        if now != meta["sha256"]:
            altered.append({"path": rel, "issue": "hash_changed", "before": meta["sha256"], "after": now})

    return {
        "pendingRequested": 21,
        "incorporated": incorporated,
        "review": review,
        "failed": failed,
        "results": results,
        "preExistingFiles": pre["fileCount"],
        "preExistingBytes": pre["totalBytes"],
        "alteredExisting": altered,
        "license": FEDB_LICENSE,
        "repo": FEDB_REPO,
    }


def build_manifest(acquire_report: dict[str, Any]) -> dict[str, Any]:
    v21 = json.loads(V2_1_JSON.read_text(encoding="utf-8"))
    acquired_by = {
        r["externalId"]: r for r in acquire_report["results"] if r.get("status") == "OK"
    }
    entries = []
    missing0 = missing1 = invalid = 0
    total_bytes = 0
    for ex in v21["selected"]:
        eid = ex["externalId"]
        p0 = ASSETS / eid / "0.jpg"
        p1 = ASSETS / eid / "1.jpg"
        f0 = p0.is_file()
        f1 = p1.is_file()
        if not f0:
            missing0 += 1
        if not f1:
            missing1 += 1
        sha0 = sha1 = None
        bytes0 = bytes1 = 0
        ok0 = ok1 = False
        dims0 = dims1 = None
        if f0:
            d0 = p0.read_bytes()
            bytes0 = len(d0)
            total_bytes += bytes0
            v = validate_jpeg(d0, "0.jpg")
            ok0 = v["ok"]
            sha0 = v["sha256"]
            dims0 = v["dimensions"]
            if not ok0:
                invalid += 1
        if f1:
            d1 = p1.read_bytes()
            bytes1 = len(d1)
            total_bytes += bytes1
            v = validate_jpeg(d1, "1.jpg")
            ok1 = v["ok"]
            sha1 = v["sha256"]
            dims1 = v["dimensions"]
            if not ok1:
                invalid += 1

        acquired = acquired_by.get(eid)
        if f0 and f1 and ok0 and ok1:
            status = "OK"
        elif not f0 or not f1:
            status = "MISSING"
        else:
            status = "INVALID"

        source = "apk_preexisting"
        license_ = FEDB_LICENSE
        if acquired:
            source = "free-exercise-db_download_v2_1"
        elif ex.get("mediaStatus") == "source_only":
            source = "copied_earlier_from_local_input"
        elif ex.get("mediaStatus") == "apk":
            source = "apk_preexisting_v1"

        entries.append(
            {
                "externalSource": ex.get("externalSource") or ex.get("source") or "free-exercise-db",
                "externalId": eid,
                "name": ex.get("translatedName") or ex.get("originalName") or eid,
                "muscleGroup": ex.get("muscleGroup"),
                "frame0": f0,
                "frame1": f1,
                "sha256_frame0": sha0,
                "sha256_frame1": sha1,
                "bytes0": bytes0,
                "bytes1": bytes1,
                "dimensions0": dims0,
                "dimensions1": dims1,
                "source": source,
                "license": license_,
                "licenseUrl": FEDB_LICENSE_URL,
                "status": status,
                "assetPath0": f"app/src/main/assets/exercises/{eid}/0.jpg" if f0 else None,
                "assetPath1": f"app/src/main/assets/exercises/{eid}/1.jpg" if f1 else None,
            }
        )

    new_bytes = sum(
        (r.get("bytes0") or 0) + (r.get("bytes1") or 0)
        for r in acquire_report["results"]
        if r.get("status") == "OK"
    )

    # Dedup folders
    dirs = [d.name for d in ASSETS.iterdir() if d.is_dir()] if ASSETS.is_dir() else []
    dup_dirs = len(dirs) - len(set(dirs))

    audit = {
        "selectedExercises": len(v21["selected"]),
        "withBothFrames": sum(1 for e in entries if e["frame0"] and e["frame1"]),
        "missingFrame0": missing0,
        "missingFrame1": missing1,
        "invalidJpeg": invalid,
        "duplicates": dup_dirs,
        "statusOk": sum(1 for e in entries if e["status"] == "OK"),
        "alteredExistingCount": len(acquire_report["alteredExisting"]),
        "sizeBeforeBytes": acquire_report["preExistingBytes"],
        "sizeNewBytes": new_bytes,
        "sizeAfterBytes": total_bytes,
        "sizeBeforeMiB": round(acquire_report["preExistingBytes"] / (1024 * 1024), 2),
        "sizeNewMiB": round(new_bytes / (1024 * 1024), 2),
        "sizeAfterMiB": round(total_bytes / (1024 * 1024), 2),
    }

    return {
        "version": "2.1",
        "createdAt": datetime.now(timezone.utc).isoformat(),
        "selectedCount": len(entries),
        "license": FEDB_LICENSE,
        "repo": FEDB_REPO,
        "acquisition": {
            "requested": acquire_report["pendingRequested"],
            "incorporated": acquire_report["incorporated"],
            "review": acquire_report["review"],
            "failed": acquire_report["failed"],
            "results": acquire_report["results"],
        },
        "audit": audit,
        "exercises": entries,
        "alteredExisting": acquire_report["alteredExisting"],
    }


def write_manifest_md(manifest: dict[str, Any]) -> str:
    a = manifest["audit"]
    acq = manifest["acquisition"]
    lines = [
        "# MEDIA_MANIFEST V2.1",
        "",
        f"- Selecionados V2.1: **{manifest['selectedCount']}**",
        f"- Com 0.jpg+1.jpg OK: **{a['statusOk']}**",
        f"- Missing 0: **{a['missingFrame0']}** | Missing 1: **{a['missingFrame1']}**",
        f"- JPEG inválidos: **{a['invalidJpeg']}** | Duplicatas: **{a['duplicates']}**",
        f"- Alterações nas mídias antigas: **{a['alteredExistingCount']}**",
        "",
        "## Tamanho",
        "",
        f"- Antes: **{a['sizeBeforeMiB']} MiB** ({a['sizeBeforeBytes']} bytes)",
        f"- Novas: **{a['sizeNewMiB']} MiB** ({a['sizeNewBytes']} bytes)",
        f"- Depois: **{a['sizeAfterMiB']} MiB** ({a['sizeAfterBytes']} bytes)",
        "",
        "## Aquisição",
        "",
        f"- Solicitadas: {acq['requested']}",
        f"- Incorporadas: {acq['incorporated']}",
        f"- REVIEW: {acq['review']}",
        f"- Falhas: {acq['failed']}",
        "",
        f"Origem: `{manifest['repo']}` — Licença: **{manifest['license']}**",
        "",
        "## Exercícios",
        "",
        "| externalId | frames | status | source |",
        "| ---------- | ------ | ------ | ------ |",
    ]
    for e in manifest["exercises"]:
        frames = ("OK" if e["frame0"] else "X") + "/" + ("OK" if e["frame1"] else "X")
        lines.append(
            f"| `{e['externalId']}` | {frames} | {e['status']} | {e['source']} |"
        )
    lines.append("")
    return "\n".join(lines)


def main() -> None:
    report = acquire()
    manifest = build_manifest(report)
    MANIFEST_JSON.write_text(json.dumps(manifest, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    MANIFEST_MD.write_text(write_manifest_md(manifest), encoding="utf-8")
    AUDIT_JSON.write_text(
        json.dumps(
            {
                "audit": manifest["audit"],
                "acquisition": {
                    k: manifest["acquisition"][k]
                    for k in ("requested", "incorporated", "review", "failed")
                },
                "alteredExisting": manifest["alteredExisting"],
            },
            indent=2,
        )
        + "\n",
        encoding="utf-8",
    )
    a = manifest["audit"]
    print("=== ACQUISITION ===")
    print(
        f"requested={report['pendingRequested']} incorporated={report['incorporated']} "
        f"review={report['review']} failed={report['failed']}"
    )
    print("=== AUDIT ===")
    print(
        f"Selected={a['selectedExercises']} both={a['withBothFrames']} "
        f"missing0={a['missingFrame0']} missing1={a['missingFrame1']} "
        f"invalid={a['invalidJpeg']} dup={a['duplicates']} altered={a['alteredExistingCount']}"
    )
    print(
        f"size MiB before/new/after = {a['sizeBeforeMiB']}/{a['sizeNewMiB']}/{a['sizeAfterMiB']}"
    )
    print(f"wrote {MANIFEST_JSON}")
    print(f"wrote {MANIFEST_MD}")
    if report["review"] or report["failed"] or a["missingFrame0"] or a["missingFrame1"]:
        sys.exit(1)


if __name__ == "__main__":
    main()
