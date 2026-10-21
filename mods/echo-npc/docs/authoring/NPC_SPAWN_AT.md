# Npc.SpawnAt

`Npc.SpawnAt` ставит существующий durable NPC в авторскую позицию сцены. Команда принимает `NpcId`, позицию, абсолютный `yaw` и `pitch`; она не создает новый случайный ActorId и не дублирует уже materialized entity.

Пример:

```echoscript
Npc.SpawnAt(Sasha, SashaSpawn, yaw: TODO_SASHA_YAW, pitch: 0);
```

Если NPC уже materialized тем же scene command, возвращается идемпотентный результат. Если он уже materialized другой причиной, команда отклоняется диагностикой `NPC_SPAWN_ALREADY_MATERIALIZED`: `SpawnAt` не телепортирует существующего NPC молча.

Forge runtime выполняет save-before-dispatch: durable desired state сохраняется, затем server thread материализует entity и ждет scene-ready boundary. Командный аналог: `/echo_npc spawn_at <npc_id> <pos> <yaw> <pitch>`.
