# План D1

## Границы модулей

- `dialogue-contracts`: идентификаторы, diagnostics и публичные неизменяемые контракты без Forge, Minecraft и EchoScript syntax types.
- `dialogue-presentation-api`: public neutral D6 contracts для dynamic speaker presentation providers.
- `dialogue-core`: immutable compiled dialogue, deterministic session transitions и политика одной активной сессии на аудиторию.
- `dialogue-echoscript-adapter`: semantic binding профиля `DialogueResource` поверх EchoScript parse result.
- `dialogue-forge-1.20.1`: composition root, resource reload, catalog, commands, networking, client overlay, GameTests и packaging.

## Жизненный цикл сессии

Сессия создаётся на сервере для одной аудитории, открывает первую строку, принимает только явный `Advance`, увеличивает revision при каждом подтверждённом переходе и после последней строки переходит в `COMPLETED`. `Cancel` переводит сессию в `CANCELLED`. Stale revision и wrong session ID не меняют состояние.

## Server/client ownership

Сервер владеет каталогом, сессиями, validation и progression. Клиент получает read model overlay и отправляет только bounded advance request с `sessionId` и expected `revision`.

## Source pipeline

UTF-8 `.dialogue.es` → EchoScript parser → immutable syntax tree → semantic binder → validation → immutable `CompiledDialogue` → candidate catalog → atomic apply.

## Non-goals D1

Story Engine integration, choices, branching, NPC lookup, portraits, animation, camera, voice, typewriter, quests, inventory, persistence across restart and arbitrary scripting are deferred.

## D5

Текущий план D5 находится в `docs/plans/D5_EXECUTION_PLAN.md`.

D5 снимает старый D1 non-goal только для двух authored visual modes на `Alias(...)`:

- `Portrait("namespace:path")`;
- `SkinHead("namespace:path")`.

NPC/entity lookup, live player skins, remote URLs, voice, camera, movement locking, generic UI и Story-specific visual arguments остаются deferred.

## D6

Текущий план D6 находится в `docs/plans/D6_EXECUTION_PLAN.md`.

D6 снимает старый non-goal только для нейтральной runtime-границы dynamic speaker presentation provider:

- external mod регистрирует `SpeakerPresentationProviderFactory` через typed Forge event;
- provider возвращает полный `ResolvedSpeakerPresentation`: display name и optional `SpeakerVisualRef`;
- при no provider, unavailable, failure, malformed result или ambiguity используется authored fallback;
- provider output не сохраняется, не входит в compiled fingerprint и не меняет network protocol `3`.

Echo NPC integration, ActorId, NpcService lookup, entity lookup, distance checks, priorities, partial merge, remote skins и automatic same-line refresh остаются deferred до N4.
