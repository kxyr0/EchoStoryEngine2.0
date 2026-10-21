# Seat Anchor Resolution

`ForgeSeatAnchorResolver` resolves a generic riding vehicle into `SeatAnchor`.

SE1 implemented source priority:

1. vehicle block position;
2. vehicle world position as passenger attachment approximation;
3. named slot matching inside `MatchRadius`.

No furniture class imports, reflection, BlockId classification or mod ID checks are used.
