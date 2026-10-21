# Runtime усталости

Поток gameplay-состояния:

```text
fatigue mutation
-> PlayerFatigue
-> PlayerFatigueStageResolver
-> PlayerFatigueSnapshot
-> server movement modifier
-> client snapshot
```

`player-vitals-core` не импортирует Minecraft или Forge. Он содержит только модель усталости, стадии, мутации,
idempotency ledger, persistence DTO и диагностику.

`player-vitals-forge-1.20.1` владеет интеграцией с Minecraft:

- `SavedDataPlayerVitalsRepository` сохраняет fatigue и revision;
- `PlayerFatigueMovementEffect` применяет один transient modifier `echo_player_vitals:fatigue_movement`;
- `PlayerHungerNeutralizer` удерживает hunger compatibility state;
- `PlayerVitalsSyncChannel` отправляет immutable snapshots клиенту;
- `/echo_vitals` показывает live-снимок и выполняет публичные мутации.

Modifier не добавляется каждый tick заново. При `RESTED` source-owned modifier снимается; при `TIRED` и `EXHAUSTED`
заменяется только если изменился multiplier.
