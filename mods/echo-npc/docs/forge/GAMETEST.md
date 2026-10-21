# GameTest

GameTests запускаются задачей:

```powershell
.\gradlew.bat :npc-forge-1.20.1:runGameTestServer --console=plain
```

## Покрытие

GameTest suite проверяет:

- host start;
- `.npc.es` compile;
- catalog install;
- invalid reload keeps old catalog;
- spawn queues durable operation and materializes one entity after save;
- entity syncs `ActorId`;
- entity syncs definition presentation data;
- duplicate spawn does not create second entity;
- `ActorId` conflict rejected;
- two actors can use one definition;
- teleport queues durable operation and then moves entity;
- despawn queues tombstone operation and then removes entity;
- repeated despawn is idempotent;
- service lookup resolves host;
- local host restart recovers durable actor record;
- shutdown clears actors;
- dedicated server does not load renderer;
- actor pins spawned definition;
- missing skin does not block server spawn;
- entity has no AI goals.

GameTests avoid wall-clock sleeps. Async checks use tick-delayed phases or
`succeedWhen` polling and always filter by exact actor id, because N2 may recover
other durable test actors in the same world.

Build task verifies that all required `ENPC-GAMETEST-RUN` markers were emitted
and that the server log reports:

```text
Echo NPC GameTests verified: discovered=21, executed=21, passed=21
```
