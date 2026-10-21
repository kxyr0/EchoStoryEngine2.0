# Echo Navigation

Echo Navigation 0.4.0 is the server-authoritative movement runtime for Echo agents.

Registered backends:

- `echo_navigation:vanilla` delegates path creation and walking to Minecraft 1.20.1 `PathNavigation`.
- `echo_navigation:astar` builds a surface-aware A* route and drives the NPC with Echo's own route motor.

Echo Navigation owns command identity, one active move per subject, backend pinning, arrival verification,
cancellation, stuck detection, settings, diagnostics and persistence snapshots.

Each request can now select `WALK` or `SPRINT`, set speed in blocks per second and state whether a presentation
adapter should synchronize animation playback to actual movement. The old request constructor remains supported.

Modules:

- `navigation-contracts` - Minecraft-free immutable IDs, requests, snapshots and diagnostics.
- `navigation-core` - Java-only lifecycle, idempotency, validation, progress, stuck rules and A* planning.
- `navigation-forge-api-1.20.1` - public Forge service, backend and agent resolver boundary.
- `navigation-vanilla-1.20.1` - vanilla backend plus Forge surface snapshot and A* route motor.
- `navigation-forge-1.20.1` - Forge mod host, commands, settings and SavedData integration.
- `navigation-testmod-1.20.1` - development-only testmod shell.

The NPC binding lives in `D:\JDHSeason\mods\echo-npc-navigation-adapter`, not in this repository.
