# PlayerSleepSession

Session создаётся через `PlayerSleepSessionFactory` после подтверждённого vanilla sleep.

Snapshot содержит:

- session id;
- scenario id;
- player ref;
- owner;
- bed snapshot;
- origin snapshot;
- status;
- revision;
- dream destination;
- return/recovery/control modes;
- started server tick.

В snapshot не сохраняются `ServerPlayer`, `Level`, `BlockState`, screen, shader или callbacks.
