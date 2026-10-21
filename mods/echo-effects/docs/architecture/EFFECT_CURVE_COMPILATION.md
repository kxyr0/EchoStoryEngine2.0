# Effect Curve Compilation

Curves are compiled once on reload or source bind, not sampled from source every tick.

Validation rejects:

- empty curves;
- negative key times;
- duplicate key times;
- unsorted keys;
- non-finite values.

Sampling is deterministic and supports `STEP`, `LINEAR`, `SMOOTH`, and `CUBIC`.
