# Version audit

Проверена фактически установленная сборка PrismLauncher `Форма Мысли`:

- Minecraft `1.20.1`;
- Forge `47.4.10`;
- `sophisticatedbackpacks-1.20.1-3.24.62.2017.jar`;
- `sophisticatedcore-1.20.1-1.3.73.2192.jar`;
- отдельный Curios JAR отсутствует и адаптер от него не зависит.

По локальному bytecode (`javap`, без предположений по другой версии) подтверждено:

- `BackpackBlockEntity.getBackpackWrapper()` и `setBackpack(ItemStack)`;
- `IBackpackWrapper.getBackpack()` и `getContentsUuid()`;
- UUID хранится в item tag `contentsUuid`;
- `BackpackStorage.getOrCreateBackpackContents(UUID)` является глобальным
  хранилищем содержимого;
- `BackpackItem.tryPlace(Player, Direction, BlockPlaceContext)` выполняет
  официальный tier-specific placement lifecycle;
- `BackpackItem.getMainColor/getAccentColor` читают настоящие цвета.

`cloneBackpack()` намеренно запрещён в production code: его bytecode удаляет
старый `contentsUuid`, создаёт новый UUID и копирует storage data. Для exact
identity используется только `getBackpack().copy()`.

Адаптер компилируется через deobfuscated flat-directory dependencies именно на
эти два локальных JAR. Forge metadata объявляет оба мода обязательными, поэтому
при их удалении загрузчик выдаёт обычную missing-dependency ошибку до запуска
кода, а не поздний `ClassNotFoundException`.
