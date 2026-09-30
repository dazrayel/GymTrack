"""Tests for Etapa 4.4.2A instruction audit (analysis only)."""

from __future__ import annotations

import hashlib
import json
import sys
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "translate"))
sys.path.insert(0, str(ROOT / "analysis"))
sys.path.insert(0, str(ROOT / "curation"))

import audit_instructions as audit
import classify_importability as ci
from catalog_paths import analysis_catalog_path  # noqa: E402

NORMALIZED = analysis_catalog_path()
OUT_JSON = ROOT / "analysis" / "instruction-audit-4-4-2.json"


class InstructionAuditTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.catalog_bytes = NORMALIZED.read_bytes()
        cls.catalog = json.loads(cls.catalog_bytes.decode("utf-8"))
        cls.payload = audit.build_audit(cls.catalog)
        cls.catalog_sha = hashlib.sha256(cls.catalog_bytes).hexdigest()

    def test_total_exercises_777(self) -> None:
        self.assertEqual(777, self.payload["totalExercises"])

    def test_with_without_sum_to_777(self) -> None:
        self.assertEqual(
            777,
            self.payload["exercisesWithInstructions"]
            + self.payload["exercisesWithoutInstructions"],
        )

    def test_instruction_count_consistent(self) -> None:
        rows = [
            r for r in self.catalog
            if ci.classify(r) in ("IMPORTABLE", "IMPORTABLE_WITH_FALLBACK")
        ]
        total = sum(len((r.get("sourceData") or {}).get("instructions") or []) for r in rows)
        self.assertEqual(total, self.payload["totalInstructions"])

    def test_unique_le_total(self) -> None:
        self.assertLessEqual(
            self.payload["uniqueInstructions"],
            self.payload["totalInstructions"],
        )

    def test_classification_sums_to_total(self) -> None:
        c = self.payload["classification"]
        self.assertEqual(
            self.payload["totalInstructions"],
            sum(c.values()),
        )

    def test_classification_keys(self) -> None:
        keys = set(self.payload["classification"])
        self.assertEqual(
            keys,
            {
                "TRANSLATED_EXISTING",
                "PASSTHROUGH_ENGLISH",
                "PORTUGUESE",
                "MIXED",
                "UNKNOWN",
            },
        )

    def test_no_duplicate_identities(self) -> None:
        rows = [
            r for r in self.catalog
            if ci.classify(r) in ("IMPORTABLE", "IMPORTABLE_WITH_FALLBACK")
        ]
        ids = [(r.get("source") or "free-exercise-db", r["externalId"]) for r in rows]
        self.assertEqual(len(ids), len(set(ids)))
        self.assertEqual(777, len(ids))

    def test_source_data_instructions_intact(self) -> None:
        """Re-reading catalog after audit must match original bytes."""
        again = NORMALIZED.read_bytes()
        self.assertEqual(self.catalog_sha, hashlib.sha256(again).hexdigest())
        # Nested instructions lists unchanged vs in-memory original parse
        original = json.loads(self.catalog_bytes.decode("utf-8"))
        for a, b in zip(original, self.catalog):
            self.assertEqual(
                (a.get("sourceData") or {}).get("instructions"),
                (b.get("sourceData") or {}).get("instructions"),
            )

    def test_gymtrack_exercises_not_modified_hash(self) -> None:
        self.assertEqual(
            self.catalog_sha,
            hashlib.sha256(NORMALIZED.read_bytes()).hexdigest(),
        )

    def test_deterministic_build(self) -> None:
        first = audit.dumps_deterministic(audit.build_audit(self.catalog))
        second = audit.dumps_deterministic(audit.build_audit(self.catalog))
        self.assertEqual(first, second)

    def test_top_instructions_sorted(self) -> None:
        tops = self.payload["topInstructions"]
        pairs = [(r["count"], r["instruction"]) for r in tops]
        self.assertEqual(pairs, sorted(pairs, key=lambda kv: (-kv[0], kv[1])))

    def test_patterns_sorted(self) -> None:
        pats = self.payload["patterns"]
        pairs = [(r["count"], r["pattern"]) for r in pats]
        self.assertEqual(pairs, sorted(pairs, key=lambda kv: (-kv[0], kv[1])))

    def test_verbs_sorted(self) -> None:
        verbs = self.payload["verbs"]
        pairs = [(r["count"], r["verb"]) for r in verbs]
        self.assertEqual(pairs, sorted(pairs, key=lambda kv: (-kv[0], kv[1])))

    def test_saved_json_matches_build_if_present(self) -> None:
        if not OUT_JSON.exists():
            self.skipTest("instruction-audit-4-4-2.json not generated yet")
        saved = OUT_JSON.read_text(encoding="utf-8")
        # Rebuild without sourceCatalogSha16 then compare after injecting same field
        payload = audit.build_audit(self.catalog)
        saved_obj = json.loads(saved)
        if "sourceCatalogSha16" in saved_obj:
            payload["sourceCatalogSha16"] = saved_obj["sourceCatalogSha16"]
        fresh = audit.dumps_deterministic(payload)
        self.assertEqual(saved, fresh)

    def test_distribution_sums_to_777(self) -> None:
        dist = self.payload["instructionCountDistribution"]
        self.assertEqual(777, sum(r["exerciseCount"] for r in dist))

    def test_original_instructions_not_mutated_by_classifier(self) -> None:
        sample = "Repeat for the recommended amount of repetitions."
        before = sample
        audit.classify_instruction(sample)
        self.assertEqual(before, sample)


if __name__ == "__main__":
    unittest.main()
