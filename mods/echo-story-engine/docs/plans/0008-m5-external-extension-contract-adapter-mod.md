# 0008 M5 External Extension Contract And Adapter Mod

## Цель

Реализовать M5: стабильный публичный контракт, через который отдельные Forge-моды явно регистрируют обработчики
абстрактных Story action, а Echo Story Engine валидирует эти обработчики, запускает actions, принимает completion,
безопасно восстанавливает WAITING после рестарта и не переносит concrete gameplay logic в core.

## Текущая архитектура external actions

- `story-contracts` содержит нейтральные `ExternalCommand`, `ActionExecutionId`, `ExternalActionResultCategory` и
  runtime input/state.
- `story-core` создаёт `ExternalCommand` при входе execution в `WAITING`.
- `story-persistence` умеет сохранять `PendingActionCheckpoint` и создавать recovery request с тем же
  `ActionExecutionId`.
- `story-forge-1.20.1` сохраняет checkpoint до dispatch, затем вызывает `ExternalCommandSink`.
- Production sink сейчас только диагностирует отсутствие handler через `ESE-FORGE-ACTION-001`.
- GameTest M4 доказывает recovery через тестовый прямой вызов host, но не через отдельный adapter mod.

## Границы scope firewall

1. Нужно ли изменение, чтобы определить, валидировать, выполнить, сохранить или диагностировать story?
   Да: это публичная граница выполнения внешних story actions.
2. Может ли это существовать как standalone gameplay system без Story Engine?
   Сам bridge нет; concrete handler в adapter mod может, поэтому gameplay остаётся за пределами core/engine.
3. Упоминает ли изменение concrete Minecraft mechanic/entity/mod/gameplay domain?
   Только Forge lifecycle/event/server API в Forge-модулях. Concrete mechanics в M5 не добавляются.

Вывод: допустимы только neutral contracts, Forge registration bridge и story-runtime dispatch/recovery orchestration.

## Архитектурное решение

Добавить два публичных модуля:

```text
story-contracts <- story-extension-api <- story-extension-forge-1.20.1
story-contracts <- story-core <- story-definition <- story-echoscript-adapter <- story-forge-1.20.1
story-contracts <- story-core <- story-persistence <- story-forge-1.20.1
story-forge-1.20.1 -> story-extension-api
story-forge-1.20.1 -> story-extension-forge-1.20.1
```

`story-extension-api` остаётся plain Java 17 API без Forge/Minecraft/core implementation/source/persistence/EchoScript.

`story-extension-forge-1.20.1` содержит только Forge registration event, server-scoped factory/context/registrar.
Только `story-forge-1.20.1` и `story-extension-forge-1.20.1` имеют право импортировать Forge/Minecraft.

Старое правило “только platform module импортирует Forge” уточняется новым ADR: platform runtime и public Forge bridge
являются двумя разрешёнными platform-facing modules.

## Public API

`story-extension-api` вводит:

- версию Story Extension API `1.0`;
- descriptor extension с namespaced id, display name, implementation version, required major, minimum minor;
- descriptor action handler с action type и capabilities;
- server-scoped session;
- request types для start, recovery и cancel;
- result types для start/recovery/cancel;
- immutable completion data с result category и bounded output facts;
- thread-safe completion sink с явным acceptance result.

API использует существующие `StoryId`, `ExecutionId`, `ActionExecutionId`, `NamespacedId` и
`ExternalActionResultCategory`. В API нет Minecraft, Forge, filesystem, logging backend, `CompletableFuture`,
reflection, scanning или serialization framework.

## Forge bridge

`story-extension-forge-1.20.1` вводит server-scoped registration event:

- event публикуется engine один раз при создании `ForgeServerHost`;
- external mod подписывается на Forge bus и вызывает registrar;
- event не cancelable, server-side only по смыслу контракта;
- registrar закрывается после event, поздняя регистрация отклоняется;
- фабрика создаёт одну session на один active host/server;
- bridge не отдаёт mutable engine collections, persistence, catalog или runtime internals.

## Lifecycle

- На server start engine создаёт host.
- Host публикует registration event.
- Собранные factories валидируются до установки active catalog handlers.
- Для совместимых и уникальных extensions создаётся одна session.
- Duplicate extension id, incompatible API version и duplicate action type получают diagnostics и не становятся active.
- После sessions recovery requests могут быть направлены в handlers.
- На server stop sessions закрываются idempotently, completion sink закрывается, очереди drain/reject новых work items.
- Integrated world restart получает fresh sessions и не использует global mutable registry.

## Compatibility/version policy

- Engine поддерживает Story Extension API `1.0`.
- Descriptor требует exact major и minimum minor.
- `requiredMajor != 1` отклоняется.
- `minimumMinor > 0` отклоняется для engine `1.0`.
- Incompatible extension rejected before handler activation with one structured diagnostic.

## Duplicate policy

- Один action type может иметь только одного active owner.
- Duplicate action type обнаруживается до установки active handler catalog.
- Diagnostic содержит action type и оба owners.
- Ambiguous action type недоступен.
- Уникальные action types от не конфликтующих extensions остаются usable.

## Threading/completion/cancellation/recovery

- registration, session creation, handler `start`, `recover`, `cancel` выполняются на server thread.
- `start` должен быть быстрым; async завершение возвращается через sink.
- completion sink можно вызывать с любого thread.
- Engine копирует completion в immutable data и ставит bounded server work item.
- Runtime completion применяется только на server thread и не reentrant.
- Sink возвращает acceptance: accepted, duplicate, unknown action, queue full, shutdown, invalid request.
- `ActionExecutionId` является idempotency key для start/recover/completion.
- cancel направляется handler только если он заявил capability; unsupported cancel диагностируется без gameplay side effects.
- Missing provider при новом execution fails только affected command через diagnostic.
- Missing provider при recovery сохраняет durable WAITING и диагностирует blocked recovery без нового action id.

## Diagnostics

Все diagnostics проходят через текущий structured pipeline и bounded buffer. Добавляются категории:

- registration opened/closed;
- extension accepted/rejected/incompatible/duplicate id;
- duplicate action owner;
- session create/close failed;
- handler start/recover/cancel accepted/rejected/threw;
- completion accepted/rejected duplicate/unknown/full/shutdown;
- recovery blocked/unsupported/failed/resumed;
- diagnostic context bounded.

Operator-facing messages пишутся по-русски в UTF-8. Stable codes/API names остаются английскими.

## Commands

Добавить read-only:

```text
/echo_story extensions
/echo_story extension <extensionId>
```

List показывает id, display name, implementation version, API requirement, status, session state, action count,
warning/error count. Detail показывает descriptor, status/rejection, action types/capabilities и recent diagnostics.

## Separate adapter build

Создать отдельный Gradle build:

```text
integration-tests/echo-story-extension-test-mod
```

Он не входит в root `settings.gradle`, использует опубликованные artifacts из `build/integration-maven`, имеет mod id
`echo_story_extension_test`, display `Echo Story Extension Test`, package
`io.github.kxyr0.echostoryengine.testextension` и runtime dependency на `echo_story_engine`.

Adapter содержит только deterministic test actions:

- async success;
- explicit failure;
- cancellation;
- restart recovery with same `ActionExecutionId`;
- duplicate completion;
- completion from non-server thread;
- completion after shutdown.

Без sleeps и без concrete gameplay.

## Publication strategy

Root task публикует в local file Maven repo:

```text
build/integration-maven
```

Минимум: `story-contracts`, `story-extension-api`, `story-extension-forge-1.20.1`.
Adapter build потребляет только этот Maven repo и Forge/Maven Central, не project deps, не Maven Local runtime.

## GameTest and isolated server strategy

- GameTest должен запускать engine вместе с отдельным adapter mod jar, а не тестовыми source-set shortcuts.
- Zero-test guard обновляется с учётом M5 scenarios.
- Isolated server smoke содержит ровно engine jar и adapter jar.
- Artifact inspections проверяют отсутствие adapter/test classes в engine jar и отсутствие engine/api/classes копий в
  adapter jar.

## Planned production packages

- `story-extension-api/.../descriptor`: API version, descriptors, capabilities.
- `story-extension-api/.../execution`: request/result/completion contracts.
- `story-extension-forge-1.20.1/.../forge`: registration event, registrar, context, factory.
- `story-forge-1.20.1/.../extension/catalog`: validated active extension/action catalog.
- `story-forge-1.20.1/.../extension/lifecycle`: event publication, sessions, close policy.
- `story-forge-1.20.1/.../extension/dispatch`: command dispatch, recovery, cancellation, completion queue.
- `story-forge-1.20.1/.../extension/readmodel`: command read models.
- `story-forge-1.20.1/.../diagnostic/catalog`: M5 diagnostics.

Имена с `Manager`, `Helper`, `Utils`, `Common`, `GlobalService`, `Impl` не используются.

## Tests

Plain JVM tests:

- API invariants and defensive copies;
- compatibility major/minor;
- duplicate extension/action;
- freeze/late registration;
- immutable active catalog;
- start/recover/cancel/completion acceptance;
- completion queue full/shutdown/duplicate/unknown;
- exception isolation;
- close idempotency;
- bounded diagnostics and Russian command formatting;
- architecture rules for new modules and no forbidden imports/scanning/global registry.

Forge/GameTest:

- separate mod registration;
- start/completion/failure/cancellation;
- recovery same id after restart;
- duplicate completion ignored;
- missing provider recovery blocked;
- incompatible provider rejected;
- duplicate action provider rejected;
- provider exception isolated;
- shutdown rejects late completion.

## Planned files

- `settings.gradle`, `build.gradle`, `gradle.properties`.
- `story-extension-api/**`.
- `story-extension-forge-1.20.1/**`.
- `story-forge-1.20.1/build.gradle`.
- `story-forge-1.20.1/src/main/java/.../extension/**`.
- `story-forge-1.20.1/src/main/java/.../diagnostic/**`.
- `story-forge-1.20.1/src/test/java/**`.
- `story-forge-1.20.1/src/gameTest/java/**`.
- `integration-tests/echo-story-extension-test-mod/**`.
- `docs/decisions/0014-*.md` through focused M5 ADRs.
- `docs/extensions/**`.
- `docs/architecture/PROJECT_MAP.md`, dependency docs, forge docs, `README.md`, `PROGRESS.md`.

## Risks

- ForgeGradle may not make a separate adapter jar available to `runGameTestServer` without extra run configuration.
- Publication to file Maven repo must avoid Maven Local assumptions.
- Completion from another thread can accidentally apply runtime state off server thread unless queue boundaries are strict.
- Recovery without provider must preserve WAITING, not convert it into success/failure.
- Source-shape limits require small focused production types and package splits.

## Rollback

Manual rollback through the current diff. Do not use destructive git commands, commits, branches, tags or pushes without
explicit user request.

## Verification

Required final checks:

- `.\gradlew.bat projects`
- `.\gradlew.bat dependencies`
- `.\gradlew.bat test`
- `.\gradlew.bat architectureTest`
- `.\gradlew.bat check`
- `.\gradlew.bat build`
- `.\gradlew.bat sourceShapeCheck`
- `.\gradlew.bat checkWhitespace`
- `git diff --check`
- root integration publication task;
- independent adapter build;
- engine and adapter artifact inspections;
- `.\gradlew.bat :story-forge-1.20.1:runGameTestServer`
- isolated two-mod server smoke when local EULA is already accepted.

## Non-goals

- No Dialogue/NPC/navigation/vehicle/combat/animation/wardrobe/UI/gameplay integration.
- No EchoScript standalone project changes.
- No reflection/classpath scanning/ServiceLoader/static global registry.
- No network protocol, client UI or editor.
- No exact-once external action guarantee.
- No commits.

## Status

Implemented and verified.
