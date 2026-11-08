"""API response shapes (developer guide section 4.6)."""

from typing import Any

from pydantic import BaseModel, ConfigDict


class FrameOut(BaseModel):
    id: str
    name: str
    lens_width_mm: int
    lens_height_mm: float
    bridge_mm: int
    temple_length_mm: int
    total_width_mm: float
    shape: str
    material: str
    colour: str
    price_rs: int
    sprite_url: str | None
    anchors: dict[str, Any] | None


class ToleranceBandOut(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    check: str
    good_min_mm: float
    good_max_mm: float
    ok_min_mm: float
    ok_max_mm: float
    source: str
