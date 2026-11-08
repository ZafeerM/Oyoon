"""Public URLs for objects in storage (MinIO)."""

from app.config import settings


def sprite_url(sprite_key: str | None) -> str | None:
    """URL of a frame's sprite, or None if the frame has no sprite yet."""
    if not sprite_key:
        return None
    return f"{settings.sprite_base_url.rstrip('/')}/{sprite_key.lstrip('/')}"
