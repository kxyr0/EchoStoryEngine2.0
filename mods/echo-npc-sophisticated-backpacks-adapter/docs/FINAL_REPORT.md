# Final report — 2026-08-04

Независимый репозиторий адаптера создан в
`D:\JDHSeason\mods\echo-npc-sophisticated-backpacks-adapter` и инициализирован
как Git-репозиторий. Версия production-мода: `0.2.0`.

## Результат

Основной JAR:

`npc-backpack-forge-1.20.1/build/libs/echo-npc-sophisticated-backpacks-adapter-0.2.0.jar`

SHA-256:

`2F8C0F9E29F3680BC9109783A7BACE41C674AFC8A8CBD7B070860C72D971B7CD`

Optional script и animation adapters выпускаются отдельными JAR. Testmod не
входит в production JAR; автоматический scan подтвердил 119 допустимых entries
и отсутствие классов Echo, Sophisticated Backpacks и testmod.

## Выполненные проверки

- `gradlew check build`: успешно, 51 task; unit, architecture, whitespace,
  source-shape, reobfuscation и production-JAR verification.
- Echo Animation: definition/core/EchoScript/Forge test suites успешно.
- Forge GameTest: `All 1 required tests passed` на Minecraft 1.20.1,
  Forge 47.4.10, Sophisticated Backpacks 3.24.62.2017 и Core 1.3.73.2192.
- GameTest подтвердил exact round-trip нормализованного backpack payload:
  fingerprint, UUID, цвета и external `BackpackStorage` contents сохранены.

## Граница автоматической приёмки

Ручной запуск клиента, визуальная проверка tier model/display item/anchors и
ручная crash-kill/restart матрица не выполнялись и не заявляются как пройденные.
Полный чек-лист находится в `docs/TESTING.md`.
