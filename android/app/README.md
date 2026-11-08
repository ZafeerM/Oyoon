# :app

The Android app (owned by the Android devs). Camera, UI, and calls into `:core` for all maths.

Depends on `:core`. Measurement, gating, filtering or fit logic belongs in `:core`, not here.

## What it does now

- Front camera preview (CameraX); each frame goes to MediaPipe Face Landmarker
  (`setNumFaces(1)`, `RunningMode.LIVE_STREAM`, `setOutputFacialTransformationMatrixes(true)`,
  GPU with CPU fallback).
- Each result becomes a `:core` `FrameSample` (pixel landmarks, raw transformation matrix, face
  brightness) and goes through `:core`'s `gate()`.
- Compose screen: status chip (green "Measuring…" when accepted, red with the reason when rejected,
  grey when no face), and live PD = median of the last 15 accepted frames. PD only changes on
  accepted frames.
- Logcat tag `OyoonFrame`: one line per frame with iris sizes, angles, brightness, gate result, PD.

## Layout

- `src/main/kotlin/MainActivity.kt`: permission, CameraX binding, landmarker lifecycle, UI state.
- `src/main/kotlin/landmarker/FaceLandmarkerHelper.kt`: creates and runs the landmarker.
- `src/main/kotlin/landmarker/FrameSampler.kt`: MediaPipe result to `FrameSample` (+ brightness).
- `src/main/kotlin/live/LiveMeasurer.kt`: gate state, live PD, Logcat line.
- `src/main/kotlin/ui/`: `MeasureScreen.kt` (screen), `FaceOverlay.kt` (mesh), `MeasureUiState.kt`.
- `src/main/assets/face_landmarker.task`: model, committed so builds need no download step.

## Debug tools (debug builds only)

- Text: delegate, per-frame ms, processed FPS; asked vs actual analysis resolution; iris A/B/mean px;
  yaw/pitch/roll and brightness.
- Bottom bar: analysis resolution (640×480 / 1280×720 / 1920×1080) and landmark index labels
  (468–477, 168, 234, 454). Changing resolution resets the gate history and PD.
- The face mesh with highlighted irises is drawn in all builds.

Some code is adapted from the MediaPipe samples (Apache 2.0). See [NOTICE.md](NOTICE.md).
