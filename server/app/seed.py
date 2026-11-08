"""Load the catalogue and tolerance bands from contracts/ into the database.

Run inside the api container:  python -m app.seed
Safe to run again: rows are inserted or updated by id / check name.
"""

import json
from pathlib import Path

from sqlalchemy.orm import Session

from app.config import settings
from app.db.session import SessionLocal
from app.models.tables import Frame, ToleranceBand

FRAME_FIELDS = {
    "id", "name", "lens_width_mm", "lens_height_mm", "bridge_mm", "temple_length_mm",
    "total_width_mm", "shape", "material", "colour", "price_rs", "sprite_key", "anchors", "approved",
}
BAND_FIELDS = {"check", "good_min_mm", "good_max_mm", "ok_min_mm", "ok_max_mm", "source"}


def _load(path: Path) -> list[dict]:
    with path.open(encoding="utf-8") as f:
        rows = json.load(f)
    if not isinstance(rows, list):
        raise ValueError(f"{path} must contain a JSON list")
    return rows


def _pick(row: dict, fields: set[str], path: Path) -> dict:
    unknown = set(row) - fields
    if unknown:
        raise ValueError(f"{path}: unknown fields {sorted(unknown)} in row {row.get('id') or row.get('check')}")
    return row


def seed(session: Session, contracts_dir: Path = settings.contracts_dir) -> tuple[int, int]:
    """Upserts frames and tolerance bands. Returns (frames, bands) loaded."""
    catalogue = contracts_dir / "fixtures" / "catalogue.json"
    bands = contracts_dir / "tolerance-bands.json"

    frame_rows = [_pick(r, FRAME_FIELDS, catalogue) for r in _load(catalogue)]
    band_rows = [_pick(r, BAND_FIELDS, bands) for r in _load(bands)]

    for row in frame_rows:
        session.merge(Frame(**row))
    for row in band_rows:
        session.merge(ToleranceBand(**row))
    session.commit()
    return len(frame_rows), len(band_rows)


def main() -> None:
    with SessionLocal() as session:
        frames, bands = seed(session)
    print(f"Seeded {frames} frames and {bands} tolerance bands from {settings.contracts_dir}")


if __name__ == "__main__":
    main()
