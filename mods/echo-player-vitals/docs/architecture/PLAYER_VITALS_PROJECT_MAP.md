# Project Map: Echo Player Vitals

## Модули

- `player-vitals-contracts` — neutral immutable contracts: ID, revision, fatigue, reserve, health, hunger, result.
- `player-vitals-core` — plain Java runtime: stage resolver, mutations, ledger, persistence codec, repository port.
- `player-vitals-forge-api-1.20.1` — public Forge API: service lookup, query, mutation, lifecycle event, client bridge.
- `player-vitals-forge-1.20.1` — Forge runtime: SavedData, commands, sync, movement, hunger compatibility.
- `player-vitals-testmod-1.20.1` — development-only module boundary.

## Gameplay flow

```text
Set/Add/Reduce/Restore fatigue
-> mutation idempotency
-> fatigue stage
-> movement multiplier
-> visual nausea value in snapshot
-> synchronized PlayerVitalsSnapshot
```

## Dependency flow

```text
Echo Player Vitals public API
-> Echo Player HUD Vitals Adapter
-> Echo Player HUD metric provider
```

HUD не владеет усталостью, hunger, замедлением или сохранением gameplay state.
