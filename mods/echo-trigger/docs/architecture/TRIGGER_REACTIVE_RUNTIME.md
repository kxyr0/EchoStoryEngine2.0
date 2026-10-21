# Reactive Runtime Architecture

Reactive triggers are split into three layers:

- `trigger-contracts` owns neutral geometry, callback invocation, fire policy, stay policy, and debug-view records.
- `trigger-core` binds EchoScript AST into `ReactiveTriggerDefinition` and evaluates movement snapshots.
- `trigger-forge-1.20.1` captures server player movement and publishes public callback invocation events.

The runtime returns `TriggerCallbackInvocation` objects. It does not execute callback body actions directly. This keeps
Echo Trigger independent from Echo Dialogue, Story, Player, and other feature implementations.

## Movement Evaluation

The Forge bridge records one `PlayerMovementSnapshot` per player on server tick end. First observation is treated as a
baseline and does not synthesize enter or exit. Later snapshots include previous and current dimension, so dimension
changes can produce an exit for the old dimension and an enter for the new dimension.

For each compiled reactive definition, the core runtime tracks:

- current inside/outside membership by stable trigger instance id;
- total accepted callback count;
- one-shot callback kinds already consumed;
- cooldown expiry tick;
- next stay tick.

Accepted callbacks create or reuse the stable trigger instance id, then return a public invocation payload. Forge posts
that payload as `TriggerCallbackInvocationEvent` from `trigger-forge-api-1.20.1`.

## Debug Boundary

`TriggerAreaCatalogView` exposes read-only `TriggerAreaSnapshot` records for future echo-debug tooling. It includes
trigger id, area name, dimension, shape kind, min, max, and center. No debug GUI or wand behavior is implemented in
0.3.0.
