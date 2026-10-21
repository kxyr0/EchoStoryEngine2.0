# Echo NPC Sophisticated Backpacks Adapter

Независимый Forge 1.20.1-адаптер, который позволяет Echo NPC переносить
настоящий блок Sophisticated Backpack на спину или в руку и затем ставить тот
же рюкзак обратно в мир. Версия мода: `0.2.0`, mod id:
`echo_npc_sophisticated_backpacks`.

Главная гарантия — переносится копия реального `ItemStack` из
`BackpackBlockEntity`, сохраняющая `contentsUuid`. Глобальные данные
Sophisticated Backpacks по этому UUID не клонируются и не преобразуются, поэтому
сохраняются содержимое, upgrades, цвета, display item и вложенные рюкзаки.

## Сборка

Требуются Java 17 и локальные репозитории `../echo-npc`, `../echo-navigation`,
`../echo-animation`. Установленные JAR Sophisticated Backpacks автоматически
берутся из PrismLauncher instance `Форма Мысли`; другой каталог задаётся через
`-Psophisticated_mods_dir=<path>`.

```powershell
.\gradlew.bat check build --console=plain
```

Основной production JAR создаётся в
`npc-backpack-forge-1.20.1/build/libs/`. Script и Animation adapters собираются
отдельными JAR и не встраиваются в основной мод. Testmod development-only.

## Команды

```text
/echo_npc_backpack equip <npc_id> <x> <y> <z>
/echo_npc_backpack unequip <npc_id> <x> <y> <z>
/echo_npc_backpack inspect <npc_id>
/echo_npc_backpack transition <npc_id>
/echo_npc_backpack payload <npc_id>
/echo_npc_backpack reservations
/echo_npc_backpack animation binding <npc_id>
/echo_npc_backpack diagnostics
/echo_npc_backpack recover <npc_id>
/echo_npc_backpack debug anchors <npc_id>
/echo_npc_backpack debug off
```

Подробнее: [архитектура](docs/ARCHITECTURE.md),
[транзакции и recovery](docs/TRANSACTION_AND_RECOVERY.md),
[EchoScript API](docs/ECHOSCRIPT_RU.md) и
[animation markers](docs/ANIMATION_MARKERS_RU.md).
