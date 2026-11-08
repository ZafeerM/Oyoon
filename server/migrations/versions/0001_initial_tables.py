"""Initial tables: frames, tolerance_bands, vendor_submissions, jobs (guide section 4.6).

Revision ID: 0001
Revises:
Create Date: 2026-11-08
"""

from alembic import op
import sqlalchemy as sa
from sqlalchemy.dialects import postgresql

revision = "0001"
down_revision = None
branch_labels = None
depends_on = None

JsonB = sa.JSON().with_variant(postgresql.JSONB(), "postgresql")
STATUS_CHECK = "status IN ('queued', 'running', 'done', 'failed')"


def upgrade() -> None:
    op.create_table(
        "frames",
        sa.Column("id", sa.String(32), primary_key=True),
        sa.Column("name", sa.String(200), nullable=False),
        sa.Column("lens_width_mm", sa.Integer, nullable=False),
        sa.Column("bridge_mm", sa.Integer, nullable=False),
        sa.Column("temple_length_mm", sa.Integer, nullable=False),
        sa.Column("lens_height_mm", sa.Float, nullable=False),
        sa.Column("total_width_mm", sa.Float, nullable=False),
        sa.Column("shape", sa.String(50), nullable=False),
        sa.Column("material", sa.String(50), nullable=False),
        sa.Column("colour", sa.String(50), nullable=False),
        sa.Column("price_rs", sa.Integer, nullable=False),
        sa.Column("sprite_key", sa.String(300), nullable=True),
        sa.Column("anchors", JsonB, nullable=True),
        sa.Column("approved", sa.Boolean, nullable=False, server_default=sa.false()),
        sa.Column("created_at", sa.DateTime(timezone=True), nullable=False, server_default=sa.func.now()),
    )
    op.create_table(
        "tolerance_bands",
        sa.Column("check", sa.String(50), primary_key=True),
        sa.Column("good_min_mm", sa.Float, nullable=False),
        sa.Column("good_max_mm", sa.Float, nullable=False),
        sa.Column("ok_min_mm", sa.Float, nullable=False),
        sa.Column("ok_max_mm", sa.Float, nullable=False),
        sa.Column("source", sa.Text, nullable=False),
    )
    op.create_table(
        "vendor_submissions",
        sa.Column("id", sa.Uuid, primary_key=True),
        sa.Column("frame_id", sa.String(32), sa.ForeignKey("frames.id"), nullable=True),
        sa.Column("photo_keys", JsonB, nullable=False),
        sa.Column("spec", JsonB, nullable=False),
        sa.Column("status", sa.String(20), nullable=False, server_default="queued"),
        sa.Column("created_at", sa.DateTime(timezone=True), nullable=False, server_default=sa.func.now()),
        sa.CheckConstraint(STATUS_CHECK, name="vendor_submissions_status"),
    )
    op.create_table(
        "jobs",
        sa.Column("id", sa.Uuid, primary_key=True),
        sa.Column("submission_id", sa.Uuid, sa.ForeignKey("vendor_submissions.id"), nullable=False),
        sa.Column("status", sa.String(20), nullable=False, server_default="queued"),
        sa.Column("attempts", sa.Integer, nullable=False, server_default="0"),
        sa.Column("error", sa.Text, nullable=True),
        sa.Column("created_at", sa.DateTime(timezone=True), nullable=False, server_default=sa.func.now()),
        sa.Column("updated_at", sa.DateTime(timezone=True), nullable=False, server_default=sa.func.now()),
        sa.CheckConstraint(STATUS_CHECK, name="jobs_status"),
    )
    op.create_index("ix_jobs_status_created_at", "jobs", ["status", "created_at"])


def downgrade() -> None:
    op.drop_index("ix_jobs_status_created_at", table_name="jobs")
    op.drop_table("jobs")
    op.drop_table("vendor_submissions")
    op.drop_table("tolerance_bands")
    op.drop_table("frames")
