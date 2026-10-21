# Facial animations

Canonical suffix: `.face-animation.es`.

Animation end behavior:

- `HOLD` keeps the final sample;
- `RETURN_TO_BASE` samples base at time after duration;
- `LOOP` wraps time by duration.

Interpolation:

- `STEP`;
- `LINEAR`.

A0 does not own per-Actor animation state. Sampling is stateless.

