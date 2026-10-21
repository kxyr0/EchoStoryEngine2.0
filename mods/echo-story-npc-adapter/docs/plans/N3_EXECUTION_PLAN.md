# План выполнения N3

## Цель

Создать маленький независимый Forge adapter mod:

Story WAITING action -> deterministic `ActorMutationId` -> public `NpcService`
mutation -> NPC durable/materialized postcondition -> Story completion.

## Идентичность

- Имя проекта: Echo Story NPC Adapter.
- Root project name: `echo-story-npc-adapter`.
- Mod ID: `echo_story_npc_adapter`.
- Version: `0.1.0-SNAPSHOT`.
- Maven group: `io.github.kxyr0.echostorynpcadapter`.
- Base package: `io.github.kxyr0.echostorynpcadapter`.
- Minecraft: `1.20.1`.
- Forge: `47.4.10`.
- Java: 17.
- Extension ID: `echo_story_npc_adapter:bridge`.

## Входные Public Artifacts

Story public artifacts:

- `story-contracts`;
- `story-extension-api`;
- `story-extension-forge-1.20.1`.

Echo NPC public artifacts:

- `npc-contracts`;
- `npc-forge-api-1.20.1`.

Production adapter не использует project dependencies на sibling builds.
Локальные automated builds используют sibling repositories
`build/integration-maven`, а не Maven Local.

## Фактические результаты аудита

- Фактический путь Story Engine repository:
  `D:\JDHSeason\mods\echo-story-engine`. Путь
  `D:\JDHSeason\mods\echostoryengine` из prompt отсутствует.
- Story `ActionStartRequest` и `ActionRecoveryRequest` expose
  `Map<String, String> payload`.
- Story `ActionCompletion` expose-ит `Map<String, String> outputs`.
- Story `ActionExecutionId` стабилен и является idempotency key адаптера.
- Story completion sink является thread-safe, но этот адаптер завершает actions
  на server tick path после наблюдения NPC state.
- Текущие production `/echo_story` commands read-only.
- Текущий EchoScript intrinsic `Action(...)` принимает `type`, `output`, `next`.
  Нейтральная Story model имеет `arguments`, но прежний binder не авторил
  action payload arguments из `.es` source.
- Echo NPC public API требовал caller-supplied mutation ID. N3 добавил
  `ActorMutationId`, optional IDs на spawn/teleport requests и
  `despawn(ActorId, ActorMutationId)`.

## Форма модуля

Один Forge Gradle project:

- main production source set: adapter mod;
- plain JVM tests для action identity, argument validation, mutation mapping,
  observation и completion mapping;
- architecture tests для dependency boundaries и JAR contents;
- optional integration-test support mod только для GameTest/acceptance, никогда
  не включается в production JAR.

## Типы Action

Единственные production action types:

- `echo_npc:spawn`;
- `echo_npc:teleport`;
- `echo_npc:despawn`.

`echo_npc:move` запрещен.

## Схема Arguments

Все payload values являются строками.

`echo_npc:spawn` требует:

- `actorId`;
- `definitionId`;
- `worldId`;
- `x`;
- `y`;
- `z`;
- `yaw`;
- `pitch`.

`echo_npc:teleport` требует:

- `actorId`;
- `worldId`;
- `x`;
- `y`;
- `z`;
- `yaw`;
- `pitch`.

`echo_npc:despawn` требует:

- `actorId`.

Unknown arguments отклоняются.

## Детерминированное сопоставление Mutation

Адаптер выводит ровно один `ActorMutationId` из `ActionExecutionId`:

- fixed adapter namespace;
- UUID-v5-style SHA-1 name derivation;
- canonical input: `ActionExecutionId.asString()`;
- без clock;
- без random;
- locale-independent lowercase output;
- все три action types используют одно правило mapping.

## Семантика Start

Start валидирует payload, выводит mutation ID, получает `NpcService` через
`EchoNpcServiceBridge.lookup(server)`, проверяет existing operation/actor state,
отправляет NPC mutation только через public `NpcService` и регистрирует bounded
observation. Start быстро возвращает управление и не ждет persistence или
entity materialization.

## Семантика Completion

Success требует точные public read-model postconditions:

- spawn: durable active actor, matching definition and transform, compatible
  definition, `MATERIALIZED`;
- teleport: matching actor at exact desired target, `MATERIALIZED`;
- despawn: durable absence / public query not found.

Outputs:

- spawn: `actorId`, `result=created|already_present`;
- teleport: `actorId`, `result=moved|already_at_target`;
- despawn: `actorId`, `result=removed|already_absent`.

Entity UUID и `EntityBindingId` никогда не выдаются.

## Семантика Recovery

Recover использует ту же payload validation и тот же deterministic mutation ID,
что и start. Если public NPC state уже доказывает postcondition, completion
отправляется повторно. Если matching operation pending, observation
восстанавливается. Если mutation еще не была отправлена, адаптер отправляет ее
один раз. Blocked actor states остаются waiting, пока Echo NPC не сообщит
compatible/materialized postcondition.

## Cancellation

N3 production cancellation не поддерживается. Handler capabilities объявляют
`supportsCancellation=false`; `cancel` возвращает unsupported и не выполняет
rollback.

## Observation

Адаптер хранит только bounded, server-scoped runtime observations. Он не
persist-ит adapter state, не хранит Minecraft entities, не создает threads и не
force-load-ит chunks. Observations проверяются на server tick через public
`NpcService.operation()` и `NpcService.query()`.

## Acceptance Support

Так как текущие production Story commands read-only, а прежний EchoScript
`Action(...)` authoring не давал payload arguments, полный automated end-to-end
acceptance требует отдельный test-only support mod или будущий public Story
start/payload surface. Этот support не должен попадать в production adapter JAR.

## Не-цели

Нет movement abstraction, navigation, pathfinding, animation, dialogue, AI,
schedules, combat, inventory, wardrobe, cinematics, HTTP, skin service,
EchoScript parsing, persistence implementation, renderer или entity code.

## Стратегия тестирования

- Plain JVM tests для action constants, payload decoder, mutation ID mapping,
  fake `NpcService` start/recover/complete behavior, exact outputs и observation
  cleanup.
- Architecture tests для public-only dependencies и forbidden systems.
- JAR checks доказывают, что API classes и test support не bundled.
- Forge GameTests с real Story/NPC/adapter при наличии acceptance support.
- Isolated three-mod server acceptance с final JARs.
- Isolated production client acceptance с final JARs: visible spawn,
  return-to-title recovery, visible teleport, despawn, second-world isolation и
  clean shutdown.

## Rollback

Adapter repo можно удалить без изменений Story Engine. Echo NPC public API
changes являются compatibility-preserving overloads/optional fields; если N3
откатить, существующие Echo NPC commands продолжат использовать internally
generated `op-N` operation IDs.
