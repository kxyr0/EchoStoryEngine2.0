# Echo NPC Interaction

Текущая версия NI2:

- `mods/echo-npc-interaction`, версия `0.2.0`;
- `mods/echo-npc-interaction-navigation-adapter`, версия `0.2.0`.

Главная команда DSL не изменилась:

```csharp
Npc.Interact(Sasha, 100, 64, 100);
Npc.Interact(Sasha, TargetBlock);
Npc.StopInteraction(Sasha);
Npc.StandUp(Sasha);
```

Координаты указывают на сам target block. Автор не указывает Block ID, mod ID, provider ID, seat entity type или navigation backend.

Платформенная цепочка NI2:

```text
EchoScript
-> NPC Interaction Script Adapter
-> NPC Interaction Runtime
-> Navigation Adapter
-> Echo Navigation
-> alignment
-> proxy normal block use
-> effect journal
-> consequence classifier
-> generic ride handoff
-> Echo seat anchor fallback
-> receipt v2
-> EchoScript resume
```

Handcrafted и другие мебельные моды не становятся compile/runtime dependency production-кода. Стул определяется по факту `PROXY_STARTED_RIDING`, после чего runtime переносит сидение на настоящего NPC или возвращает typed failure.
