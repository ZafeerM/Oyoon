# :app

The Android app (owned by the Android devs). Camera, UI, networking, and calls into `:core` for all maths.

Depends on `:core`. Measurement, gating, filtering or fit logic belongs in `:core`, not here.

## What it does now

- Front camera preview (CameraX), with each frame sent to MediaPipe Face Landmarker.
- Landmarker settings: `setNumFaces(1)`, `RunningMode.LIVE_STREAM`, `setOutputFacialTransformationMatrixes(true)`.
  Blendshapes are off.
- Tries the GPU delegate first and falls back to CPU if GPU cannot start.
- Draws the 478-point face mesh over the preview. The label at the top left shows the delegate and per-frame time in ms.

## Layout

- `src/main/kotlin/MainActivity.kt`: camera permission, CameraX preview + analysis.
- `src/main/kotlin/landmarker/FaceLandmarkerHelper.kt`: creates and runs the landmarker.
- `src/main/kotlin/landmarker/OverlayView.kt`: draws the mesh.
- `src/main/assets/face_landmarker.task`: model, committed so builds need no download step.

Some code is adapted from the MediaPipe samples (Apache 2.0). See [NOTICE.md](NOTICE.md).
