"""Tests for Etapa 4.5 main catalog curation and image assets."""

from __future__ import annotations

import hashlib
import json
import sys
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
REPO = ROOT.parents[1]
sys.path.insert(0, str(ROOT / "analysis"))
sys.path.insert(0, str(ROOT / "curation"))

import curate_main_catalog as curate  # noqa: E402
from classify_importability import classify as classify_importability  # noqa: E402

from catalog_paths import analysis_catalog_path  # noqa: E402

NORMALIZED = analysis_catalog_path()
MAIN_JSON = ROOT / "analysis" / "MAIN_CATALOG.json"
IMAGE_AUDIT = ROOT / "analysis" / "exercise-image-audit-4-5.json"
ASSETS = REPO / "app" / "src" / "main" / "assets" / "exercises"


class CurateMainCatalogTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.catalog_bytes = NORMALIZED.read_bytes()
        cls.importable_ids = cls._importable_external_ids()

    @staticmethod
    def _importable_external_ids() -> set[str]:
        catalog = json.loads(NORMALIZED.read_text(encoding="utf-8"))
        return {
            r["externalId"]
            for r in catalog
            if classify_importability(r) in {"IMPORTABLE", "IMPORTABLE_WITH_FALLBACK"}
        }

    def test_importable_baseline_777(self) -> None:
        self.assertEqual(777, len(self.importable_ids))

    def test_curate_no_duplicate_external_ids(self) -> None:
        payload = curate.curate()
        ids = [e["externalId"] for e in payload["selected"]]
        self.assertEqual(len(ids), len(set(ids)))

    def test_curate_subset_of_importable(self) -> None:
        payload = curate.curate()
        for e in payload["selected"]:
            self.assertIn(e["externalId"], self.importable_ids)

    def test_curate_count_in_target_range(self) -> None:
        payload = curate.curate()
        n = payload["selectedCount"]
        self.assertGreaterEqual(n, curate.TARGET_MIN)
        self.assertLessEqual(n, curate.TARGET_MAX)

    def test_muscle_minimums_met(self) -> None:
        payload = curate.curate()
        from collections import Counter

        counts = Counter(e["muscleGroup"] for e in payload["selected"])
        for muscle, minimum in curate.MUSCLE_MINIMUMS.items():
            self.assertGreaterEqual(
                counts.get(muscle, 0),
                minimum,
                msg=f"{muscle} below minimum {minimum}",
            )

    def test_muscle_soft_caps_respected(self) -> None:
        payload = curate.curate()
        from collections import Counter

        counts = Counter(e["muscleGroup"] for e in payload["selected"])
        for muscle, count in counts.items():
            cap = curate.MUSCLE_MAXIMUMS.get(muscle, curate.DEFAULT_MUSCLE_MAX)
            self.assertLessEqual(count, cap, msg=f"{muscle} exceeds cap {cap}")

    def test_abdomen_not_dominates_selection(self) -> None:
        payload = curate.curate()
        from collections import Counter

        counts = Counter(e["muscleGroup"] for e in payload["selected"])
        abs_count = counts.get("Abdômen", 0)
        self.assertLessEqual(abs_count, curate.MUSCLE_MAXIMUMS["Abdômen"] + 5)
        self.assertLess(abs_count, payload["selectedCount"] // 3)

    def test_curate_deterministic_payload(self) -> None:
        a = curate.dumps_deterministic(curate.curate())
        b = curate.dumps_deterministic(curate.curate())
        self.assertEqual(a, b)
        self.assertEqual(
            hashlib.sha256(a.encode()).hexdigest(),
            hashlib.sha256(b.encode()).hexdigest(),
        )

    def test_main_catalog_json_matches_curate(self) -> None:
        self.assertTrue(MAIN_JSON.exists(), "Run curate_main_catalog.py first")
        on_disk = MAIN_JSON.read_text(encoding="utf-8")
        fresh = curate.dumps_deterministic(curate.curate())
        self.assertEqual(on_disk, fresh)

    def test_audit_matrix_complete(self) -> None:
        payload = json.loads(MAIN_JSON.read_text(encoding="utf-8"))
        self.assertEqual(777, len(payload["auditMatrix"]))
        selected = sum(1 for r in payload["auditMatrix"] if r["selected"])
        self.assertEqual(payload["selectedCount"], selected)

    def test_image_assets_when_audit_present(self) -> None:
        if not IMAGE_AUDIT.exists():
            self.skipTest("Run audit_selected_images.py first")
        audit = json.loads(IMAGE_AUDIT.read_text(encoding="utf-8"))
        catalog = json.loads(MAIN_JSON.read_text(encoding="utf-8"))
        self.assertEqual(catalog["selectedCount"], len(audit["exercises"]))
        for row in audit["exercises"]:
            eid = row["externalId"]
            for rel in row.get("assetPaths") or []:
                path = REPO / rel.replace("/", "\\") if sys.platform == "win32" else REPO / rel
                self.assertTrue(path.is_file(), msg=str(path))
            expected = len(row.get("files") or [])
            if expected:
                dest_dir = ASSETS / eid
                self.assertTrue(dest_dir.is_dir(), msg=eid)
                jpgs = list(dest_dir.glob("*.jpg"))
                self.assertEqual(len(jpgs), min(2, expected), msg=eid)

    def test_gymtrack_catalog_bytes_unchanged_by_curation_tools(self) -> None:
        self.assertEqual(self.catalog_bytes, NORMALIZED.read_bytes())


if __name__ == "__main__":
    unittest.main()
