"""Database tables (developer guide section 4.6)."""

import uuid
from datetime import datetime
from typing import Any

from sqlalchemy import (
    JSON,
    CheckConstraint,
    DateTime,
    Float,
    ForeignKey,
    Index,
    Integer,
    String,
    Text,
    Uuid,
    false,
    func,
)
from sqlalchemy.dialects.postgresql import JSONB
from sqlalchemy.orm import DeclarativeBase, Mapped, mapped_column

# JSONB on PostgreSQL, plain JSON elsewhere (SQLite in tests).
JsonB = JSON().with_variant(JSONB(), "postgresql")

STATUS_CHECK = "status IN ('queued', 'running', 'done', 'failed')"


class Base(DeclarativeBase):
    pass


class Frame(Base):
    """An eyeglass frame in the catalogue."""

    __tablename__ = "frames"

    id: Mapped[str] = mapped_column(String(32), primary_key=True)
    name: Mapped[str] = mapped_column(String(200))
    # Printed on the temple arm (e.g. 52□18-140), so whole millimetres.
    lens_width_mm: Mapped[int] = mapped_column(Integer)
    bridge_mm: Mapped[int] = mapped_column(Integer)
    temple_length_mm: Mapped[int] = mapped_column(Integer)
    # Measured with a ruler, so may have decimals.
    lens_height_mm: Mapped[float] = mapped_column(Float)
    total_width_mm: Mapped[float] = mapped_column(Float)
    shape: Mapped[str] = mapped_column(String(50))
    material: Mapped[str] = mapped_column(String(50))
    colour: Mapped[str] = mapped_column(String(50))
    price_rs: Mapped[int] = mapped_column(Integer)
    # Object key in the sprites bucket, e.g. "f-0007/front.png"; null until a sprite exists.
    sprite_key: Mapped[str | None] = mapped_column(String(300))
    anchors: Mapped[dict[str, Any] | None] = mapped_column(JsonB)
    approved: Mapped[bool] = mapped_column(default=False, server_default=false())
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now())


class ToleranceBand(Base):
    """Fit tolerance for one check, with its source."""

    __tablename__ = "tolerance_bands"

    check: Mapped[str] = mapped_column(String(50), primary_key=True)
    good_min_mm: Mapped[float] = mapped_column(Float)
    good_max_mm: Mapped[float] = mapped_column(Float)
    ok_min_mm: Mapped[float] = mapped_column(Float)
    ok_max_mm: Mapped[float] = mapped_column(Float)
    source: Mapped[str] = mapped_column(Text)


class VendorSubmission(Base):
    """Photos and specs a vendor uploaded for a new frame."""

    __tablename__ = "vendor_submissions"
    __table_args__ = (CheckConstraint(STATUS_CHECK, name="vendor_submissions_status"),)

    id: Mapped[uuid.UUID] = mapped_column(Uuid, primary_key=True, default=uuid.uuid4)
    frame_id: Mapped[str | None] = mapped_column(ForeignKey("frames.id"))
    photo_keys: Mapped[dict[str, Any]] = mapped_column(JsonB)
    spec: Mapped[dict[str, Any]] = mapped_column(JsonB)
    status: Mapped[str] = mapped_column(String(20), default="queued", server_default="queued")
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now())


class Job(Base):
    """A unit of worker work for one submission."""

    __tablename__ = "jobs"
    __table_args__ = (
        CheckConstraint(STATUS_CHECK, name="jobs_status"),
        # Serves the worker's "oldest queued job" claim query.
        Index("ix_jobs_status_created_at", "status", "created_at"),
    )

    id: Mapped[uuid.UUID] = mapped_column(Uuid, primary_key=True, default=uuid.uuid4)
    submission_id: Mapped[uuid.UUID] = mapped_column(ForeignKey("vendor_submissions.id"))
    status: Mapped[str] = mapped_column(String(20), default="queued", server_default="queued")
    attempts: Mapped[int] = mapped_column(Integer, default=0, server_default="0")
    error: Mapped[str | None] = mapped_column(Text)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now())
    updated_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), server_default=func.now(), onupdate=func.now()
    )
