# Vanilla Sleep Interception

Interception реализован в `VanillaSleepInterception`.

Границы:

- `PlayerSleepInBedEvent`: фиксирует попытку сна, но не подтверждает успех;
- `TickEvent.ServerTickEvent`: подтверждает `ServerPlayer.isSleeping()` и sleeping pos;
- `SleepFinishedTimeEvent`: подавляет vanilla night skip при active session;
- `PlayerSleepBedProvider`: подтверждает canonical bed snapshot.

Клик по кровати не является доказательством сна. Scenario стартует только после sleeping state.
