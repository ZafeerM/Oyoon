# Tracking checklist (S2-QA-1)

Manual check of the live measurement screen (S2-AND-2) and its validity gate (S2-CORE-2).
Run it on a real phone with a **debug build** so the debug text shows angles, iris px and
brightness. File a GitHub issue for every row where the actual chip does not match the expected one.

## Before you start

- Build: debug APK from the commit under test (write the commit hash below).
- Analysis resolution: pick one in the bottom bar and keep it for the whole run (write it below).
  The resolution changes iris pixel size, which decides TOO_FAR (see note at the end).
- Hold the phone in portrait, front camera, at eye height unless the row says otherwise.
- After changing position, wait about 1 second: the gate needs 15 steady frames before it
  accepts (until then the chip shows "Hold still").
- Gate limits under test are in `docs/decisions.md` (yaw/pitch ≤ 10°, roll ≤ 5°, iris ≥ 20 px,
  mismatch ≤ 10%, variation ≤ 2% over 15 frames, brightness ≥ 60).

## Chip texts

| Gate result | Chip |
|---|---|
| Accepted | green "Measuring…" |
| No face | grey "Looking for your face" |
| YAW | red "Turn to face the camera" |
| PITCH | red "Keep your head level" |
| ROLL | red "Don't tilt your head" |
| TOO_FAR | red "Move closer" |
| IRIS_MISMATCH | red "Keep both eyes visible" |
| UNSTABLE | red "Hold still" |
| TOO_DARK | red "Find more light" |

## Run details

| Field | Value |
|---|---|
| Date | |
| Tester | |
| Phone model | |
| Android version | |
| Commit | |
| Analysis resolution (asked / got) | |
| Delegate (GPU/CPU) | |

## Checklist

Unless the row says otherwise: bright room, no glasses, looking straight at the camera, about 30 cm away.

| # | Condition | How | Expected chip | Actual chip | Brightness / iris px / angles from debug text | Pass? | Issue |
|---|---|---|---|---|---|---|---|
| 1 | Bright room | Daylight or full room light, light in front of you | green "Measuring…"; PD stays within about ±1 mm | | | | |
| 2 | Dim room | One small lamp, room lights off | red "Find more light" if brightness < 60, otherwise green | | | | |
| 3 | Window behind the person | Stand with a bright window behind your head | red "Find more light" (face is backlit and dark) | | | | |
| 4 | Without glasses | Bright room, no glasses | green "Measuring…" | | | | |
| 5 | With glasses | Bright room, wearing glasses | green "Measuring…" (note any reflections, and any red "Keep both eyes visible") | | | | |
| 6 | 30 cm away | Phone about 30 cm from your face (measure it) | green "Measuring…" if iris ≥ 20 px, otherwise red "Move closer" | | | | |
| 7 | 60 cm away | Phone about 60 cm from your face (measure it) | red "Move closer" expected at most resolutions (see note) | | | | |
| 8 | Head turned left | Turn your head about 20° to the left | red "Turn to face the camera" | | | | |
| 9 | Head turned right | Turn your head about 20° to the right | red "Turn to face the camera" | | | | |
| 10 | Head tilted up | Tilt your chin up about 20° | red "Keep your head level" | | | | |
| 11 | Head tilted down | Tilt your chin down about 20° | red "Keep your head level" | | | | |
| 12 | Back to straight | After row 8–11, look straight and hold still | red "Hold still" for about 1 s, then green "Measuring…" | | | | |

For rows 8–11 also check that **only the matching angle** changes in the debug text (left/right:
yaw; up/down: pitch). If pitch and yaw look swapped, note it: the guide says to swap them and record
it in `docs/decisions.md`.

## Note on TOO_FAR and resolution

The iris is about 11.7 mm wide. Rough estimate for a typical phone front camera in portrait:
at 30 cm the iris is roughly 12 px wide at 640×480, 19 px at 1280×720 and 28 px at 1920×1080; at
60 cm it is about half that. So with the 20 px limit, 640×480 may say "Move closer" even at 30 cm.
Record the iris px from the debug text in every row: the numbers feed the resolution decision in
`docs/decisions.md` and the final TOO_FAR limit.

## Results summary

| Rows passed | Rows failed | Issues filed |
|---|---|---|
| | | |
