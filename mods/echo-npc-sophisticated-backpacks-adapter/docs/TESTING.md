# Testing and acceptance

Автоматическая baseline-команда:

```powershell
.\gradlew.bat check build --console=plain
```

Unit scope включает marker ordering/duplicates, reservation conflicts,
recovery decisions, ownership и command idempotency. Forge compile проверяет
реальные signatures установленной версии Sophisticated Backpacks, публичные
Echo API, client renderer и optional marker bridge. Production JAR scan не
должен включать testmod или sibling Echo/Sophisticated classes.

Интеграционный тест с реальными Forge и Sophisticated Backpacks запускается
отдельно:

```powershell
.\gradlew.bat :npc-backpack-testmod-1.20.1:runGameTestServer --console=plain
```

Задача требует серверный маркер `All 1 required tests passed` и проверяет
нормализованный block → item snapshot → remove → official placement round-trip:
тот же `contentsUuid`, полный payload fingerprint, main/accent colors и запись
`BackpackStorage`.

## Manual compatibility matrix

Каждый кейс проверяется equip → save/restart → unequip → повторная проверка:

- empty backpack каждого tier;
- contents в разных slots и stack sizes;
- inventory/feeding/tank/battery и другие upgrades;
- main/accent dye colors;
- display item;
- nested backpack с собственным UUID;
- source change после command validation;
- occupied destination;
- restart до extraction, после payload save, после remove, после placement;
- два NPC на один source/destination;
- stale/duplicate/out-of-order markers;
- разных NPC forms и LEFT/RIGHT hand transforms;
- dedicated server без client classloading.

Обязательная проверка data integrity сравнивает item registry ID, count,
`contentsUuid`, serialized stack NBT, `BackpackStorage` contents, upgrades,
colors и display item до/после. Визуально проверяются настоящая tier model,
цвета, display item, отсутствие z-fighting и корректность формы тела.

Ручной запуск Minecraft, визуальная приёмка и crash-kill matrix не считаются
пройденными только потому, что compile/unit tests зелёные; их результаты нужно
фиксировать отдельно.
