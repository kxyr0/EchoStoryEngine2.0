# NPC scene placement

Scene placement строится поверх public `NpcMaterializationService` и durable `ActorId`. `Npc.SpawnAt` сохраняет desired state до materialization и не считает создание Java entity достаточным доказательством scene-ready.

```text
NpcId
-> durable actor record
-> spawn position validation
-> repository save
-> Forge mutation queue
-> materializer
-> runtime snapshot
-> scene-ready result
```

Проверка позиции выполняется в `forge/spawn/NpcSpawnPositionValidator`: dimension, bounds, loaded chunk и collision для тела NPC. Невалидная позиция возвращает `NPC_SPAWN_POSITION_INVALID` или более конкретный код вроде `NPC_SPAWN_COLLISION`.

`SpawnAt` не заменяет будущий `PlaceAt`: уже живущий NPC не переставляется молча.
