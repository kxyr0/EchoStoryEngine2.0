# Vanilla Sleep Interception Audit

Forge 1.20.1 boundaries used by PS1:

- `net.minecraftforge.event.entity.player.PlayerSleepInBedEvent`
- `net.minecraftforge.event.entity.player.PlayerWakeUpEvent`
- `net.minecraftforge.event.level.SleepFinishedTimeEvent`
- `net.minecraftforge.event.TickEvent.ServerTickEvent`

Local evidence:

- `echo-trigger` already uses `PlayerSleepInBedEvent` and `PlayerWakeUpEvent` for bed enter/leave trigger events.
- Forge changelog present in the workspace documents `ServerTickEvent` server access for this loader family.
- Compilation against Forge `1.20.1-47.4.10` succeeded with these boundaries.

Decision:

- No reflection.
- No broad Mixin.
- `PlayerSleepInBedEvent` is treated as attempt only.
- Real confirmation is delayed to server tick and requires `ServerPlayer.isSleeping()` plus provider-confirmed bed position.
- Vanilla night skip is intercepted only while at least one Echo Player Sleep session is active.
