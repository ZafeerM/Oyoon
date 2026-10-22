# Decisions

Every decision, newest first, with its date and reason.

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
