"""Settings read from environment variables (see docker-compose.yml)."""

from pathlib import Path

from pydantic_settings import BaseSettings

# server/app/config.py -> the repo root is two levels above server/.
_REPO_ROOT = Path(__file__).resolve().parents[2]


class Settings(BaseSettings):
    database_url: str = "postgresql+psycopg://oyoon:oyoon@localhost:5432/oyoon"
    # Public base URL of the sprites bucket; a frame's sprite_url is this + "/" + sprite_key.
    sprite_base_url: str = "http://localhost:9000/sprites"
    # Folder holding the contracts/ files (catalogue, tolerance bands) used for seeding.
    contracts_dir: Path = _REPO_ROOT / "contracts"


settings = Settings()
