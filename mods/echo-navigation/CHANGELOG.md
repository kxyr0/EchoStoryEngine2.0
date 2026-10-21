# Changelog

## 0.4.0

- Added a public route-only surface A* service for lightweight non-`Mob` consumers.
- The route-only service reuses the existing immutable snapshot and A* planner without starting a movement motor.

## 0.3.0

- Added movement gait, speed in blocks per second and optional presentation-speed synchronization to public requests
  and snapshots.
- Applied requested speed in both the surface A* route motor and vanilla `PathNavigation` backend.
- Preserved the previous request and snapshot constructors with legacy defaults.

## 0.2.3

- Prevented the A* backend from creating two-block jump edges across door columns so door routes keep their
  `OPEN_DOOR` traversal and the NPC opens doors instead of trying to jump at them.

## 0.2.2

- Synchronized NPC route motor body, head and entity yaw while moving so custom NPC models face the movement target
  instead of visually walking backward on some coordinate directions.

## 0.2.0

- Added `echo_navigation:astar`, a separate surface-aware A* backend with a custom NPC route motor.
- Added typed traversal edges for walk, step, stair, slab, ladder and door route segments.
- Added a collision-shape surface sampler that plans from quantized NPC feet positions instead of block centers.
- Added bounded A* search metrics and unit coverage for deterministic routing and search limits.
- Taught the vanilla backend to plan through hand-openable wooden doors and open route doors within one block of the NPC.
- Added a ladder-aware fallback block path and climb assist for vertical ladder segments when vanilla path creation cannot reach the target.

## 0.1.1

- Published public navigation lifecycle snapshots for debug integrations.

## 0.1.0

- Created the Echo Navigation multi-module repository.
- Added neutral movement contracts and a Java-only runtime core.
- Added the public Forge API for services, query access, backend providers and agent resolvers.
- Added `echo_navigation:vanilla` backed by Minecraft `PathNavigation`.
- Added Forge runtime host, settings, diagnostics, commands and SavedData snapshot writing.
- Added source-shape, whitespace and publication metadata checks.
- Added N1 architecture and authoring documentation.

Known N1 gaps:

- SavedData currently writes movement snapshots, but full decode and restart restoration are not complete.
- Dialogue, Trigger and Story now have adapter-backed `Npc.Move(...)` authoring paths through public integration APIs.
- Manual Minecraft GameTest acceptance for real path walking is still pending.
