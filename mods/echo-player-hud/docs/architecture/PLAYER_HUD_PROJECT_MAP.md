# Project Map: Echo Player HUD

## Модули

- `player-hud-contracts` — neutral contracts: metric IDs, snapshots, layout primitives, gradients, diagnostics.
- `player-hud-core` — registry, default layout, reference-canvas transform, bar fill, gradient compiler.
- `player-hud-forge-api-1.20.1` — public Forge API для provider registration и service lookup.
- `player-hud-forge-1.20.1` — client renderer, vanilla HUD suppressor, commands.
- `player-hud-testmod-1.20.1` — development-only module boundary.

## HUD flow

```text
Health provider
Spark provider
Fatigue provider
-> HudMetricSnapshot
-> HudLayoutSnapshot
-> framebuffer transform
-> bars
```

## Dependency flow

```text
Echo Content Player
-> HUD Content Player Adapter
-> Echo Player HUD

Echo Player Vitals
-> HUD Vitals Adapter
-> Echo Player HUD
```

HUD не владеет gameplay state.
