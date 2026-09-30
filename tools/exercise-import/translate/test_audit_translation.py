"""Tests for the pt-BR translation audit tool (Etapa 4.3)."""

from __future__ import annotations

import json
import sys
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "translate"))
sys.path.insert(0, str(ROOT / "analysis"))

import audit_translation as audit
from classify_importability import classify as classify_importability


class TranslationAuditTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.catalog = json.loads(audit.NORMALIZED.read_text(encoding="utf-8"))
        cls.glossary = audit._load_glossary()

    def test_importable_count_is_777(self) -> None:
        rows = audit.importable_rows(self.catalog)
        self.assertEqual(777, len(rows))
        self.assertEqual(876, len(self.catalog))
        rejected = sum(1 for row in self.catalog if classify_importability(row) == "REJECTED")
        self.assertEqual(99, rejected)

    def test_barbell_bench_press_is_supino_not_word_salad(self) -> None:
        result = audit.audit_name("Barbell Bench Press", self.glossary)
        self.assertIn("Supino", result["suggestedName"])
        self.assertIn("Barra", result["suggestedName"])
        self.assertNotIn("Pressão", result["suggestedName"])
        self.assertNotIn("Banco Press", result["suggestedName"])

    def test_pulldown_is_not_puxada(self) -> None:
        for name in (
            "Lat Pulldown",
            "Straight-Arm Pulldown",
            "Wide-Grip Lat Pulldown",
            "V-Bar Pulldown",
            "Underhand Cable Pulldowns",
        ):
            suggested = audit.audit_name(name, self.glossary)["suggestedName"]
            self.assertNotRegex(suggested, r"(?i)puxada")
            self.assertRegex(suggested, r"(?i)pulldown")

    def test_pull_up_is_barra_fixa(self) -> None:
        result = audit.audit_name("Band Assisted Pull-Up", self.glossary)
        self.assertIn("Barra Fixa", result["suggestedName"])

    def test_chin_up_is_supinated(self) -> None:
        result = audit.audit_name("Chin-Up", self.glossary)
        self.assertIn("Barra Fixa Supinada", result["suggestedName"])

    def test_face_pull_kept(self) -> None:
        result = audit.audit_name("Face Pull", self.glossary)
        self.assertEqual("Face Pull", result["suggestedName"])

    def test_report_validates_and_is_deterministic(self) -> None:
        first = audit.dumps_deterministic(audit.build_report(self.catalog, self.glossary))
        second = audit.dumps_deterministic(audit.build_report(self.catalog, self.glossary))
        self.assertEqual(first, second)
        payload = json.loads(first)
        self.assertEqual(777, payload["totalImportable"])
        self.assertEqual(777, len(payload["exercises"]))
        counts = payload["classificationCounts"]
        self.assertEqual(777, counts["DIRECT"] + counts["CONTEXTUAL"] + counts["REVIEW"])
        ids = [(e["source"], e["externalId"]) for e in payload["exercises"]]
        self.assertEqual(ids, sorted(ids))
        self.assertEqual(len(ids), len(set(ids)))

    # --- Etapa 4.3.1 refinement tests ---

    def test_skull_crusher_is_direct(self) -> None:
        for name in ("Band Skull Crusher", "Decline Close-Grip Bench To Skull Crusher"):
            result = audit.audit_name(name, self.glossary)
            self.assertIn("Skull Crusher", result["suggestedName"])

    def test_chains_produce_direct(self) -> None:
        result = audit.audit_name("Deadlift with Chains", self.glossary)
        self.assertEqual("DIRECT", result["classification"])
        self.assertIn("Correntes", result["suggestedName"])

    def test_leverage_equipment_resolved(self) -> None:
        result = audit.audit_name("Leverage Chest Press", self.glossary)
        self.assertIn("Supino", result["suggestedName"])
        self.assertIn("Leverage", result["suggestedName"])

    def test_box_squat_and_box_jump_are_direct(self) -> None:
        box_squat = audit.audit_name("Box Squat", self.glossary)
        self.assertEqual("DIRECT", box_squat["classification"])
        self.assertIn("Box", box_squat["suggestedName"])
        box_jump = audit.audit_name("Front Box Jump", self.glossary)
        self.assertEqual("DIRECT", box_jump["classification"])
        self.assertIn("Box Jump", box_jump["suggestedName"])

    def test_jump_squat_is_direct(self) -> None:
        result = audit.audit_name("Weighted Jump Squat", self.glossary)
        self.assertEqual("DIRECT", result["classification"])
        self.assertIn("Agachamento com Salto", result["suggestedName"])

    def test_hack_squat_is_direct(self) -> None:
        result = audit.audit_name("Narrow Stance Hack Squats", self.glossary)
        self.assertIn("Hack", result["suggestedName"])

    def test_hip_movements_resolved(self) -> None:
        for name, expected in (
            ("Cable Hip Adduction", "Adução de Quadril"),
            ("Hip Flexion with Band", "Flexão de Quadril"),
            ("Smith Machine Hip Raise", "Elevação de Quadril"),
        ):
            result = audit.audit_name(name, self.glossary)
            self.assertIn(expected, result["suggestedName"], msg=f"Failed for {name!r}")
            self.assertNotEqual("REVIEW", result["classification"], msg=f"Still REVIEW for {name!r}")

    def test_leg_raise_is_direct(self) -> None:
        result = audit.audit_name("Flat Bench Lying Leg Raise", self.glossary)
        self.assertIn("Elevação de Pernas", result["suggestedName"])

    def test_stretch_as_core_movement(self) -> None:
        for name in ("Standing Biceps Stretch", "Quad Stretch"):
            result = audit.audit_name(name, self.glossary)
            self.assertIn("Alongamento", result["suggestedName"])
            self.assertNotEqual("REVIEW", result["classification"])

    def test_oblique_modifier_resolved(self) -> None:
        result = audit.audit_name("Oblique Crunches", self.glossary)
        self.assertIn("Abdominal", result["suggestedName"])
        self.assertIn("Oblíquo", result["suggestedName"])

    def test_hyperextension_direct(self) -> None:
        result = audit.audit_name("Reverse Hyperextension", self.glossary)
        self.assertIn("Hiperextensão", result["suggestedName"])
        self.assertNotEqual("REVIEW", result["classification"])

    def test_crossover_contextual(self) -> None:
        result = audit.audit_name("Cable Crossover", self.glossary)
        self.assertIn("Crossover", result["suggestedName"])
        self.assertNotEqual("REVIEW", result["classification"])

    def test_sled_compounds_resolved(self) -> None:
        push = audit.audit_name("Sled Push", self.glossary)
        self.assertEqual("DIRECT", push["classification"])
        self.assertIn("Trenó", push["suggestedName"])
        row = audit.audit_name("Sled Row", self.glossary)
        self.assertEqual("DIRECT", row["classification"])

    def test_delt_modifier_resolved(self) -> None:
        result = audit.audit_name("Lying Rear Delt Raise", self.glossary)
        self.assertIn("Deltóide", result["suggestedName"])
        self.assertNotEqual("REVIEW", result["classification"])

    def test_review_count_after_refinement(self) -> None:
        payload = audit.build_report(self.catalog, self.glossary)
        counts = payload["classificationCounts"]
        # After Etapa 4.3.1: REVIEW must be <= 429 (down from 515)
        self.assertLessEqual(counts["REVIEW"], 429)
        self.assertGreaterEqual(counts["DIRECT"], 258)


if __name__ == "__main__":
    unittest.main()
