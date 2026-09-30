"""Tests for Etapa 4.8 — apply pt-BR names onto the normalized catalog."""

from __future__ import annotations

import copy
import hashlib
import json
import sys
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "translate"))
sys.path.insert(0, str(ROOT / "analysis"))
sys.path.insert(0, str(ROOT / "curation"))

import apply_name_translations as apply  # noqa: E402
import classify_importability as ci  # noqa: E402
from catalog_paths import analysis_catalog_path  # noqa: E402

NORMALIZED = analysis_catalog_path()
TRANSLATIONS = ROOT / "translate" / "translations.json"
ASSET = ROOT.parents[1] / "app" / "src" / "main" / "assets" / "exercises" / "gymtrack-exercises.json"


class ApplyNameTranslationsTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.catalog = json.loads(NORMALIZED.read_text(encoding="utf-8"))
        cls.translations_file = json.loads(TRANSLATIONS.read_text(encoding="utf-8"))
        cls.translations = apply.load_translations(TRANSLATIONS)

    def test_catalog_counts(self) -> None:
        self.assertEqual(876, len(self.catalog))
        importable = [
            r for r in self.catalog
            if ci.classify(r) in ("IMPORTABLE", "IMPORTABLE_WITH_FALLBACK")
        ]
        rejected = [r for r in self.catalog if ci.classify(r) == "REJECTED"]
        self.assertEqual(777, len(importable))
        self.assertEqual(99, len(rejected))

    def test_translation_status_counts(self) -> None:
        counts = self.translations_file["statusCounts"]
        self.assertEqual(641, counts["TRANSLATED"])
        self.assertEqual(133, counts["UNCHANGED"])
        self.assertEqual(3, counts["REVIEW"])
        self.assertEqual(777, self.translations_file["total"])

    def test_apply_only_changes_name(self) -> None:
        before = copy.deepcopy(self.catalog)
        after, stats = apply.apply_names(before, self.translations)
        apply.assert_only_name_changed(before, after)
        self.assertEqual(876, stats["total"])
        self.assertEqual(777, stats["importable"])
        self.assertEqual(99, stats["rejected"])

    def test_identity_unchanged(self) -> None:
        before = copy.deepcopy(self.catalog)
        after, _ = apply.apply_names(before, self.translations)
        before_ids = [(r["source"], r["externalId"]) for r in before]
        after_ids = [(r["source"], r["externalId"]) for r in after]
        self.assertEqual(before_ids, after_ids)
        self.assertEqual(len(after_ids), len(set(after_ids)))

    def test_source_data_unchanged(self) -> None:
        before = copy.deepcopy(self.catalog)
        after, _ = apply.apply_names(before, self.translations)
        for old, new in zip(before, after):
            self.assertEqual(old["sourceData"], new["sourceData"], msg=old["externalId"])

    def test_muscle_and_equipment_remain_portuguese(self) -> None:
        after, _ = apply.apply_names(copy.deepcopy(self.catalog), self.translations)
        importable = [
            r for r in after
            if ci.classify(r) in ("IMPORTABLE", "IMPORTABLE_WITH_FALLBACK")
        ]
        for row in importable:
            mg = row.get("muscleGroup")
            eq = row.get("equipmentType")
            if mg:
                self.assertNotRegex(mg, r"(?i)^(chest|back|shoulders|biceps)$")
            if eq:
                self.assertNotRegex(eq, r"(?i)^(barbell|dumbbell|cable)$")

    def test_barbell_bench_press_translated(self) -> None:
        after, _ = apply.apply_names(copy.deepcopy(self.catalog), self.translations)
        row = next(r for r in after if r["externalId"] == "Barbell_Bench_Press_-_Medium_Grip")
        self.assertIn("Supino", row["name"])
        self.assertIn("Barra", row["name"])
        self.assertEqual("Peitoral", row["muscleGroup"])
        self.assertEqual("Barra", row["equipmentType"])

    def test_pulldown_not_puxada(self) -> None:
        after, _ = apply.apply_names(copy.deepcopy(self.catalog), self.translations)
        apply.assert_no_pulldown_as_puxada(after)
        pulldowns = [
            r for r in after
            if "pulldown" in (r.get("externalId") or "").lower()
            or "pulldown" in (r.get("name") or "").lower()
        ]
        self.assertTrue(pulldowns)
        for row in pulldowns:
            self.assertNotRegex(row["name"], r"(?i)puxada")

    def test_on_disk_catalog_matches_asset(self) -> None:
        # After Stage 4.9 the Android asset is the curated 136 pack; the full
        # 876 archive (analysis_catalog_path) is kept separate.
        from catalog_paths import PUBLISHED_CATALOG

        self.assertTrue(NORMALIZED.exists())
        self.assertTrue(ASSET.exists())
        self.assertTrue(PUBLISHED_CATALOG.exists())
        self.assertEqual(876, len(self.catalog))
        published = json.loads(PUBLISHED_CATALOG.read_text(encoding="utf-8"))
        self.assertEqual(136, len(published))
        self.assertEqual(PUBLISHED_CATALOG.read_bytes(), ASSET.read_bytes())

    def test_deterministic_apply(self) -> None:
        a, _ = apply.apply_names(copy.deepcopy(self.catalog), self.translations)
        b, _ = apply.apply_names(copy.deepcopy(self.catalog), self.translations)
        ta = apply.dumps_deterministic(a)
        tb = apply.dumps_deterministic(b)
        self.assertEqual(ta, tb)
        self.assertEqual(
            hashlib.sha256(ta.encode()).hexdigest(),
            hashlib.sha256(tb.encode()).hexdigest(),
        )


if __name__ == "__main__":
    unittest.main()
