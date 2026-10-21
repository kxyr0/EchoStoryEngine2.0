# Предметы и инвентарь

`HasItem("minecraft:diamond")` проверяет hotbar, main inventory и offhand. Armor, Ender Chest, открытый container,
cursor stack и выброшенные items не входят в scope.

Item matcher бывает двух видов:

- Item ID: `"minecraft:diamond"`.
- Item Tag: `"#minecraft:planks"`.

Echo Item Pack не нужен как dependency: любой `jdh:*` предмет виден как обычный Forge registry item.

Predicates: `HasItem`, `ItemCountAtLeast`, `ItemCountAtMost`, `ItemCountExactly`, `HasEmptyInventorySlot`.

Events: `ItemAdded`, `ItemRemoved`, `ItemPickedUp`, `ItemDropped`, `ItemUsed`, `ItemConsumed`,
`InventoryBecameFull`, `InventorySlotFreed`.

`HasItem(Key, count: 3)` означает current count >= 3. `ItemAdded(Key, count: 3)` означает delta >= 3 после подписки.
После recovery `HasItem` может стать true по текущему snapshot, но `ItemAdded` не создаёт synthetic event.

```csharp
Trigger.Wait(HasItem("minecraft:diamond", count: 3));
Trigger.Wait(ItemAdded("#minecraft:wool"));
```
