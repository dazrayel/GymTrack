"""Unit tests for Free Exercise DB → GymTrack intermediate normalization."""

from __future__ import annotations

import json
import unittest
from copy import deepcopy

from mappings import MULTIPLE_PRIMARY_RULE, SOURCE_NAME
from normalize_free_exercise_db import map_equipment, map_muscle, normalize_dataset, normalize_exercise


def valid_row(**overrides):
    row = {
        "id": "Bench_Press",
        "name": "Barbell Bench Press - Medium Grip",
        "force": "push",
        "level": "beginner",
        "mechanic": "compound",
        "equipment": "barbell",
        "primaryMuscles": ["chest"],
        "secondaryMuscles": ["shoulders", "triceps"],
        "instructions": ["Step one."],
        "category": "strength",
        "images": ["Bench_Press/0.jpg"],
    }
    row.update(overrides)
    return row


class MappingTests(unittest.TestCase):
    def test_abdominals_maps_to_abdomen(self):
        self.assertEqual(map_muscle("abdominals"), "Abdômen")
        self.assertEqual(map_muscle("abs"), "Abdômen")
        self.assertEqual(map_muscle("abdomen"), "Abdômen")

    def test_lats_and_middle_back_map_to_costas(self):
        self.assertEqual(map_muscle("lats"), "Costas")
        self.assertEqual(map_muscle("middle back"), "Costas")

    def test_neck_abductors_adductors_unmapped(self):
        self.assertIsNone(map_muscle("neck"))
        self.assertIsNone(map_muscle("abductors"))
        self.assertIsNone(map_muscle("adductors"))

    def test_body_only_and_kettlebells(self):
        self.assertEqual(map_equipment("body only"), ("Peso corporal", "mapped"))
        self.assertEqual(map_equipment("kettlebells"), ("Kettlebell", "mapped"))

    def test_equipment_null_not_invented(self):
        self.assertEqual(map_equipment(None), (None, "null"))


class NormalizeExerciseTests(unittest.TestCase):
    def test_valid_exercise_normalized(self):
        record, events = normalize_exercise(valid_row())
        self.assertFalse(events["failed"])
        self.assertEqual(record["muscleGroup"], "Peitoral")
        self.assertEqual(record["secondaryMuscles"], ["Ombros", "Tríceps"])
        self.assertEqual(record["equipmentType"], "Barra")
        self.assertEqual(record["sourceData"]["force"], "push")

    def test_external_id_and_source_preserved(self):
        record, _ = normalize_exercise(valid_row(id="3_4_Sit-Up"))
        self.assertEqual(record["source"], SOURCE_NAME)
        self.assertEqual(record["externalId"], "3_4_Sit-Up")
        self.assertIsInstance(record["externalId"], str)

    def test_names_are_not_translated(self):
        record, _ = normalize_exercise(valid_row(name="3/4 Sit-Up"))
        self.assertEqual(record["name"], "3/4 Sit-Up")

    def test_secondary_muscles_normalized(self):
        record, _ = normalize_exercise(
            valid_row(secondaryMuscles=["hamstrings", "glutes"]),
        )
        self.assertEqual(record["secondaryMuscles"], ["Posteriores", "Glúteos"])

    def test_duplicate_secondary_removed_after_normalization(self):
        record, events = normalize_exercise(
            valid_row(secondaryMuscles=["shoulders", "Shoulders", "triceps"]),
        )
        self.assertEqual(record["secondaryMuscles"], ["Ombros", "Tríceps"])
        self.assertTrue(events["duplicate_secondary"])

    def test_primary_secondary_overlap_detected(self):
        record, events = normalize_exercise(
            valid_row(
                primaryMuscles=["quadriceps"],
                secondaryMuscles=["quadriceps", "glutes"],
            ),
        )
        self.assertTrue(events["primary_secondary_overlap"])
        self.assertEqual(record["muscleGroup"], "Quadríceps")
        self.assertEqual(record["secondaryMuscles"], ["Glúteos"])
        self.assertNotIn("Quadríceps", record["secondaryMuscles"])

    def test_multiple_primary_detected_first_wins(self):
        record, events = normalize_exercise(
            valid_row(
                id="Kettlebell_Halo_With_Overhead_Extension",
                primaryMuscles=["shoulders", "triceps"],
                secondaryMuscles=[],
            ),
        )
        self.assertTrue(events["multiple_primary"])
        self.assertEqual(record["muscleGroup"], "Ombros")
        self.assertEqual(record["secondaryMuscles"], ["Tríceps"])
        self.assertEqual(
            record["sourceData"]["primaryMuscles"],
            ["shoulders", "triceps"],
        )

    def test_null_equipment_stays_null(self):
        record, events = normalize_exercise(valid_row(equipment=None))
        self.assertIsNone(record["equipmentType"])
        self.assertTrue(events["null_equipment"])
        self.assertIsNone(record["sourceData"]["equipment"])
        self.assertFalse(events["failed"])

    def test_unmapped_equipment_reported(self):
        record, events = normalize_exercise(valid_row(equipment="medicine ball"))
        self.assertIsNone(record["equipmentType"])
        self.assertTrue(events["failed"])
        self.assertEqual(record["sourceData"]["equipment"], "medicine ball")
        self.assertIn("unmapped_equipment:medicine ball", record["issues"])

    def test_unmapped_primary_muscle_reported(self):
        record, events = normalize_exercise(valid_row(primaryMuscles=["neck"]))
        self.assertIsNone(record["muscleGroup"])
        self.assertTrue(events["failed"])
        self.assertIn("neck", events["unmapped_muscles_src"])


class NormalizeDatasetTests(unittest.TestCase):
    def test_duplicate_ids_detected(self):
        rows = [valid_row(), valid_row(name="Copy")]
        output, report = normalize_dataset(rows)
        self.assertEqual(len(output), 2)
        self.assertTrue(any(e.get("error") == "duplicate_id" for e in report["errors"]))

    def test_output_count_matches_input(self):
        rows = [valid_row(id="a"), valid_row(id="b", name="Other")]
        output, report = normalize_dataset(rows)
        self.assertEqual(report["inputExerciseCount"], 2)
        self.assertEqual(report["outputExerciseCount"], 2)
        self.assertEqual(len(output), 2)

    def test_deterministic_output(self):
        rows = [
            valid_row(id="z-last", name="Z"),
            valid_row(id="a-first", name="A", primaryMuscles=["lats"]),
        ]
        out1, rep1 = normalize_dataset(deepcopy(rows))
        out2, rep2 = normalize_dataset(deepcopy(rows))
        self.assertEqual(
            json.dumps(out1, ensure_ascii=False),
            json.dumps(out2, ensure_ascii=False),
        )
        self.assertEqual(
            json.dumps(rep1, ensure_ascii=False),
            json.dumps(rep2, ensure_ascii=False),
        )
        self.assertEqual(out1[0]["externalId"], "z-last")
        self.assertEqual(out1[1]["muscleGroup"], "Costas")

    def test_multiple_primary_rule_documented_in_report(self):
        rows = [
            valid_row(
                id="multi",
                primaryMuscles=["shoulders", "triceps"],
            )
        ]
        _, report = normalize_dataset(rows)
        self.assertEqual(report["multiplePrimaryRule"], MULTIPLE_PRIMARY_RULE)
        self.assertEqual(len(report["multiplePrimaryMuscleExercises"]), 1)


if __name__ == "__main__":
    unittest.main()
