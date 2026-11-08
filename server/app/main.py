"""Oyoon API entry point: uvicorn app.main:app"""

from fastapi import FastAPI

app = FastAPI(title="Oyoon API")


@app.get("/health")
def health() -> dict[str, str]:
    """Liveness check."""
    return {"status": "ok"}
