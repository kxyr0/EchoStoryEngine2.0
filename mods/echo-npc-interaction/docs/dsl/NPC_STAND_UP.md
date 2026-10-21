# Npc.StandUp

`Npc.StandUp(npcId)` освобождает NPC из seat occupancy, созданной `Npc.Interact`.

Команда immediate: она не запускает pathfinding и не выполняет block use. Если NPC сидит на `EchoNpcSeatAnchorEntity`, runtime снимает NPC с anchor, удаляет anchor и освобождает occupancy.

Пример:

```csharp
Npc.Interact(Molly, ChairPosition);
Npc.StandUp(Molly);
```

Если NPC уже стоит, команда завершается успешно и ничего не меняет.
