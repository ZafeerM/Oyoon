"""The Alembic migration creates every table from section 4.6 (run here on SQLite)."""

from pathlib import Path

from alembic import command
from alembic.config import Config
from sqlalchemy import create_engine, inspect

SERVER_DIR = Path(__file__).resolve().parents[1]


def test_upgrade_head_creates_all_tables(tmp_path: Path) -> None:
    url = f"sqlite:///{tmp_path / 'migrate.db'}"
    cfg = Config(str(SERVER_DIR / "alembic.ini"))
    cfg.set_main_option("sqlalchemy.url", url)

    command.upgrade(cfg, "head")

    tables = set(inspect(create_engine(url)).get_table_names())
    assert {"frames", "tolerance_bands", "vendor_submissions", "jobs"} <= tables


def test_downgrade_removes_tables(tmp_path: Path) -> None:
    url = f"sqlite:///{tmp_path / 'migrate.db'}"
    cfg = Config(str(SERVER_DIR / "alembic.ini"))
    cfg.set_main_option("sqlalchemy.url", url)

    command.upgrade(cfg, "head")
    command.downgrade(cfg, "base")

    tables = set(inspect(create_engine(url)).get_table_names())
    assert not tables & {"frames", "tolerance_bands", "vendor_submissions", "jobs"}
