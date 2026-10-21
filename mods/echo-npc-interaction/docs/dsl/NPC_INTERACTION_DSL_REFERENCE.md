# NPC Interaction DSL Reference

## 1. Назначение

`echo-npc-interaction` даёт сценаристу одну универсальную команду:
`Npc.Interact(npcId, position)`. Автор указывает NPC и точную позицию блока;
runtime сам проверяет target, выбирает provider, строит подход, вызывает Echo
Navigation, поворачивает NPC, выполняет server-authoritative interaction и только
после подтверждённого результата продолжает сценарий.

## 2. Команды

- `Npc.Interact(npc, x, y, z)`
- `Npc.Interact(npc, blockPosition)`
- `Npc.StopInteraction(npc)`
- `Npc.IsInteracting(npc)` запланирована как host predicate; runtime уже даёт
  public query `NpcInteractionQueryService.isInteracting(npc)`.

## 3. Сигнатуры

```csharp
Npc.Interact(NpcId npc, int x, int y, int z);
Npc.Interact(NpcId npc, BlockPosition position);
Npc.Interact(NpcId npc, BlockPosition position, face: AUTO, hand: MAIN_HAND);
Npc.StopInteraction(NpcId npc);
// query API: NpcInteractionQueryService.isInteracting(npc)
```

В Dialogue aliases разрешаются через `Alias("Sasha", "jdh:sasha")`, поэтому
форма `Npc.Interact(Sasha, 100, 64, 100)` проходит binder. В Story текущий binder
использует namespaced id literal: `Npc.Interact("jdh:sasha", 100, 64, 100)`.

## 4. Аргументы

- `npc`: durable NPC id или alias, если host поддерживает alias resolver.
- `x`, `y`, `z`: точная позиция блока.
- `BlockPosition`: `new BlockPosition(x, y, z)` или поле с таким initializer.
- `face`: `AUTO` по умолчанию.
- `hand`: `MAIN_HAND` по умолчанию.
- `approach`: `AUTO` по умолчанию.
- `dimension`: `minecraft:overworld` по умолчанию, если host не передал другое.

## 5. Defaults

```text
face = AUTO
hand = MAIN_HAND
approach = AUTO
dimension = minecraft:overworld
```

## 6. Blocking semantics

`Npc.Interact` является durable blocking command на уровне script host. Server
thread не блокируется. Story и Dialogue сохраняют ожидание, runtime исполняет
interaction async через ticks, а следующая строка сценария выполняется только
после terminal result. Trigger callback API сейчас dispatch-only, потому что у
него нет host continuation ledger.

## 7. Story пример

```csharp
private void OnStart()
{
    Npc.Interact("jdh:sasha", 100, 64, 100);
    Dialogue.Load("jdh:after_light_off");
}
```

## 8. Dialogue пример

```csharp
private BlockPosition TargetBlock = new BlockPosition(100, 64, 100);

private void Init()
{
    Id("jdh:sample");
    Start(Greeting);
    Alias("Sasha", "jdh:sasha");
}

private void Greeting()
{
    Line("Sasha", "Иду.");
    Npc.Interact(Sasha, TargetBlock);
    Line("Sasha", "Готово.");
}
```

## 9. Cutscene пример

Cutscene host передаёт generic owner и command position через тот же script
adapter. Команда остаётся той же:

```csharp
Npc.Interact(Sasha, 100, 64, 100);
```

## 10. Кнопка

```csharp
Npc.Interact(Sasha, 100, 64, 100);
```

Ожидаемый provider: `echo_npc_interaction:vanilla_block_use`.
Проверка результата: `BLOCK_STATE_CHANGED`.

## 11. Рычаг

Рычаг используется тем же generic provider. Receipt предотвращает повторное
переключение после restart, если receipt уже сохранён.

## 12. Дверь

Runtime не меняет block state напрямую. Он вызывает normal server block-use
pipeline, затем проверяет fingerprint до и после.

## 13. Стул

Стул требует specialized provider. Generic proxy не считается сидением NPC. Если
модовый стул посадил proxy, runtime очищает proxy attachment и возвращает typed
failure `NPC_INTERACTION_REQUIRES_SEAT_PROVIDER`.

## 14. Ошибки

Ошибки возвращаются как typed diagnostics и outcomes, например
`NPC_INTERACTION_TARGET_NOT_FOUND`, `NPC_INTERACTION_NO_APPROACH`,
`NPC_INTERACTION_PLAYER_BOUND_UNSUPPORTED`.

## 15. Persistence и restart

Команда сохраняет deterministic command id, execution id, selected approach,
navigation move id и receipt. Если receipt уже есть, runtime возвращает сохранённый
result и не выполняет use второй раз.

## 16. Provider compatibility

Совместимость имеет три уровня:

- `VANILLA_USE`: блок реагирует на обычный Minecraft use.
- `PROVIDER_INTERACTION`: нужен специализированный provider.
- `UNSUPPORTED_PLAYER_BOUND`: нужен настоящий player/menu/inventory/capability.

## 17. Ограничения модовых объектов

`InteractionResult.SUCCESS` не является доказательством результата. GUI, player
inventory mutation, advancements, player capability и client menu возвращают
typed failure, а не ложный успех.

## 18. Диагностические команды

```text
/echo_npc_interaction interact <npc_id> <x> <y> <z>
/echo_npc_interaction stop <npc_id>
/echo_npc_interaction inspect <npc_id>
/echo_npc_interaction target inspect <x> <y> <z>
/echo_npc_interaction providers
/echo_npc_interaction provider inspect <provider_id>
/echo_npc_interaction approaches <npc_id> <x> <y> <z>
/echo_npc_interaction diagnostics
/echo_npc_interaction trace <npc_id>
/echo_npc_interaction trace off
```

## 19. Типичные ошибки автора

- Указывать координаты рядом с объектом вместо точного блока target.
- Ждать, что неизвестный chair mod заработает без provider.
- Считать GUI block успешным interaction.
- Писать `Npc.Move` перед каждым `Npc.Interact`; автоматический подход уже встроен.

## 20. Migration notes

NI1 не добавляет обязательные `BlockId`, provider id, backend id, item id или
interaction type. Future commands вроде `Npc.InteractEntity` должны быть отдельной
typed surface и не расширять block target семантику.
# NI2: consequence transfer

`Npc.Interact(npcId, x, y, z)` по-прежнему указывает на сам target block. Автор не указывает Block ID, mod ID, provider ID или seat type.

Порядок выполнения:

```text
Npc.Interact
-> approach
-> Navigation
-> alignment
-> proxy normal block use
-> effect journal
-> consequence classifier
-> ride handoff или Echo seat anchor
-> verification
-> receipt v2
-> script resume
```

`NO_APPROACH` означает, что use ещё не выполнялся. Для диагностики используйте:

```text
/echo_npc_interaction approaches <npc_id> <x> <y> <z>
```

Стулья модов поддерживаются через observed behavior: если proxy после normal use начал ехать на entity, runtime пытается generic ride handoff. Отдельный Java-адаптер под Handcrafted не требуется.

`Npc.StandUp(npcId)` снимает NPC с созданного interaction seat anchor и освобождает occupancy.
