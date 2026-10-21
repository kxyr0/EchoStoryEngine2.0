# Navigation Failures

Movement failures are explicit and typed.

Important failures:

- `NPC_MOVE_DESTINATION_INVALID` - malformed request or cross-dimension target.
- `NPC_MOVE_DESTINATION_UNLOADED` - destination chunk is not loaded.
- `NPC_MOVE_DESTINATION_NOT_STANDABLE` - feet/head space or floor is invalid.
- `NPC_MOVE_PATH_NOT_FOUND` - backend cannot produce or keep a valid path.
- `NPC_MOVE_STUCK` - progress stayed below the configured threshold after bounded replans.
- `NPC_MOVE_TIMEOUT` - movement exceeded its max duration.
- `NPC_MOVE_CANCELLED` - owner or command cancelled the movement.
- `NAVIGATION_BACKEND_UNAVAILABLE` - selected or pinned backend is missing.
- `NAVIGATION_VANILLA_START_REJECTED` - `PathNavigation.moveTo` rejected the path.
- `NAVIGATION_ASTAR_SEARCH_LIMIT_REACHED` - bounded A* stopped before exhausting the world.
- `NAVIGATION_ASTAR_ROUTE_INVALIDATED` - A* route state was missing or stale.

Failure policy:

- failed movement does not resume script as success;
- diagnostics include movement, subject and backend facts;
- no teleport fallback is performed;
- active move remains pinned to the backend selected at start.
