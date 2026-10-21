# Player Actor project map

## Command flow

EchoScript → Player Actor Script Adapter → `PlayerActorService` → control lease validation → body operation → `PlayerActorResult` → script resume.

## Move flow

`Player.Move` → `PlayerActorCommandGateway` → optional movement bridge → `PlayerActorMotor` → `ServerPlayer` movement → arrival/stop result.

## Ownership

- profile: `echo-content-player`;
- appearance/outfit: `echo-wardrobe`;
- body: `echo-player-actor`;
- control lease: `PlayerControlLeaseRegistry`;
- camera: `echo-cutscene`;
- route preflight: `echo-navigation`;
- Dialogue state: `echo-dialogue`.

## Modules

- `player-actor-contracts`: immutable API records/enums.
- `player-actor-core`: rotation, lease, interaction planning and idempotency.
- `player-actor-forge-api-1.20.1`: public Forge services/events.
- `player-actor-forge-1.20.1`: runtime on true `ServerPlayer`.
- `player-actor-script-adapter-1.20.1`: `Player.*` command names and payload binder.
- `player-actor-testmod-1.20.1`: development-only fixtures.
