"""Tests for Etapa 4.4.2B instruction translation."""

from __future__ import annotations

import json
import sys
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "translate"))
sys.path.insert(0, str(ROOT / "analysis"))

import translate_instructions as ti
from instruction_patterns import (
    STATUS_NOT_APPLICABLE,
    STATUS_REVIEW,
    STATUS_TRANSLATED,
)


class TranslateInstructionsTests(unittest.TestCase):
    def test_existing_lookup_repeat(self) -> None:
        r = ti.translate_sentence("Repeat for the recommended amount of repetitions.")
        self.assertEqual(STATUS_TRANSLATED, r.status)
        self.assertIn("Repita", r.text)
        self.assertIn("repetições", r.text)

    def test_return_starting_position(self) -> None:
        r = ti.translate_sentence("Return to the starting position.")
        self.assertEqual(STATUS_TRANSLATED, r.status)
        self.assertEqual("Retorne à posição inicial.", r.text)

    def test_keep_back_straight(self) -> None:
        r = ti.translate_sentence("Keep your back straight.")
        self.assertEqual(STATUS_TRANSLATED, r.status)
        self.assertEqual("Mantenha as costas retas.", r.text)

    def test_keep_back_straight_throughout(self) -> None:
        r = ti.translate_sentence("Keep your back straight throughout the movement.")
        self.assertEqual(STATUS_TRANSLATED, r.status)
        self.assertIn("durante todo o movimento", r.text)

    def test_hold_seconds_pattern(self) -> None:
        r = ti.translate_sentence("Hold this position for 10 seconds.")
        self.assertEqual(STATUS_TRANSLATED, r.status)
        self.assertEqual("Mantenha essa posição por 10 segundos.", r.text)

    def test_empty_not_applicable(self) -> None:
        r = ti.translate_sentence("")
        self.assertEqual(STATUS_NOT_APPLICABLE, r.status)
        self.assertEqual("", r.text)

    def test_whitespace_empty_not_applicable(self) -> None:
        r = ti.translate_sentence("   ")
        self.assertEqual(STATUS_NOT_APPLICABLE, r.status)

    def test_partner_stays_review(self) -> None:
        s = "You will need a partner for this drill."
        r = ti.translate_sentence(s)
        self.assertEqual(STATUS_REVIEW, r.status)
        self.assertEqual(s, r.text)

    def test_pulldown_never_puxada(self) -> None:
        # Even if a sentence mentioning pulldown is translated, never Puxada
        for en, _pt in ti.SENTENCE_LOOKUP.items():
            if "pulldown" in en.lower() or "pull-down" in en.lower():
                r = ti.translate_sentence(en)
                self.assertNotRegex(r.text, r"(?i)puxada")

    def test_pull_up_barra_fixa_in_lookup(self) -> None:
        s = "Grab the pull-up bar with the palms facing forward using a wide grip."
        r = ti.translate_sentence(s)
        self.assertEqual(STATUS_TRANSLATED, r.status)
        self.assertIn("Barra Fixa", r.text)
        self.assertNotRegex(r.text, r"(?i)puxada")

    def test_face_pull_preserved_if_present(self) -> None:
        # Pattern allowlist keeps Face Pull when compose/patterns mention it
        from instruction_patterns import try_safe_compose

        out = try_safe_compose("Perform a face pull with control.")
        if out:
            self.assertIn("Face Pull", out)

    def test_preserves_length_and_order(self) -> None:
        src = [
            "Return to the starting position.",
            "Repeat for the recommended amount of repetitions.",
            "You will need a partner for this drill.",
        ]
        out, _ = ti.translate_instructions(src)
        self.assertEqual(3, len(out))
        self.assertEqual("Retorne à posição inicial.", out[0])
        self.assertIn("Repita", out[1])
        self.assertEqual(src[2], out[2])  # REVIEW keeps English

    def test_empty_slot_preserved_in_list(self) -> None:
        src = ["Return to the starting position.", "", "Repeat for the recommended amount of repetitions."]
        out, results = ti.translate_instructions_detailed(src)
        self.assertEqual(3, len(out))
        self.assertEqual("", out[1])
        self.assertEqual(STATUS_NOT_APPLICABLE, results[1].status)

    def test_numbers_preserved(self) -> None:
        r = ti.translate_sentence("Hold this position for 10 seconds.")
        self.assertIn("10", r.text)

    def test_no_mixed_gibberish_on_passthrough(self) -> None:
        s = "Lie down on the floor and secure your feet. Your legs should be bent at the knees."
        r = ti.translate_sentence(s)
        # Either fully translated via lookup or left as REVIEW English — never mixed
        if r.status == STATUS_REVIEW:
            self.assertEqual(s, r.text)
        else:
            self.assertNotRegex(r.text, r"\b(your|the|with|should)\b")

    def test_deterministic(self) -> None:
        s = "Keep your chest up."
        a = ti.translate_sentence(s)
        b = ti.translate_sentence(s)
        self.assertEqual(a, b)

    def test_exact_match_property_compat(self) -> None:
        r = ti.translate_sentence("Return to the starting position.")
        self.assertTrue(r.exact_match)
        r2 = ti.translate_sentence("Some unique rare sentence xyzzy.")
        self.assertFalse(r2.exact_match)


if __name__ == "__main__":
    unittest.main()
