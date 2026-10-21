# Changelog

## 0.2.0

- Added PA0 opening-scene commands: `Player.SpawnAt`, `Player.SetHotbarItem`, `Player.SelectHotbarSlot`,
  `Player.SitAt` and `Player.SelectedHotbarSlot`.
- Added Forge runtime support for current-player placement, authored hotbar slots 1..9, selected-slot sync and
  seat-provider backed `SitAt`.
- Added `/echo_player_actor` diagnostics commands for spawn, hotbar, selection, sit, inspect and opening status.
- Added `echo_story_player_actor_adapter` Story Extension actions for durable Story Engine execution.
- Added optional `echo_player_actor_refurbished_furniture_adapter` chair seat provider.

## 0.1.0

- Создан PA1: контракты, core, Forge API/runtime, EchoScript adapter и testmod.
- Добавлены control lease, typed IDs, Rotate, LookAt, Move, Stop, Interact, StandUp, StopUsingItem и predicates.
- Добавлены project map, русская документация и проверки формы исходников.
