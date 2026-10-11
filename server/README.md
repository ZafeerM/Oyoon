# server/

Python backend. **Open this folder in VS Code / PyCharm.** Built and run separately from `android/`. The two sides talk only over HTTPS, using `contracts/openapi.yaml`.

- `app/`: FastAPI service
- `worker/`: sprite pipeline
- `tests/`: pytest
- `requirements.txt`, `Dockerfile`, `docker-compose.yml` (api + worker + postgres + minio)
