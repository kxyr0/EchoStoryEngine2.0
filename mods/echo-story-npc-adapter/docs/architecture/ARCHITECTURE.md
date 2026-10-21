# Архитектура

Echo Story NPC Adapter является тонким Forge-слоем композиции между двумя
публичными контрактами:

- Story Extension API диспетчеризует внешние actions.
- Публичный Echo NPC `NpcService` выполняет durable actor mutations.

Адаптер не владеет состоянием Story, NPC repository, Minecraft entity,
renderer или политикой загрузки chunks.

## Runtime-Поток

1. `EchoStoryExtensionRegistrationEvent` публикуется Story Engine host.
2. Адаптер регистрирует `StoryNpcExtensionFactory`.
3. Factory создает по одной `StoryNpcExtensionSession` на Story host/server.
4. Handler получает start/recover на server thread.
5. Адаптер выводит детерминированный `ActorMutationId`.
6. Адаптер получает `NpcService` через `EchoNpcServiceBridge`.
7. Адаптер отправляет spawn/teleport/despawn через public API.
8. `ObservationBook` проверяет public operation/query state на server tick.
9. Адаптер отправляет `ActionCompletion` в Story completion sink.

## Владение

- Story owns `StoryId`, `ExecutionId`, `ActionExecutionId` and WAITING state.
- Echo NPC владеет `ActorId`, durable actor state, `EntityBindingId` и entity
  reconciliation.
- Адаптер владеет только ограниченными runtime observations.

## Рестарт

Адаптер не сохраняет собственное состояние. Recovery управляется Story через
at-least-once вызовы `recover` и durable read models Echo NPC.
