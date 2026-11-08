"""Oyoon API entry point: uvicorn app.main:app"""

from fastapi import FastAPI

from app.api.catalogue import router as catalogue_router

app = FastAPI(title="Oyoon API")
app.include_router(catalogue_router)


@app.get("/health")
def health() -> dict[str, str]:
    """Liveness check."""
    return {"status": "ok"}
