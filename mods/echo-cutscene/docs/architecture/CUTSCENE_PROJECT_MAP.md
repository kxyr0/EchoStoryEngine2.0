# Echo Cutscene: карта проекта

Версия: `0.1.0`.

Модули:

- `cutscene-contracts` - нейтральные идентификаторы, описание камер, definition и runtime DTO. Не зависит от Minecraft/Forge.
- `cutscene-core` - компиляция кривых, часы playback, registry lifecycle, recording/resampling/simplification.
- `cutscene-forge-api-1.20.1` - public Forge events/services для других модов.
- `cutscene-forge-1.20.1` - detached camera runtime, network, commands, body lock.
- `cutscene-testmod-1.20.1` - development-only тестовая точка загрузки.

Отдельные интеграции:

- `../echo-dialogue-cutscene-adapter` слушает `DialogueExternalWaitDispatchEvent` и вызывает `CutsceneService`.
- `../echo-debug` версии `0.2.0` добавляет optional команды `/echo_debug cutscene ...`.
- Echo NPC N5 form anchors (`eyes`, `head`, `chest`, `feet`) являются preferred
  источником framing targets для NPC child/adult scenes.

Главная гарантия C1: игрок не переводится в spectator. Серверное тело остается `ServerPlayer`, а камера на клиенте
переключается на client-only `Marker`.
