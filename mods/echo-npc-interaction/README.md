# Echo NPC Interaction

Версия: `0.1.0`.

Репозиторий добавляет узкий runtime для `Npc.Interact`: точная block target позиция,
выбор provider, подход через Echo Navigation, поворот NPC, server-side use pipeline,
проверка результата, receipt и script resume после подтверждения.

Основной DSL:

```csharp
Npc.Interact(Sasha, 100, 64, 100);
Npc.Interact(Sasha, TargetBlock);
Npc.StopInteraction(Sasha);
```

Код разделён на neutral contracts, plain Java core, Forge public API, Forge runtime,
EchoScript adapter и development testmod. Документация: `docs/dsl`,
`docs/architecture`, `docs/diagnostics`.

