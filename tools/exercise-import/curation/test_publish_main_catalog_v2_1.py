"""Tests for Stage 4.9 — publish MAIN_CATALOG_V2_1 as the GymTrack pack."""

from __future__ import annotations

import hashlib
import json
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
REPO = ROOT.parents[1]

import sys

sys.path.insert(0, str(ROOT / "curation"))

from catalog_paths import (  # noqa: E402
    FULL_876_CATALOG,
    PUBLISHED_CATALOG,
    analysis_catalog_path,
)
from publish_main_catalog_v2_1 import (  # noqa: E402
    EXPECTED,
    SOURCE,
    ASSET,
    ASSETS_DIR,
    V2_1_JSON,
    publish,
    sha256_bytes,
)

TRANSLATIONS = ROOT / "translate" / "translations.json"


class PublishMainCatalogV21Tests(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.result = publish()
        cls.v21 = json.loads(V2_1_JSON.read_text(encoding="utf-8"))
        cls.published = json.loads(PUBLISHED_CATALOG.read_text(encoding="utf-8"))
        cls.asset = json.loads(ASSET.read_text(encoding="utf-8"))
        cls.full = json.loads(FULL_876_CATALOG.read_text(encoding="utf-8"))
        cls.translations = {
            (t["source"], t["externalId"]): t["name"]
            for t in json.loads(TRANSLATIONS.read_text(encoding="utf-8"))["translations"]
        }

    def test_counts(self) -> None:
        self.assertEqual(EXPECTED, self.v21["selectedCount"])
        self.assertEqual(EXPECTED, len(self.v21["selected"]))
        self.assertEqual(EXPECTED, len(self.published))
        self.assertEqual(EXPECTED, len(self.asset))
        self.assertEqual(876, len(self.full))
        self.assertEqual(EXPECTED, self.result["count"])

    def test_ids_match_v2_1(self) -> None:
        selected = {
            (e.get("externalSource") or e.get("source") or SOURCE, e["externalId"])
            for e in self.v21["selected"]
        }
        published = {(r["source"], r["externalId"]) for r in self.published}
        self.assertEqual(selected, published)
        self.assertEqual(EXPECTED, len(published))

    def test_no_identity_duplicates(self) -> None:
        keys = [(r["source"], r["externalId"]) for r in self.published]
        self.assertTrue(all(s and eid for s, eid in keys))
        self.assertEqual(len(keys), len(set(keys)))

    def test_asset_matches_tools_output(self) -> None:
        tools_text = PUBLISHED_CATALOG.read_text(encoding="utf-8")
        asset_text = ASSET.read_text(encoding="utf-8")
        self.assertEqual(tools_text, asset_text)
        self.assertEqual(
            sha256_bytes(tools_text.encode("utf-8")),
            self.result["sha256"],
        )

    def test_deterministic_sha(self) -> None:
        first = sha256_bytes(PUBLISHED_CATALOG.read_bytes())
        again = publish()
        second = sha256_bytes(PUBLISHED_CATALOG.read_bytes())
        self.assertEqual(first, second)
        self.assertEqual(first, again["sha256"])

    def test_media_complete(self) -> None:
        missing = []
        for r in self.published:
            eid = r["externalId"]
            for frame in ("0.jpg", "1.jpg"):
                path = ASSETS_DIR / eid / frame
                if not path.is_file():
                    missing.append(f"{eid}/{frame}")
        self.assertEqual([], missing)
        self.assertEqual(EXPECTED, self.result["media"]["with0"])
        self.assertEqual(EXPECTED, self.result["media"]["with1"])
        self.assertEqual(EXPECTED * 2, self.result["media"]["jpgFilesExpected"])

    def test_names_match_translated_catalog(self) -> None:
        full_by_id = {(r["source"], r["externalId"]): r for r in self.full}
        for r in self.published:
            key = (r["source"], r["externalId"])
            self.assertEqual(full_by_id[key]["name"], r["name"])
            self.assertEqual(full_by_id[key]["sourceData"], r["sourceData"])
            if key in self.translations:
                self.assertEqual(self.translations[key], r["name"])
        pulldownish = [
            r for r in self.published
            if "Pulldown" in (r.get("name") or "") or "Pulldown" in r["externalId"]
        ]
        self.assertTrue(pulldownish, "expected at least one Pulldown* exercise in V2.1")
        for r in pulldownish:
            self.assertNotIn("Puxada", r["name"])
            # Stage 4.8 glossary: technical term stays Pulldown (never Puxada)
            if r["name"] == "Pulldown" or r["name"].endswith("Pulldown"):
                self.assertIn("Pulldown", r["name"])

    def test_analysis_catalog_path_prefers_full_archive(self) -> None:
        self.assertEqual(FULL_876_CATALOG.resolve(), analysis_catalog_path().resolve())


if __name__ == "__main__":
    unittest.main()
