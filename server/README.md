# server/

Python backend. **Open this folder in VS Code / PyCharm.** Built and run separately from `android/`.
The two sides talk only over HTTPS, using `contracts/openapi.yaml`.

- `app/`: FastAPI service (`main.py`, `api/`, `models/`, `db/`, `storage/`, `seed.py`)
- `migrations/`: Alembic migrations (applied automatically when the api container starts)
- `worker/`: sprite pipeline (not built yet)
- `tests/`: pytest
- `requirements.txt`, `Dockerfile`, `docker-compose.yml` (api + postgres for now)

## Run with Docker

Needs Docker Desktop. From this `server/` folder:

```bash
docker compose up --build
```

The api container runs `alembic upgrade head` before starting, so all tables exist with no manual
steps. Load the catalogue and tolerance bands from `contracts/` (safe to run again):

```bash
docker compose exec api python -m app.seed
```

```bash
curl localhost:8000/frames
```

## Run tests locally

Tests use an in-memory SQLite database, so they need no Docker or Postgres:

```bash
py -m venv .venv
```

```bash
.venv/Scripts/python -m pip install -r requirements.txt
```

```bash
.venv/Scripts/python -m pytest
```

(On macOS/Linux use `python3` and `.venv/bin/python`.)

## Endpoints (guide section 4.6)

- `GET /health`: liveness check
- `GET /frames`: all approved frames, with `sprite_url` built from `sprite_key`
- `GET /frames/{id}`: one approved frame, or 404
- `GET /tolerance-bands`: contents of `contracts/tolerance-bands.json`

## Settings (environment variables)

- `DATABASE_URL`: SQLAlchemy URL (default: local Postgres)
- `SPRITE_BASE_URL`: public base URL of the sprites bucket
- `CONTRACTS_DIR`: folder with the `contracts/` files used by `app.seed`
