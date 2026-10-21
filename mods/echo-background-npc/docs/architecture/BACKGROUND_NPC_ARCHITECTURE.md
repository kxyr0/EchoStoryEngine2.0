# Архитектура Echo Background NPC

Система разделяет стабильного логического агента и временное тело. `BackgroundAgentRuntime` хранит compact immutable state для всех агентов. В пределах 64 блоков `MaterializedBodies` создаёт ровно один общий для multiplayer объект `BackgroundNpcEntity`; после 76 блоков тело удаляется, но identity, schedule, appearance и persistent exceptions остаются. Сущность наследует `Entity`, не `Mob`, и не имеет Brain, GoalSelector или PathNavigation.

## Границы модулей

- `background-npc-contracts` — typed IDs, definitions, tasks, state, diagnostics.
- `background-npc-core` — schedule index, virtual state, leases, LOD, route cache/coalescing/budget, render primitives и ambient sessions без Forge implementations.
- `background-npc-echoscript-adapter` — parser/semantic binding четырёх ES2 resource kinds и atomic candidate.
- малые time/navigation/animation/dialogue/voice adapters — единственные места зависимости от публичных API соседних Echo-систем.
- `background-npc-forge-api-1.20.1` — service lookup, named-location resolver и materialization event.
- `background-npc-forge-1.20.1` — reload host, persistence, entity lifecycle, network, renderer и commands.

Echo Time получает один revisioned provider со всеми отсортированными occurrences. События crossing меняют state; `INITIALIZED`, `SET` и `RATE_CHANGED` вызывают controlled recompute. One-shot ledger обеспечивает exactly-once и сохраняется между restart.

Route request имеет ключ `(navigationRevision, startAnchor, targetAnchor, movementProfile)`. Одинаковые запросы coalesce; не более двух новых планов вычисляется за tick. Adapter вызывает route-only public Echo Navigation API, который использует тот же immutable surface snapshot и A*, что основной backend, но не требует `Mob`. Geometry shared и не копируется на каждого агента.

Client renderer вызывается Minecraft после distance/frustum rejection. Он передаёт видимого subject в public Echo Animation humanoid renderer; при недоступном provider остаётся дешёвый shared `PlayerModel`. Texture resolution и immutable render resources кешируются. Сервер меняет animation playback только на task/line transitions.

Ambient conversation временно получает lease обоих участников, останавливает travel, разворачивает тела друг к другу и автоматически меняет talk/listen state. На клиента отправляется только выбранная ближайшая line с line-of-sight; максимум один overlay. Echo Dialogue foreground автоматически скрывает transient overlay. Echo Voice ambient lane является positional, ограничена тремя голосами, отсекает дальний звук до decode и освобождает lane при foreground voice.

Resource reload компилирует полный immutable catalog. Ошибка отклоняет candidate целиком и оставляет LKG. Активная ambient session держит свою dialogue revision до завершения. Restart сохраняет только dead IDs и bounded occurrence ledger; обычное положение восстанавливается из Echo Time и расписания.
