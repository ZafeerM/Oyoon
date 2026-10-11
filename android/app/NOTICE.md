# Third-party notices for `:app`

## MediaPipe face landmarker sample

Parts of this module are adapted from
[google-ai-edge/mediapipe-samples](https://github.com/google-ai-edge/mediapipe-samples),
`examples/face_landmarker/android`.

Copyright 2023 The TensorFlow Authors. Licensed under the Apache License, Version 2.0
(http://www.apache.org/licenses/LICENSE-2.0).

Adapted files (each keeps the original licence header plus a note describing what changed):

- `src/main/kotlin/MainActivity.kt` (camera setup from `CameraFragment.kt`)
- `src/main/kotlin/landmarker/FaceLandmarkerHelper.kt`
- `src/main/kotlin/landmarker/OverlayView.kt`

## Face landmarker model

`src/main/assets/face_landmarker.task` is the MediaPipe Face Landmarker model (float16, v1), from
https://storage.googleapis.com/mediapipe-models/face_landmarker/face_landmarker/float16/1/face_landmarker.task

SHA-256: `64184e229b263107bc2b804c6625db1341ff2bb731874b0bcc2fe6544e0bc9ff`

Licensed under the Apache License, Version 2.0 (see the MediaPipe Face Mesh V2 model card).
