# 0014 External Extension Contract And Forge Bridge

## Status

Accepted.

## Context

M4 сохраняет external action checkpoint до dispatch, но production handler registry отсутствует. M5 должен позволить
отдельному Forge-моду зарегистрировать обработчики abstract Story actions без попадания gameplay logic в core.

## Decision

Добавлены два публичных модуля:

```text
story-contracts <- story-extension-api <- story-extension-forge-1.20.1
```

`story-extension-api` содержит loader-neutral Java 17 contracts: extension descriptor, API version, action
capabilities, start/recovery/cancel requests, invocation results, completion data и thread-safe completion sink.
Модуль зависит только от `story-contracts` и Java standard library.

`story-extension-forge-1.20.1` содержит только Forge registration bridge: server-scoped registration event, registrar,
factory и context. Этот модуль и `story-forge-1.20.1` являются единственными production modules, которым разрешены
Forge/Minecraft imports.

`story-forge-1.20.1` публикует registration event при создании server host, валидирует descriptors, создаёт одну
session на host, строит immutable action catalog, dispatches external actions after durable save, принимает
completion через bounded server-thread queue и запускает recovery scan durable WAITING snapshots после подготовки
sessions.

Duplicate extension id отклоняется. Duplicate action type не выбирается по order/last-wins и недоступен как
ambiguous. Уникальные action types остаются usable.

API compatibility policy: engine поддерживает Story Extension API `1.0`; descriptor требует exact major и minimum
minor.

## Alternatives

- Reflection/classpath scanning/ServiceLoader. Rejected: lifecycle должен быть явным и server-scoped.
- Global static registry. Rejected: integrated server restart требует fresh sessions and no leaked host state.
- Put handler contracts in `story-core`. Rejected: core must not know Forge or extension lifecycle.
- Choose last duplicate action provider. Rejected: order-dependent behavior hides integration errors.

## Consequences

External mods compile against public artifacts and depend on `echo_story_engine` at runtime. Engine jar embeds public
extension API/bridge classes. Recovery remains at-least-once; `ActionExecutionId` is the idempotency key. Missing
provider during recovery preserves durable WAITING and reports blocked recovery.

## Enforcement

ArchUnit forbids Forge/Minecraft imports outside `story-forge-1.20.1` and `story-extension-forge-1.20.1`.
`sourceShapeCheck` enforces source folder and type-size limits. Root task `publishExtensionIntegrationArtifacts`
publishes public artifacts to `build/integration-maven`; `buildExtensionTestAdapter` builds a separate adapter mod
against those artifacts.

## Links

- `docs/plans/0008-m5-external-extension-contract-adapter-mod.md`
- `docs/extensions/EXTENSION_API.md`
- `docs/extensions/CREATING_AN_ADAPTER.md`
