# Npc.StopInteraction

`Npc.StopInteraction(npc)` отменяет только interaction, принадлежащий этому NPC.
Если interaction уже запустил NavigationMove, adapter отменяет принадлежащий move.
NPC остаётся в текущей безопасной позиции.

```csharp
Npc.StopInteraction(Sasha);
```

Команда не отменяет unrelated movement/story/dialogue systems.

