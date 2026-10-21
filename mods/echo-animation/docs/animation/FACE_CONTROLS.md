# Face controls

A0 includes normalized control math for eye closure and gaze bounds.

`BlinkControl("eye_closure", upperTravel: 1000, lowerTravel: 1000)` stores character-specific calibration in the FaceRig. Control values are clamped to `0..1` before travel is applied.

High-level `.face-rig.es` resources can use `Blink "eye_closure"` with
`PeriodTicks(...)`, `CloseTicks(...)`, `HoldTicks(...)`, and `OpenTicks(...)`
to tune idle blink frequency and speed.
