# Decisions

Every decision, newest first, with its date and reason.

---

## 2026-11-08 — Backend stack details (S2-BE-1, S2-BE-2)

**Decisions:**

- **No object storage in docker-compose yet.** MinIO no longer publishes Docker images
  (`minio/minio` is gone from Docker Hub). Nothing in Sprint 2 uses storage, so compose runs only
  `api` and `postgres`. An S3-compatible store is chosen with the vendor-upload / worker task.
- **No worker service yet.** It is added with the sprite-pipeline task.
- **Frame sizes:** `lens_width_mm`, `bridge_mm`, `temple_length_mm` are integers (printed on the
  arm); `lens_height_mm` and `total_width_mm` are decimals (measured with a ruler).
- **`GET /frames` and `GET /frames/{id}` only return frames with `approved = true`.**
- **`sprite_url` = `SPRITE_BASE_URL` + `/` + `sprite_key`**, or null when there is no sprite.
- **Seeding is manual** (`python -m app.seed`), not run at container start, so fake or test data
  never lands in a database by accident. Migrations do run at start.
- **Placeholder data:** `contracts/fixtures/catalogue.json` holds 3 rows named "SAMPLE … replace
  with a real frame" and `contracts/tolerance-bands.json` 2 rows with source "PLACEHOLDER". QA and
  Backend replace them with 15–20 real frames and real bands.
- **Tests run on SQLite** (JSONB falls back to JSON), so they need no Docker; `docker compose up`
  is the Postgres check.

**Reason:** Keep Sprint 2 to what the tasks need, and avoid shipping an unmaintained image.

---

## 2026-11-08 — Validity gate start values and rules (S2-CORE-2)

**Decision:** `GateConfig` defaults (`android/core/src/main/kotlin/gate/Gate.kt`):

| Check | Start limit | Source |
|---|---|---|
| Yaw | \|yaw\| ≤ 10° | Guide 4.2 |
| Pitch | \|pitch\| ≤ 10° | Guide 4.2 |
| Roll | \|roll\| ≤ 5° | Guide 4.2 |
| Too far | mean iris diameter ≥ 20 px | Guide 4.2 (revisit with the resolution test) |
| Iris mismatch | \|A − B\| / larger ≤ 10% | Guide 4.2; divisor chosen by team |
| Unstable | (max − min) / mean of mean iris diameter over last 15 frames ≤ 2% | Guide 4.2; formula chosen by team |
| Too dark | mean face luma ≥ 60 (0–255) | Team guess; guide gives no number |

Rules beyond the guide:

- Checks run in the order YAW, PITCH, ROLL, TOO_FAR, IRIS_MISMATCH, UNSTABLE, TOO_DARK; the first
  failure is reported.
- Every frame with a face adds its mean iris diameter to the stability history, including
  rejected ones, so head movement also breaks stability.
- Until 15 frames of history exist, frames are rejected as UNSTABLE (no PD from too little data).
- If a frame has no brightness value, the TOO_DARK check is skipped.
- Yaw/pitch/roll are computed in `:core` (`HeadPose.fromMatrix`) from the raw matrix, not in the
  app, so the formula is unit-tested.

**Reason:** Start values to get going, not from the literature. Final values come from QA's data
in Sprint 3 and replace this entry.

---

## 2026-10-11 — Camera analysis resolution (OPEN, measurements pending)

**Question:** Which CameraX analysis resolution to send to MediaPipe Face Landmarker.
A higher resolution gives more pixels across the iris (better scale precision) but lowers FPS.

**How to measure** (debug build, bottom bar):

1. Hold the phone at arm's length, face centred, even light.
2. Tap a resolution, wait ~5 s for the FPS to settle.
3. Write down the values from the top-left label: `fps`, `got` (actual size), iris `mean` px.
4. Repeat for each resolution.

Iris width = edge-to-edge distance between iris landmarks 469↔471 and 474↔476, in pixels of
the analysis image (not screen pixels). FPS = landmarker results finished per second.

| Phone model | Asked | Got | Delegate | FPS | Iris px (468) | Iris px (473) | Iris px (mean) |
|---|---|---|---|---|---|---|---|
| | 640×480 | | | | | | |
| | 1280×720 | | | | | | |
| | 1920×1080 | | | | | | |

**Decision:** _to fill in once measured._

**Reason:** _to fill in once measured._
