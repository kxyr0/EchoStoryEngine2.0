# ADR-0013: Clip Completion And Graph Pinning

## Decision

Clip completion is separate from state completion. Existing subjects remain
pinned to their graph revision across reload.

## Consequences

- `HOLD_LAST_FRAME` stays in state after clip end.
- `WAIT_FOR_EXIT` needs an explicit event.
- Reload may install a newer graph revision for new subjects while old subjects
  retain their pinned revision.
