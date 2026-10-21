# Opening scene

Opening setup выполняется до `Dialogue.Load`. Dialogue занимается репликами и cutscene, а не подготовкой тел игроков и NPC.

Целевой порядок:

```text
Player setup
-> Npc.SpawnAt
-> materialization
-> scene-ready
-> Npc.SleepAt
-> bed provider
-> sleeping attachment
-> Dialogue.Load
```

Шаблон EchoScript должен оставлять TODO для координат, yaw, item id книги и фактического block id кровати, если автор их еще не предоставил. Не угадывайте `SisterBedType`: `expectedBlock` защищает сцену от сломанной или замененной кровати.

Story adapter регистрирует N4 action ids `echo_npc:spawn_at`, `echo_npc:sleep_at` и `echo_npc:wake_up`; authoring слой отображает их как `Npc.SpawnAt`, `Npc.SleepAt` и `Npc.WakeUp`.
