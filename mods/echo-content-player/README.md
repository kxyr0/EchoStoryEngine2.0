# Echo Content Player

Echo Content Player stores the durable player profile used by the Echo platform: stable player identity, Echo display name, Spark, base appearance, profile revisions, lifecycle metadata, default template provenance, and mutation idempotency.

The module is intentionally narrow. It does not own story state, dialogue state, triggers, inventory, equipment, wardrobe layers, RPG stats, reputation, currencies, achievements, or UI settings.

## Modules

- `player-contracts` contains immutable public Java contracts without Minecraft, Forge, or EchoScript implementation imports.
- `player-core` contains plain Java profile creation, validation, mutation, persistence DTOs, recovery, skin validation, and EchoScript template binding.
- `player-forge-api-1.20.1` exposes the stable services that other Forge-side Echo modules may consume.
- `player-forge-1.20.1` is the Forge runtime host and production artifact.
- `player-testmod-1.20.1` contains development-only manual acceptance hooks and is not embedded into the production jar.
