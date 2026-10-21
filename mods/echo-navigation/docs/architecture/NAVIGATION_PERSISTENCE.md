# Navigation Persistence

N1 persistence principle: save before dispatch.

Implemented now:

- accepted movement snapshots are encoded into Forge `SavedData`;
- movement IDs, command IDs, owner IDs, subject IDs, destination, status, backend and progress are written;
- command idempotency exists in `navigation-core`;
- active moves are pinned to their selected backend ID.

Incomplete N1 recovery:

- decoding saved snapshot strings back into live runtime records is not wired yet;
- backend recovery after server restart is only represented by the public API shape;
- Story result delivery after restart still depends on the Story external command recovery path and adapter session state.

This document is intentionally strict: persisted bytes alone are not proof that restart recovery works.
