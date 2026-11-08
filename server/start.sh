#!/bin/sh
# Container entry point: bring the database schema up to date, then serve the API.
set -e
alembic upgrade head
exec uvicorn app.main:app --host 0.0.0.0 --port 8000
