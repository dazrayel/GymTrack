"""Tests for MAIN_CATALOG V2.1 and MEDIA_CANDIDATES V2.1."""

from __future__ import annotations

import hashlib
import json
import unittest
from collections import Counter
from pathlib import Path
from urllib.parse import urlparse

ROOT = Path(__file__).resolve().parents[1]
REPO = ROOT.parents[1]

import sys

sys.path.insert(0, str(ROOT / "analysis"))
sys.path.insert(0, str(ROOT / "curation"))

import curate_main_catalog_v2 as curate_v2  # noqa: E402
import curate_main_catalog_v2_1 as curate  # noqa: E402
import probe_media_candidates_v2_1 as media  # noqa: E402
from classify_importability import classify as classify_importability  # noqa: E402

from catalog_paths import analysis_catalog_path  # noqa: E402

NORMALIZED = analysis_catalog_path()
V1_JSON = ROOT / "analysis" / "MAIN_CATALOG.json"
V2_JSON = ROOT / "analysis" / "MAIN_CATALOG_V2.json"
V2_1_JSON = ROOT / "analysis" / "MAIN_CATALOG_V2_1.json"
V2_1_MD = ROOT / "analysis" / "MAIN_CATALOG_V2_1.md"
MEDIA_JSON = ROOT / "analysis" / "MEDIA_CANDIDATES_V2_1.json"
MEDIA_MD = ROOT / "analysis" / "MEDIA_CANDIDATES_V2_1.md"
SOURCE_IMAGES = ROOT / "input" / "free-exercise-db" / "exercises"
ASSETS = REPO / "app" / "src" / "main" / "assets" / "exercises"


class CurateMainCatalogV21Tests(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.catalog_bytes = NORMALIZED.read_bytes()
        cls.v1_bytes = V1_JSON.read_bytes()
        cls.v2_bytes = V2_JSON.read_bytes()
        cls.importable_ids = {
            r["externalId"]
            for r in json.loads(NORMALIZED.read_text(encoding="utf-8"))
            if classify_importability(r) in {"IMPORTABLE", "IMPORTABLE_WITH_FALLBACK"}
        }
        cls.rejected_ids = {
            r["externalId"]
            for r in json.loads(NORMALIZED.read_text(encoding="utf-8"))
            if classify_importability(r)
            not in {"IMPORTABLE", "IMPORTABLE_WITH_FALLBACK"}
        }
        cls.payload = curate.curate_v2_1()
        cls.v1_ids = {
            e["externalId"] for e in json.loads(cls.v1_bytes.decode("utf-8"))["selected"]
        }

    def test_selected_count_in_range(self) -> None:
        n = self.payload["selectedCount"]
        self.assertGreaterEqual(n, curate.TARGET_MIN)
        self.assertLessEqual(n, curate.TARGET_MAX)

    def test_no_duplicate_ids(self) -> None:
        ids = [e["externalId"] for e in self.payload["selected"]]
        self.assertEqual(len(ids), len(set(ids)))

    def test_subset_of_importable(self) -> None:
        for e in self.payload["selected"]:
            self.assertIn(e["externalId"], self.importable_ids)
            self.assertTrue(e.get("externalId"))
            self.assertTrue(e.get("externalSource") or e.get("source"))

    def test_no_rejected(self) -> None:
        for e in self.payload["selected"]:
            self.assertNotIn(e["externalId"], self.rejected_ids)

    def test_media_rule_local_or_pending(self) -> None:
        for e in self.payload["selected"]:
            local = e.get("sourceImage0") and e.get("sourceImage1")
            apk = e.get("apkImage0") and e.get("apkImage1")
            pending = e.get("pendingMediaAcquisition")
            self.assertTrue(
                local or apk or pending,
                msg=f"{e['externalId']} sem mídia (input/APK) e sem pending",
            )

    def test_distribution_targets(self) -> None:
        counts = Counter(e["muscleGroup"] for e in self.payload["selected"])
        self.assertGreaterEqual(counts["Costas"], 17)
        self.assertGreaterEqual(counts["Ombros"], 13)
        self.assertGreaterEqual(counts["Bíceps"], 8)
        self.assertLessEqual(counts["Abdômen"], 18)
        self.assertGreaterEqual(counts["Abdômen"], 15)
        self.assertGreaterEqual(counts["Lombar"], 5)
        self.assertLessEqual(counts["Lombar"], 7)
        self.assertGreaterEqual(counts["Antebraço"], 6)
        self.assertLessEqual(counts["Antebraço"], 7)
        self.assertGreaterEqual(counts["Trapézio"], 5)
        self.assertLessEqual(counts["Trapézio"], 6)
        for muscle, (lo, hi) in curate.MUSCLE_TARGETS.items():
            n = counts.get(muscle, 0)
            self.assertGreaterEqual(n, lo, msg=f"{muscle} < {lo}")
            self.assertLessEqual(n, hi, msg=f"{muscle} > {hi}")

    def test_v1_preferably_preserved(self) -> None:
        selected = {e["externalId"] for e in self.payload["selected"]}
        missing = self.v1_ids - selected
        self.assertEqual(
            set(),
            missing,
            msg=f"V1 exercises removed in V2.1: {sorted(missing)}",
        )
        self.assertEqual(0, self.payload["v1RemovedCount"])

    def test_deterministic(self) -> None:
        a = curate.dumps_deterministic(curate.curate_v2_1())
        b = curate.dumps_deterministic(curate.curate_v2_1())
        self.assertEqual(a, b)
        self.assertEqual(
            hashlib.sha256(a.encode()).hexdigest(),
            hashlib.sha256(b.encode()).hexdigest(),
        )

    def test_json_matches_curate(self) -> None:
        """Selection identity is stable; media flags may change after APK acquisition."""
        self.assertTrue(V2_1_JSON.exists())
        on_disk = json.loads(V2_1_JSON.read_text(encoding="utf-8"))
        fresh = curate.curate_v2_1()
        self.assertEqual(on_disk["selectedCount"], fresh["selectedCount"])
        self.assertEqual(
            [e["externalId"] for e in on_disk["selected"]],
            [e["externalId"] for e in fresh["selected"]],
        )
        self.assertEqual(
            {r["externalId"] for r in on_disk["removedFromV2"]},
            {r["externalId"] for r in fresh["removedFromV2"]},
        )

    def test_markdown_exists(self) -> None:
        self.assertTrue(V2_1_MD.exists())
        text = V2_1_MD.read_text(encoding="utf-8")
        self.assertIn("V2.1", text)
        self.assertIn("Abdômen", text)

    def test_v1_and_v2_untouched(self) -> None:
        self.assertEqual(self.v1_bytes, V1_JSON.read_bytes())
        self.assertEqual(self.v2_bytes, V2_JSON.read_bytes())
        v2 = json.loads(self.v2_bytes.decode("utf-8"))
        self.assertEqual(2, v2.get("version"))
        self.assertEqual(135, v2.get("selectedCount"))

    def test_normalized_unchanged(self) -> None:
        self.assertEqual(self.catalog_bytes, NORMALIZED.read_bytes())

    def test_apk_assets_cover_v21_after_acquisition(self) -> None:
        both = 0
        if ASSETS.is_dir():
            for d in ASSETS.iterdir():
                if d.is_dir() and (d / "0.jpg").is_file() and (d / "1.jpg").is_file():
                    both += 1
        # Media acquisition stage expands APK to the full V2.1 set.
        self.assertEqual(136, both)

    def test_v2_suite_still_loads(self) -> None:
        # Regression: V2 curation module still deterministic / countable.
        p = curate_v2.curate_v2()
        self.assertEqual(135, p["selectedCount"])


class MediaCandidatesV21Tests(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.payload = json.loads(MEDIA_JSON.read_text(encoding="utf-8"))

    def test_media_file_exists(self) -> None:
        self.assertTrue(MEDIA_JSON.exists())
        self.assertTrue(MEDIA_MD.exists())

    def test_not_copied_to_apk(self) -> None:
        self.assertFalse(self.payload["copiedToApk"])
        for c in self.payload["candidates"]:
            self.assertFalse(c.get("copiedToApk", False))
            self.assertFalse(c.get("downloadedLocally", False))

    def test_no_duplicate_candidates(self) -> None:
        ids = [c["externalId"] for c in self.payload["candidates"]]
        self.assertEqual(len(ids), len(set(ids)))

    def test_license_registered_when_found(self) -> None:
        for c in self.payload["candidates"]:
            if c["status"] == "FOUND":
                self.assertTrue(c.get("license"))
                self.assertTrue(c.get("licenseUrl"))
                self.assertTrue(c.get("sourceUrl0"))
                self.assertTrue(c.get("sourceUrl1"))
                for url in (c["sourceUrl0"], c["sourceUrl1"], c["licenseUrl"]):
                    parsed = urlparse(url)
                    self.assertIn(parsed.scheme, {"http", "https"})
                    self.assertTrue(parsed.netloc)

    def test_found_statuses_have_both_frames(self) -> None:
        for c in self.payload["candidates"]:
            if c["status"] == "FOUND":
                self.assertTrue(c["foundMedia"]["frame0"])
                self.assertTrue(c["foundMedia"]["frame1"])

    def test_priority_a_mostly_found(self) -> None:
        a = [c for c in self.payload["candidates"] if c["priority"] == "A"]
        self.assertGreaterEqual(len(a), 10)
        found = sum(1 for c in a if c["status"] in {"FOUND", "ALREADY_AVAILABLE"})
        self.assertGreaterEqual(found, len(a) - 2)

    def test_apk_has_full_v21_set(self) -> None:
        both = sum(
            1
            for d in ASSETS.iterdir()
            if d.is_dir() and (d / "0.jpg").is_file() and (d / "1.jpg").is_file()
        )
        self.assertEqual(136, both)


if __name__ == "__main__":
    unittest.main()
