"""Catalogue endpoints: frames and tolerance bands."""

from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy import select
from sqlalchemy.orm import Session

from app.db.session import get_db
from app.models.schemas import FrameOut, ToleranceBandOut
from app.models.tables import Frame, ToleranceBand
from app.storage.urls import sprite_url

router = APIRouter()


def _frame_out(frame: Frame) -> FrameOut:
    return FrameOut(
        id=frame.id,
        name=frame.name,
        lens_width_mm=frame.lens_width_mm,
        lens_height_mm=frame.lens_height_mm,
        bridge_mm=frame.bridge_mm,
        temple_length_mm=frame.temple_length_mm,
        total_width_mm=frame.total_width_mm,
        shape=frame.shape,
        material=frame.material,
        colour=frame.colour,
        price_rs=frame.price_rs,
        sprite_url=sprite_url(frame.sprite_key),
        anchors=frame.anchors,
    )


@router.get("/frames", response_model=list[FrameOut])
def list_frames(db: Session = Depends(get_db)) -> list[FrameOut]:
    """All approved frames."""
    frames = db.scalars(select(Frame).where(Frame.approved.is_(True)).order_by(Frame.id))
    return [_frame_out(f) for f in frames]


@router.get("/frames/{frame_id}", response_model=FrameOut)
def get_frame(frame_id: str, db: Session = Depends(get_db)) -> FrameOut:
    """One approved frame, or 404."""
    frame = db.get(Frame, frame_id)
    if frame is None or not frame.approved:
        raise HTTPException(status_code=404, detail=f"frame {frame_id} not found")
    return _frame_out(frame)


@router.get("/tolerance-bands", response_model=list[ToleranceBandOut])
def list_tolerance_bands(db: Session = Depends(get_db)) -> list[ToleranceBand]:
    """Contents of contracts/tolerance-bands.json, as loaded into the database."""
    return list(db.scalars(select(ToleranceBand).order_by(ToleranceBand.check)))
