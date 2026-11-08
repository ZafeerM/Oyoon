# model/

Core data types: `FrameSpec`, `Snapshot`, `Measurements`, `FitResult`.

Serialisable types must match `contracts/snapshot.schema.json`.

Now: `Landmarks` (all face-mesh indices), `FrameSample` (one frame's landmarks in pixels),
`Snapshot` (list of frames), `Measurements` (mm values).
