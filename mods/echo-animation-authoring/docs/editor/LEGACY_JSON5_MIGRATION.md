# Legacy JSON5 migration

JSON5 is migration input only. The production content pack must receive generated `.face-animation.es` files, not JSON5.

The A0 migration maps:

- `lengthSeconds` to `durationMillis`;
- `end: "hold"` to `HOLD`;
- `move.x` and `move.y` to local translation in thousandths;
- eyelid `to` values to `y` translation in thousandths.

Warnings are emitted for:

- `tilt`, because old rotation axis semantics were not proven;
- `blinkTravelY`, because character calibration requires author confirmation.

