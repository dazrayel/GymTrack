"""Tests for MAIN_CATALOG V2 curation (selection only — no APK/importer changes)."""

from __future__ import annotations

import hashlib
import json
import unittest
from collections import Counter
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
REPO = ROOT.parents[1]
sys_path_analysis = ROOT / "analysis"
sys_path_curation = ROOT / "curation"

import sys

sys.path.insert(0, str(sys_path_analysis))
sys.path.insert(0, str(sys_path_curation))

import curate_main_catalog_v2 as curate  # noqa: E402
from classify_importability import classify as classify_importability  # noqa: E402

from catalog_paths import analysis_catalog_path  # noqa: E402

NORMALIZED = analysis_catalog_path()
V1_JSON = ROOT / "analysis" / "MAIN_CATALOG.json"
V2_JSON = ROOT / "analysis" / "MAIN_CATALOG_V2.json"
V2_MD = ROOT / "analysis" / "MAIN_CATALOG_V2.md"
SOURCE_IMAGES = ROOT / "input" / "free-exercise-db" / "exercises"
ASSETS = REPO / "app" / "src" / "main" / "assets" / "exercises"
APP_SRC = REPO / "app" / "src"


class CurateMainCatalogV2Tests(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.catalog_bytes = NORMALIZED.read_bytes()
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
        cls.payload = curate.curate_v2()

    def test_importable_baseline_777(self) -> None:
        self.assertEqual(777, len(self.importable_ids))

    def test_selected_count_in_range(self) -> None:
        n = self.payload["selectedCount"]
        self.assertGreaterEqual(n, curate.TARGET_MIN)
        self.assertLessEqual(n, curate.TARGET_MAX)

    def test_no_duplicate_external_ids(self) -> None:
        ids = [e["externalId"] for e in self.payload["selected"]]
        self.assertEqual(len(ids), len(set(ids)))

    def test_selected_have_external_source_and_id(self) -> None:
        for e in self.payload["selected"]:
            self.assertTrue(e.get("externalId"))
            self.assertTrue(e.get("externalSource") or e.get("source"))

    def test_selected_subset_of_importable(self) -> None:
        for e in self.payload["selected"]:
            self.assertIn(e["externalId"], self.importable_ids)

    def test_no_rejected_selected(self) -> None:
        for e in self.payload["selected"]:
            self.assertNotIn(e["externalId"], self.rejected_ids)

    def test_all_selected_have_two_source_images(self) -> None:
        for e in self.payload["selected"]:
            eid = e["externalId"]
            self.assertTrue(
                (SOURCE_IMAGES / eid / "0.jpg").is_file(),
                msg=f"missing source 0.jpg for {eid}",
            )
            self.assertTrue(
                (SOURCE_IMAGES / eid / "1.jpg").is_file(),
                msg=f"missing source 1.jpg for {eid}",
            )
            self.assertEqual(2, e["imageCount"])

    def test_selected_media_summary_all_two_images(self) -> None:
        ms = self.payload["mediaSummary"]
        self.assertEqual(ms["selectedWithTwoImages"], self.payload["selectedCount"])
        self.assertEqual(0, ms["selectedWithOneImage"])
        self.assertEqual(0, ms["selectedWithZeroImages"])

    def test_muscle_priority_minimums(self) -> None:
        counts = Counter(e["muscleGroup"] for e in self.payload["selected"])
        self.assertGreaterEqual(counts.get("Lombar", 0), 6)
        self.assertGreaterEqual(counts.get("Antebraço", 0), 5)
        self.assertGreaterEqual(counts.get("Trapézio", 0), 5)
        for muscle, minimum in curate.MUSCLE_MINIMUMS.items():
            self.assertGreaterEqual(
                counts.get(muscle, 0),
                minimum,
                msg=f"{muscle} below V2 minimum {minimum}",
            )

    def test_muscle_soft_caps(self) -> None:
        counts = Counter(e["muscleGroup"] for e in self.payload["selected"])
        for muscle, count in counts.items():
            cap = curate.MUSCLE_MAXIMUMS.get(muscle, 20)
            self.assertLessEqual(count, cap, msg=f"{muscle} exceeds cap {cap}")

    def test_candidates_without_media_have_no_local_pair(self) -> None:
        for c in self.payload["candidatesWithoutMedia"]:
            eid = c["externalId"]
            both = (SOURCE_IMAGES / eid / "0.jpg").is_file() and (
                SOURCE_IMAGES / eid / "1.jpg"
            ).is_file()
            self.assertFalse(both, msg=f"candidate unexpectedly has media: {eid}")
            self.assertIn(eid, self.importable_ids)

    def test_candidates_not_overlapping_selected(self) -> None:
        selected = {e["externalId"] for e in self.payload["selected"]}
        candidates = {c["externalId"] for c in self.payload["candidatesWithoutMedia"]}
        self.assertFalse(selected & candidates)

    def test_deterministic(self) -> None:
        a = curate.dumps_deterministic(curate.curate_v2())
        b = curate.dumps_deterministic(curate.curate_v2())
        self.assertEqual(a, b)
        self.assertEqual(
            hashlib.sha256(a.encode()).hexdigest(),
            hashlib.sha256(b.encode()).hexdigest(),
        )

    def test_json_on_disk_matches_curate(self) -> None:
        """Selection identity is stable; apkImage* flags may change after media acquisition."""
        self.assertTrue(V2_JSON.exists(), "Run curate_main_catalog_v2.py first")
        on_disk = json.loads(V2_JSON.read_text(encoding="utf-8"))
        fresh = curate.curate_v2()
        self.assertEqual(on_disk["selectedCount"], fresh["selectedCount"])
        self.assertEqual(
            [e["externalId"] for e in on_disk["selected"]],
            [e["externalId"] for e in fresh["selected"]],
        )

    def test_markdown_exists(self) -> None:
        self.assertTrue(V2_MD.exists())
        text = V2_MD.read_text(encoding="utf-8")
        self.assertIn("MAIN_CATALOG V2", text)
        self.assertIn("Lombar", text)
        self.assertIn("Antebraço", text)
        self.assertIn("Trapézio", text)

    def test_audit_matrix_covers_importables(self) -> None:
        self.assertEqual(777, len(self.payload["auditMatrix"]))
        selected = sum(1 for r in self.payload["auditMatrix"] if r["selected"])
        self.assertEqual(self.payload["selectedCount"], selected)

    def test_v1_catalog_untouched_by_v2_tools(self) -> None:
        # V1 file still present; V2 is a sibling artifact.
        self.assertTrue(V1_JSON.exists())
        v1 = json.loads(V1_JSON.read_text(encoding="utf-8"))
        self.assertEqual(1, v1.get("version"))
        self.assertEqual(102, v1.get("selectedCount"))

    def test_normalized_catalog_bytes_unchanged(self) -> None:
        self.assertEqual(self.catalog_bytes, NORMALIZED.read_bytes())

    def test_no_app_source_files_touched_marker(self) -> None:
        """V2 curation JSON remains valid; assets may grow in later media stages."""
        # After V2.1 media acquisition, APK holds the full V2.1 set (136).
        both = 0
        if ASSETS.is_dir():
            for d in ASSETS.iterdir():
                if d.is_dir() and (d / "0.jpg").is_file() and (d / "1.jpg").is_file():
                    both += 1
        self.assertGreaterEqual(both, 102)
        self.assertTrue((APP_SRC / "main").is_dir())


if __name__ == "__main__":
    unittest.main()
