"""Shared paths for full vs published GymTrack exercise catalogs."""

from __future__ import annotations

from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
OUT_DIR = ROOT / "output" / "free-exercise-db"
PUBLISHED_CATALOG = OUT_DIR / "gymtrack-exercises.json"
FULL_876_CATALOG = OUT_DIR / "gymtrack-exercises-full-876.json"


def analysis_catalog_path() -> Path:
    """Catalog used by curation/analysis tools (full 876 when archived)."""
    if FULL_876_CATALOG.exists():
        return FULL_876_CATALOG
    return PUBLISHED_CATALOG
