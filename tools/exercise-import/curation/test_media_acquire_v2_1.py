"""Tests for V2.1 media acquisition / APK asset integrity."""

from __future__ import annotations

import json
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
REPO = ROOT.parents[1]

import sys

sys.path.insert(0, str(ROOT / "curation"))

from acquire_media_v2_1 import validate_jpeg  # noqa: E402
from audit_media_v2_1 import audit  # noqa: E402

V2_1 = ROOT / "analysis" / "MAIN_CATALOG_V2_1.json"
MANIFEST = ROOT / "analysis" / "MEDIA_MANIFEST_V2_1.json"
PRE_HASHES = ROOT / "analysis" / "_assets_pre_hash_v2_1.json"
ASSETS = REPO / "app" / "src" / "main" / "assets" / "exercises"
# Ensure V1/V2 catalog JSON untouched markers
V1 = ROOT / "analysis" / "MAIN_CATALOG.json"
V2 = ROOT / "analysis" / "MAIN_CATALOG_V2.json"
from catalog_paths import analysis_catalog_path, PUBLISHED_CATALOG  # noqa: E402

NORMALIZED = analysis_catalog_path()
PUBLISHED = PUBLISHED_CATALOG


class MediaAcquireV21Tests(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.v21 = json.loads(V2_1.read_text(encoding="utf-8"))
        cls.manifest = json.loads(MANIFEST.read_text(encoding="utf-8"))
        cls.v1_bytes = V1.read_bytes()
        cls.v2_bytes = V2.read_bytes()
        cls.norm_bytes = NORMALIZED.read_bytes()

    def test_audit_136_complete(self) -> None:
        result = audit()
        self.assertEqual(136, result["Selected exercises"])
        self.assertEqual(136, result["With 0.jpg + 1.jpg"])
        self.assertEqual(0, result["Missing frame 0"])
        self.assertEqual(0, result["Missing frame 1"])
        self.assertEqual(0, result["Invalid JPEG"])
        self.assertEqual(0, result["Duplicates"])
        self.assertTrue(result["ok"])

    def test_manifest_all_ok(self) -> None:
        self.assertEqual(136, self.manifest["selectedCount"])
        self.assertEqual(136, self.manifest["audit"]["statusOk"])
        self.assertEqual(0, self.manifest["audit"]["alteredExistingCount"])
        self.assertEqual(21, self.manifest["acquisition"]["incorporated"])
        self.assertEqual(0, self.manifest["acquisition"]["review"])

    def test_every_selected_has_valid_jpeg_pair(self) -> None:
        for ex in self.v21["selected"]:
            eid = ex["externalId"]
            for name in ("0.jpg", "1.jpg"):
                path = ASSETS / eid / name
                self.assertTrue(path.is_file(), msg=f"missing {eid}/{name}")
                data = path.read_bytes()
                v = validate_jpeg(data, name)
                self.assertTrue(v["ok"], msg=f"{eid}/{name}: {v['issues']}")

    def test_preexisting_hashes_unchanged(self) -> None:
        self.assertTrue(PRE_HASHES.exists())
        pre = json.loads(PRE_HASHES.read_text(encoding="utf-8"))
        for rel, meta in pre["files"].items():
            path = REPO.joinpath(*rel.split("/"))
            self.assertTrue(path.is_file(), msg=rel)
            import hashlib

            now = hashlib.sha256(path.read_bytes()).hexdigest()
            self.assertEqual(meta["sha256"], now, msg=f"altered {rel}")

    def test_catalogs_untouched(self) -> None:
        self.assertEqual(self.v1_bytes, V1.read_bytes())
        self.assertEqual(self.v2_bytes, V2.read_bytes())
        self.assertEqual(self.norm_bytes, NORMALIZED.read_bytes())
        # V2.1 JSON itself should remain (we did not rewrite selection)
        self.assertEqual(136, self.v21["selectedCount"])
        self.assertEqual("2.1", str(self.v21["version"]))


if __name__ == "__main__":
    unittest.main()
