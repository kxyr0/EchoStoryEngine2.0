# Архитектура

## Граница

Репозиторий зависит только от публичных Echo NPC / Echo Navigation API,
generic public Echo Animation marker API в optional-модуле, Sophisticated
Backpacks/Core и Forge 1.20.1. Ни один существующий Echo engine не импортирует
этот адаптер или Sophisticated Backpacks.

Удаление основного JAR не нарушает работу Echo NPC. Удаление optional Animation
adapter оставляет режим `IMMEDIATE`. Основной мод не загружает классы Echo
Animation.

## Модули

- `npc-backpack-contracts`: immutable IDs, requests, views, statuses and service;
- `npc-backpack-core`: plain-Java state machine, marker order, reservations,
  ownership, idempotency and recovery matrix;
- `npc-backpack-forge-1.20.1`: world mutation, persistence, navigation, sync,
  rendering and commands;
- `npc-backpack-script-adapter-1.20.1`: один facade и runtime-мосты Story/Dialogue/Trigger для `Npc.*` calls;
- `npc-backpack-animation-adapter-1.20.1`: optional generic marker listener;
- `npc-backpack-testmod-1.20.1`: development-only compatibility/GameTest scope.

Contracts/core имеют 0 Minecraft, Forge и Sophisticated imports. Production
source shape: не более 180 logical lines на Java-файл и не более 7 production
Java-файлов в leaf package.

## Equip flow

1. Разрешить ActorId через публичный NPC navigation-body bridge.
2. Проверить точный `BackpackBlockEntity`, UUID и fingerprint.
3. Зарезервировать `dimension + BlockPos`, синхронно записать journal.
4. Выбрать безопасную соседнюю позицию и передать ходьбу Echo Navigation.
5. Повернуть NPC к блоку и проверить tolerance.
6. Скопировать реальный stack, записать payload и `fsync` journal.
7. Повторно проверить fingerprint и только затем удалить source block.
8. Прикрепить stack к BACK (или ждать authored markers).
9. Записать terminal result, освободить reservation и синхронизировать clients.

## Unequip flow

До начала проверяются ownership, empty/recoverable destination и reservation.
После подхода используется публичный `BackpackItem.tryPlace` с временным
FakePlayer context. После lifecycle адаптер заново читает block entity и
проверяет UUID + полный payload fingerprint. Receipt о размещении сохраняется
до очистки attachment; поэтому crash между этими шагами восстанавливается без
дубликата.

## Rendering and sync

Echo NPC предоставляет generic `TORSO_BACK`, `LEFT_HAND`, `RIGHT_HAND` model
anchors. Adapter не импортирует NPC renderer/entity internals. Exact stack
рендерится через Minecraft ItemRenderer, который вызывает настоящий
Sophisticated Backpacks item renderer/model, включая tier, dyes и display item.

Transforms читаются из client resource
`assets/<namespace>/attachment_transforms/*.json` и выбираются по active form.
Server отправляет stack, attachment point, status и monotonic revision. Client
отбрасывает stale snapshots и не принимает решений об ownership или world
mutation.
