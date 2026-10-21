# Item release and recovery

`ReleaseFlow` supports `HAND_MOTION`, `LOOK_DIRECTION`, and `LOCAL_VECTOR`. Hand motion needs
at least two recent authoritative socket samples and is clamped. Before spawning, the flow
persists a `ReleasePlan` containing dimension, exact anchor position, velocity, and a unique
`ReleaseId`. `ReleaseSpawner` writes that ID to the new `ItemEntity` persistent data.

After the world accepts the entity, custody becomes `WORLD_RELEASED`. If the server stops
between intent and confirmation, `AttachmentRecovery` performs direct entity lookup using
the deterministic UUID derived from `ReleaseId`. It confirms that witness or spawns exactly one from the durable plan;
it never creates a second witness. A reserved take with its source still present is abandoned
safely, while the explicit `RECOVERING` count witness proves whether a split completed.

Player disconnect does not discard custody. Player death invokes the normal drop path with a
system death owner. Missing witnesses, duplicate witnesses, unavailable dimensions, and
invalid states are logged with stable `EIA-*` diagnostic codes instead of silently cloning.
