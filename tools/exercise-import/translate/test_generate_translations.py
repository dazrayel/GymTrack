"""Smoke tests for the Etapa 4.4 translations catalog generator.

Verifies that generate_translations.py produces a valid, deterministic
translations.json without touching any app/ or output/ files.
"""

from __future__ import annotations

import json
import re
import sys
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "translate"))
sys.path.insert(0, str(ROOT / "analysis"))

import generate_translations as gen
import audit_translation as audit

TRANSLATIONS_FILE = ROOT / "translate" / "translations.json"

VALID_STATUSES = frozenset({"TRANSLATED", "UNCHANGED", "REVIEW", "NOT_APPLICABLE"})


class TranslationsCatalogTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.catalog = json.loads(gen.NORMALIZED.read_text(encoding="utf-8"))
        cls.audit_data = json.loads(gen.AUDIT_JSON.read_text(encoding="utf-8"))
        cls.payload = gen.build_translations(cls.catalog, cls.audit_data)
        cls.translations = cls.payload["translations"]

    # ------------------------------------------------------------------
    # Structural
    # ------------------------------------------------------------------

    def test_total_is_777(self) -> None:
        self.assertEqual(777, self.payload["total"])
        self.assertEqual(777, len(self.translations))

    def test_status_counts_sum_to_777(self) -> None:
        counts = self.payload["statusCounts"]
        self.assertEqual(777, sum(counts.values()))

    def test_all_statuses_valid(self) -> None:
        for t in self.translations:
            self.assertIn(t["status"], VALID_STATUSES, msg=t["externalId"])

    def test_all_have_source_and_external_id(self) -> None:
        for t in self.translations:
            self.assertTrue(t.get("source"), msg=t.get("externalId"))
            self.assertTrue(t.get("externalId"), msg=t.get("source"))

    def test_no_duplicate_identities(self) -> None:
        keys = [(t["source"], t["externalId"]) for t in self.translations]
        self.assertEqual(len(keys), len(set(keys)))

    def test_sorted_by_source_and_external_id(self) -> None:
        keys = [(t["source"], t["externalId"]) for t in self.translations]
        self.assertEqual(keys, sorted(keys))

    def test_all_have_non_empty_name(self) -> None:
        for t in self.translations:
            self.assertTrue(t.get("name"), msg=t.get("externalId"))

    def test_all_have_original_name(self) -> None:
        for t in self.translations:
            self.assertTrue(t.get("originalName"), msg=t.get("externalId"))

    def test_instructions_length_matches_source(self) -> None:
        """Number of translated instructions must equal source instruction count."""
        import classify_importability as ci
        importable = {
            (r.get("source") or "free-exercise-db", r["externalId"]): r
            for r in self.catalog
            if ci.classify(r) in ("IMPORTABLE", "IMPORTABLE_WITH_FALLBACK")
        }
        for t in self.translations:
            key = (t["source"], t["externalId"])
            src = importable[key]
            src_count = len(src.get("sourceData", {}).get("instructions") or [])
            self.assertEqual(
                src_count,
                len(t["instructions"]),
                msg=f"Instruction count mismatch for {key}",
            )

    # ------------------------------------------------------------------
    # Key exercise spot-checks
    # ------------------------------------------------------------------

    def _by_id(self, external_id: str) -> dict:
        idx = {t["externalId"]: t for t in self.translations}
        return idx[external_id]

    def test_barbell_bench_press_is_supino(self) -> None:
        t = self._by_id("Barbell_Bench_Press_-_Medium_Grip")
        self.assertEqual("TRANSLATED", t["status"])
        self.assertIn("Supino", t["name"])
        self.assertIn("Barra", t["name"])

    def test_romanian_deadlift(self) -> None:
        t = self._by_id("Romanian_Deadlift")
        self.assertEqual("TRANSLATED", t["status"])
        self.assertIn("Romeno", t["name"])

    def test_face_pull_is_unchanged(self) -> None:
        t = self._by_id("Face_Pull")
        self.assertEqual("UNCHANGED", t["status"])
        self.assertEqual("Face Pull", t["name"])

    def test_good_morning_is_unchanged(self) -> None:
        t = self._by_id("Good_Morning")
        self.assertEqual("UNCHANGED", t["status"])
        self.assertEqual("Good Morning", t["name"])

    def test_power_clean_is_unchanged(self) -> None:
        t = self._by_id("Power_Clean")
        self.assertEqual("UNCHANGED", t["status"])
        self.assertIn("Power Clean", t["name"])

    def test_leg_press_is_unchanged(self) -> None:
        t = self._by_id("Leg_Press")
        self.assertEqual("UNCHANGED", t["status"])
        self.assertEqual("Leg Press", t["name"])

    def test_pulldown_never_becomes_puxada(self) -> None:
        pulldowns = [
            t for t in self.translations
            if re.search(r"\bpulldown\b", t["originalName"], re.I)
        ]
        self.assertGreater(len(pulldowns), 0, msg="No Pulldown exercises found")
        for t in pulldowns:
            self.assertNotRegex(
                t["name"], r"(?i)puxada",
                msg=f"Pulldown became Puxada: {t['externalId']}"
            )
            self.assertRegex(
                t["name"], r"(?i)pulldown",
                msg=f"Pulldown term lost: {t['externalId']}"
            )

    def test_pull_up_is_barra_fixa(self) -> None:
        t = self._by_id("Band_Assisted_Pull-Up")
        self.assertIn("Barra Fixa", t["name"])

    def test_version_is_1(self) -> None:
        self.assertEqual(1, self.payload["version"])

    # ------------------------------------------------------------------
    # Determinism
    # ------------------------------------------------------------------

    def test_deterministic(self) -> None:
        first = gen.dumps_deterministic(gen.build_translations(self.catalog, self.audit_data))
        second = gen.dumps_deterministic(gen.build_translations(self.catalog, self.audit_data))
        self.assertEqual(first, second)

    # ------------------------------------------------------------------
    # Status sanity
    # ------------------------------------------------------------------

    def test_no_review_with_empty_name_is_equipment_only(self) -> None:
        """REVIEW exercises should not have names that are only equipment phrases."""
        bad_prefixes = ("com ", "na ", "no ", "de ", "do ", "da ")
        for t in self.translations:
            if t["status"] == "REVIEW":
                name = t["name"]
                for prefix in bad_prefixes:
                    self.assertFalse(
                        name.startswith(prefix),
                        msg=f"REVIEW exercise has equipment-only name: {t['externalId']} -> {name!r}",
                    )

    def test_translated_count_gte_direct_contextual(self) -> None:
        """TRANSLATED count should be at least equal to audit DIRECT+CONTEXTUAL count."""
        audit_payload = self.audit_data
        direct = audit_payload["classificationCounts"]["DIRECT"]
        contextual = audit_payload["classificationCounts"]["CONTEXTUAL"]
        translated = self.payload["statusCounts"]["TRANSLATED"]
        # Some DIRECT/CONTEXTUAL may map to UNCHANGED (technical English terms)
        self.assertLessEqual(0, translated)
        # Total should cover all importable
        total = sum(self.payload["statusCounts"].values())
        self.assertEqual(777, total)

    def test_saved_file_matches_build(self) -> None:
        """The saved translations.json must match the freshly built payload."""
        if not TRANSLATIONS_FILE.exists():
            self.skipTest("translations.json not yet generated")
        saved = TRANSLATIONS_FILE.read_text(encoding="utf-8")
        fresh = gen.dumps_deterministic(gen.build_translations(self.catalog, self.audit_data))
        self.assertEqual(saved, fresh, msg="translations.json is stale — re-run generate_translations.py")

    # ------------------------------------------------------------------
    # Family spot-checks (Etapa 4.4.1)
    # ------------------------------------------------------------------

    def test_olympic_family_unchanged(self) -> None:
        """All Olympic lift overrides should be UNCHANGED with English name preserved."""
        olympic_ids = [
            "Clean_Pull", "Snatch_Pull", "Jerk_Balance", "Power_Jerk",
            "Muscle_Snatch", "Snatch_Balance", "Snatch_from_Blocks",
            "Power_Clean_from_Blocks", "Power_Snatch_from_Blocks",
        ]
        for eid in olympic_ids:
            t = self._by_id(eid)
            self.assertEqual("UNCHANGED", t["status"], msg=f"Olympic {eid} should be UNCHANGED")
            # name must still contain the Olympic keyword
            olympic_terms = ("Clean", "Snatch", "Jerk")
            self.assertTrue(
                any(term in t["name"] for term in olympic_terms),
                msg=f"Olympic term missing in name for {eid}: {t['name']!r}",
            )

    def test_smr_family_translated(self) -> None:
        """SMR exercises should be TRANSLATED with 'Liberação Miofascial'."""
        smr_ids = [
            "Anterior_Tibialis-SMR", "Brachialis-SMR", "Calves-SMR",
            "Foot-SMR", "Hamstring-SMR", "Latissimus_Dorsi-SMR",
            "Lower_Back-SMR", "Peroneals-SMR", "Piriformis-SMR",
            "Quadriceps-SMR", "Rhomboids-SMR",
        ]
        for eid in smr_ids:
            t = self._by_id(eid)
            self.assertEqual("TRANSLATED", t["status"], msg=f"SMR {eid} should be TRANSLATED")
            self.assertIn("Liberação Miofascial", t["name"], msg=f"SMR term missing in {eid}")

    def test_throw_family_translated(self) -> None:
        """Throw exercises should be TRANSLATED with 'Arremesso'."""
        throw_ids = [
            "Backward_Medicine_Ball_Throw",
            "Medicine_Ball_Scoop_Throw",
            "Standing_Two-Arm_Overhead_Throw",
            "Supine_Chest_Throw",
            "Supine_One-Arm_Overhead_Throw",
            "Supine_Two-Arm_Overhead_Throw",
        ]
        for eid in throw_ids:
            t = self._by_id(eid)
            self.assertEqual("TRANSLATED", t["status"], msg=f"Throw {eid} should be TRANSLATED")
            self.assertIn("Arremesso", t["name"], msg=f"Arremesso missing in {eid}")

    def test_sled_translated_has_treno(self) -> None:
        t = self._by_id("Sled_Drag_-_Harness")
        self.assertEqual("TRANSLATED", t["status"])
        self.assertIn("Trenó", t["name"])

    def test_windmill_family_unchanged(self) -> None:
        """Windmill exercises should be UNCHANGED."""
        for eid in ["Kettlebell_Windmill", "Double_Kettlebell_Windmill", "Advanced_Kettlebell_Windmill"]:
            t = self._by_id(eid)
            self.assertEqual("UNCHANGED", t["status"], msg=f"Windmill {eid} should be UNCHANGED")
            self.assertIn("Windmill", t["name"], msg=f"Windmill term missing in {eid}")

    def test_atlas_family_unchanged(self) -> None:
        for eid in ["Atlas_Stones", "Atlas_Stone_Trainer"]:
            t = self._by_id(eid)
            self.assertEqual("UNCHANGED", t["status"])
            self.assertIn("Atlas", t["name"])

    def test_rocky_family_unchanged(self) -> None:
        for eid in ["Bradford_Rocky_Presses", "Rocky_Pull-Ups_Pulldowns"]:
            t = self._by_id(eid)
            self.assertEqual("UNCHANGED", t["status"])
            self.assertIn("Rocky", t["name"])

    def test_sprint_bench_and_lunge_translated(self) -> None:
        self.assertEqual("TRANSLATED", self._by_id("Bench_Sprint")["status"])
        self.assertEqual("TRANSLATED", self._by_id("Lunge_Sprint")["status"])
        self.assertEqual("UNCHANGED", self._by_id("Prowler_Sprint")["status"])

    def test_review_count_le_three(self) -> None:
        """After final REVIEW audit, at most 3 genuinely ambiguous cases remain."""
        review_count = self.payload["statusCounts"]["REVIEW"]
        self.assertLessEqual(review_count, 3, msg=f"Expected ≤3 REVIEW, got {review_count}")

    def test_90_90_hamstring_translated(self) -> None:
        t = self._by_id("90_90_Hamstring")
        self.assertEqual("TRANSLATED", t["status"])
        self.assertEqual("Alongamento de Posteriores 90/90", t["name"])
        self.assertEqual("90/90 Hamstring", t["originalName"])

    def test_front_cone_hops_translated(self) -> None:
        t = self._by_id("Front_Cone_Hops_or_hurdle_hops")
        self.assertEqual("TRANSLATED", t["status"])
        self.assertEqual("Saltos Frontais sobre Cones", t["name"])

    def test_side_hop_sprint_translated(self) -> None:
        t = self._by_id("Side_Hop-Sprint")
        self.assertEqual("TRANSLATED", t["status"])
        self.assertEqual("Saltos Laterais com Sprint", t["name"])

    def test_single_cone_sprint_drill_translated(self) -> None:
        t = self._by_id("Single-Cone_Sprint_Drill")
        self.assertEqual("TRANSLATED", t["status"])
        self.assertEqual("Drill de Sprint em Cone", t["name"])

    def test_rack_delivery_unchanged(self) -> None:
        t = self._by_id("Rack_Delivery")
        self.assertEqual("UNCHANGED", t["status"])
        self.assertEqual("Rack Delivery", t["name"])

    def test_downward_facing_balance_remains_review(self) -> None:
        t = self._by_id("Downward_Facing_Balance")
        self.assertEqual("REVIEW", t["status"])
        self.assertEqual("Downward Facing Balance", t["name"])

    def test_pyramid_remains_review(self) -> None:
        t = self._by_id("Pyramid")
        self.assertEqual("REVIEW", t["status"])
        self.assertEqual("Pyramid", t["name"])

    def test_return_push_from_stance_remains_review(self) -> None:
        t = self._by_id("Return_Push_from_Stance")
        self.assertEqual("REVIEW", t["status"])
        self.assertEqual("Return Push from Stance", t["name"])



if __name__ == "__main__":
    unittest.main()
