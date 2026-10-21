# 0007 M4 Forge Host, Resource Loading, Diagnostics, GameTest, Server Smoke, And Packaging

## Цель

Реализовать M4: настоящий server-side Forge host для Echo Story Engine, загрузку `.es` StoryResource из server
resources/datapacks, установку immutable compiled catalog, world-specific durable persistence root, server-thread
ownership runtime state, bounded background persistence scheduling, read-only diagnostic command tree, GameTest,
dedicated-server smoke и проверку production packaging.

M4 должен доказать путь:

```text
Forge загружает мод
  -> создается server-scoped host
  -> server resources/datapacks дают data/<namespace>/echo_story/stories/**/*.es
  -> EchoScript компилирует StoryResource
  -> immutable catalog атомарно установлен
  -> runtime execution доходит до WAITING
  -> checkpoint сохранен до выдачи command
  -> test action завершается через server thread
  -> execution доходит до FINISHED
  -> финальный checkpoint сохранен
  -> diagnostics доступны через одну команду
  -> GameTest и dedicated server checks подтверждают lifecycle/packaging
```

## Не-цели

- Не изменять standalone EchoScript repository.
- Не реализовывать M5 Extension Contract, public action registry, third-party adapter discovery или production
  external action handler SPI.
- Не реализовывать `DialogueResource`, Dialogue UI, NPC, navigation, combat, vehicles, cinematics, magic, inventory,
  client networking, editor, language server, Fabric, NeoForge, Kotlin, mixins или concrete gameplay systems.
- Не добавлять production test action как public extension mechanism.
- Не менять Minecraft, Forge, ForgeGradle, Gradle, Java, save schema V1 или public Story identifiers без ADR.
- Не создавать новый production Gradle module.
- Не коммитить изменения.

## Текущее состояние

- Рабочее дерево уже содержит завершенный M3.6 без коммита: `story-echoscript-adapter` добавлен, JSON source frontend
  удален из `story-definition`, EchoScript resolved from Maven local.
- Текущий Gradle graph:

```text
story-contracts <- story-core <- story-definition <- story-echoscript-adapter
story-contracts <- story-core <- story-persistence
story-contracts <- story-core <- story-forge-1.20.1
story-contracts <- story-forge-1.20.1
```

- Фактический Forge runtimeClasspath сейчас содержит `story-core`, `story-contracts` и Forge/Minecraft; он еще не
  содержит `story-definition`, `story-echoscript-adapter`, `story-persistence`, Gson или EchoScript runtime.
- `story-forge-1.20.1` сейчас содержит только `EchoStoryEngineMod`, `ForgeStoryCompositionRoot`,
  `ForgeDiagnosticSink`, `ForgeStoryClock`, `VolatileExecutionStateRepository`.
- Текущий entrypoint создает composition root в constructor и запускает programmatic hello story в
  `FMLCommonSetupEvent`; это будет заменено.
- Forge resource discovery, datapack reload, durable world-bound file repository, command tree, GameTests и dedicated
  server smoke пока отсутствуют.
- `java -version` в оболочке показывает Java 21, но Gradle compilation настроен на Java toolchain/release 17.
- `minecraft_version=1.20.1`, `forge_version=47.4.10`, `mod_id=echo_story_engine`.
- EchoScript Maven local artifacts доступны:
  `io.github.kxyr0.echoscript:echo-script-api:0.1.0-SNAPSHOT` и
  `io.github.kxyr0.echoscript:echo-script-language:0.1.0-SNAPSHOT`.

## Ограничения

- Нижние модули (`story-contracts`, `story-core`, `story-definition`, `story-persistence`,
  `story-echoscript-adapter`) остаются без Forge/Minecraft imports.
- `story-forge-1.20.1` становится единственным production module, который знает Forge APIs, resource manager,
  server lifecycle, command registration и world path binding.
- EchoScript types не попадают в public API, runtime state, persistence schema или core ports.
- File `Path` не проходит через core ports.
- Mutable Story state принадлежит только server thread.
- Background I/O не вызывает Minecraft APIs и не мутирует Story state напрямую.
- Все новые human-facing документы, operator-facing diagnostics, command messages и комментарии архитектурных решений
  пишутся по-русски, при сохранении Java identifiers/API names на английском.
- Все production diagnostics/messages, добавленные в M4, должны быть UTF-8 и покрыты mojibake check.
- Нельзя ослаблять Checkstyle, PMD, ArchUnit, `sourceShapeCheck`, `checkWhitespace` или удалять failing tests.

## Архитектурное решение

M4 добавляет зависимости в Forge module, не меняя нижние модули:

```text
story-contracts <- story-core <- story-definition <- story-echoscript-adapter <- story-forge-1.20.1
story-contracts <- story-core <- story-persistence <- story-forge-1.20.1
```

`story-forge-1.20.1` будет использовать:

- `EchoScriptStorySourceCompiler` для in-memory `.es` compilation;
- `FileExecutionStateRepository` как durable repository behind core port;
- `ExecutionRecoveryPlanner` для restart/recovery;
- Forge server lifecycle events для создания/закрытия host;
- Forge/AddReloadListenerEvent или фактический 1.20.1-equivalent server reload API для resource reload;
- Brigadier через Forge command registration event для read-only diagnostics.

Для durability/platform decisions нужен новый ADR M4, покрывающий:

- canonical server resource location `data/<namespace>/echo_story/stories/**/*.es`;
- atomic catalog reload и invalid candidate retention;
- world-specific persistence root;
- server-thread ownership плюс bounded I/O scheduling;
- production dependency packaging strategy.

## Scope Firewall

1. Нужно ли это для story? Да: Forge host нужен, чтобы определить, загрузить, выполнить, сохранить и диагностировать
   story lifecycle на реальном сервере.
2. Может ли это существовать как standalone gameplay system? Нет, кроме внешних action handlers, которые M4 не
   реализует.
3. Упоминает ли concrete Minecraft mechanic/domain? Только Forge platform lifecycle/resource/command APIs. Они
   остаются в platform adapter module.

Вывод: реализация допустима только в `story-forge-1.20.1`, без gameplay semantics в core.

## Proposed Host Lifecycle

- `EchoStoryEngineMod` остается тонким `@Mod` entrypoint: регистрирует lifecycle handlers и не создает runtime story
  execution в constructor.
- `ForgeStoryCompositionRoot` создает platform adapters и lifecycle coordinator, но не компилирует `.es` и не хранит
  business rules.
- `ForgeServerHost` создается для конкретного `MinecraftServer` на server starting/started event и закрывается на
  stopping/stopped event.
- States: `NOT_STARTED`, `STARTING`, `ACTIVE`, `STOPPING`, `STOPPED`, `FAILED`.
- Один active host на один active server; повторные events idempotent.
- Integrated server restart создает fresh host; old host disposal clears queues, repositories, catalog references and
  server reference.
- Dedicated server использует тот же host path и не требует client classes.

## Resource Loading Lifecycle

- Canonical path: `data/<namespace>/echo_story/stories/**/*.es`.
- Resource location identity maps to source id: `<namespace>:echo_story/stories/.../<file>.es`.
- Source file path is not Story ID; Story ID comes from `Story("namespace:path", ...)`.
- Server resource reload discovers `.es` files from all namespaces/resource packs/datapacks through Minecraft server
  resource manager.
- Reload pipeline:
  1. discover resources;
  2. read UTF-8 source text;
  3. compile with `EchoScriptStorySourceCompiler`;
  4. collect diagnostics;
  5. reject candidate if any source has `ERROR`, duplicate Story ID, duplicate incompatible definition, or no valid
     initial catalog;
  6. atomically install new `CompiledStoryCatalogSnapshot`.
- Routine successful files are not logged one-by-one. Reload emits bounded summary diagnostics.

## Compiled Catalog Replacement Policy

- Catalog object is immutable and replaced atomically by reference.
- Readers either observe old catalog or new catalog, never partial state.
- Invalid reload retains previous catalog and records diagnostics.
- Duplicate same-fingerprint definitions may be accepted only if the plan/test proves deterministic equivalence;
  incompatible duplicate Story ID rejects the entire candidate.
- New executions use current catalog at start time.
- Active executions are pinned to the compiled fingerprint stored in their checkpoint.
- Old definitions may be retained only while referenced by active loaded executions; no unbounded history.
- Missing compatible definition during restore returns structured persistence diagnostic.

## Active-Execution Definition Policy

- Runtime host resolves a definition for an execution by `StoryId` plus checkpoint fingerprint.
- If no checkpoint exists, the current catalog definition starts the execution.
- If a checkpoint exists, compatible fingerprint must match the loaded execution; otherwise runtime suppresses command
  dispatch and reports `ESE-PERSISTENCE-*`.
- Active execution does not silently switch to a newer definition after reload.

## Persistence-Root Binding

- M4 reuses `FileExecutionStateRepository`.
- The root is derived from the active server world path through supported Minecraft 1.20.1 APIs.
- Proposed logical subdirectory:

```text
<world>/data/echo_story_engine/executions
```

- The exact API and path expression will be verified against local Forge mappings before implementation.
- Root is per world/server; one world must not load another world's executions.

## Server-Thread Ownership

- Story runtime calls and catalog reference changes happen on server thread.
- Forge event handlers enqueue work to the server thread when needed.
- Completion results re-enter through server thread before calling `StoryRuntimeUseCase`.
- No I/O worker calls Minecraft APIs or mutates Story runtime state.

## Background I/O Model

- M3 file repository is synchronous; M4 wraps it behind a server-host-owned bounded I/O scheduler.
- Server thread computes transition and immutable checkpoint payload first.
- I/O worker writes checkpoint and returns immutable save result.
- Server thread exposes external commands only after successful save callback.
- Queue limits:
  - default pending persistence operation capacity: 64;
  - per-tick result drain budget: 32;
  - shutdown flush timeout: 10 seconds.
- Queue overflow rejects new work with structured diagnostic and no command dispatch.
- Shutdown rejects new work, drains completed results, waits bounded time for in-flight saves, then closes worker.

## Server-Tick Ownership

- Server tick drains bounded host queues and applies completed persistence results.
- It does not scan all executions, evaluate stories every tick, or poll filesystem.
- Correct tick phase will be verified against Forge 1.20.1 event API.

## External Actions In M4

- Production M4 does not introduce M5 action registration.
- For GameTest only, a test-side adapter may observe the host-dispatched abstract command and feed a deterministic
  success completion back through server thread.
- Production host handles missing action support safely with structured diagnostics.

## Diagnostic Command Design

- Command root: `/echo_story`.
- Read-only subcommands:
  - `/echo_story health`;
  - `/echo_story stories`;
  - `/echo_story executions`;
  - `/echo_story execution <executionId>`;
  - `/echo_story diagnostics`;
  - `/echo_story diagnostics <limit>`.
- Permission level: 2.
- Output is Russian UTF-8. `diagnostics <limit>` accepts only positive bounded integers; the documented hard max is
  `50`.
- No raw source dump, full variables dump, full snapshot dump, absolute storage paths, stack traces, or mutation.
- Command reads the same bounded `InMemoryDiagnosticBuffer` and host health read model.

## Diagnostics And Russian UTF-8

- New M4 diagnostic messages/operator hints are Russian.
- Existing stable code identifiers remain unchanged.
- Add tests/scans for mojibake markers and expected Cyrillic text in diagnostics, resource fixture text and command
  formatting.
- Forge logging remains one namespace: `io.github.kxyr0.echostoryengine.story`.

## GameTest Design

- Configure actual Forge 1.20.1 GameTest source set/task path if supported by current ForgeGradle project.
- Minimal structure template only if Forge requires it.
- Required GameTests:
  1. host bootstrap creates server host and no client class is loaded;
  2. `.es` resource discovery builds catalog with expected Story ID;
  3. end-to-end host execution reaches WAITING, saves, dispatches test action, completes to FINISHED, saves final
     state, and avoids duplicate dispatch;
  4. host recreation with persistent storage recovers WAITING action and finishes;
  5. invalid reload retains previous catalog and emits rejection diagnostic.
- If actual reload infrastructure cannot be invoked safely from GameTest, the test must use Forge host APIs that go
  through the same resource/candidate pipeline and document the limitation.

## Dedicated-Server Test Design

- Use the generated ForgeGradle server/gameTestServer task path found from `.\gradlew.bat tasks --all`.
- Normal dedicated server smoke runs only if EULA is already accepted in the generated run directory.
- If EULA is not accepted, report exact path and manual step; do not auto-accept it.
- Server smoke checks startup, mod load, no client class loading, command registration, resource load summary, and
  clean shutdown where feasible.

## Production JAR Packaging

- Final Forge JAR must run outside Gradle source-set classpaths and without Maven Local at runtime.
- It must include or correctly nest:
  - `story-contracts`;
  - `story-core`;
  - `story-definition`;
  - `story-echoscript-adapter`;
  - `story-persistence`;
  - EchoScript API/language runtime;
  - Gson if required by `story-persistence`.
- It must not package Forge/Minecraft classes, GameTest-only classes, test action classes, source sets, absolute paths,
  or `examplemod`.
- Add/update packaging verification task to inspect final artifact contents.

## Planned Production Types

Planned package roots inside `story-forge-1.20.1`:

- `..story.forge`: thin mod entrypoint.
- `..story.forge.composition`: platform composition and lifecycle wiring.
- `..story.forge.host`: server host lifecycle, host state, immutable catalog, active execution routing.
- `..story.forge.resource`: server resource discovery and reload candidate compilation.
- `..story.forge.persistence`: world path binding and bounded persistence scheduling.
- `..story.forge.diagnostic`: Forge command formatting, health summary, Russian diagnostics.
- `..story.forge.infrastructure`: logging/time adapters and any retained port adapters.

Concrete type names will be chosen by responsibility during implementation. No `Manager`, `Helper`, `Utils`,
`Common`, `GlobalService`, or `Impl`.

## Planned Test Types

- Plain JVM tests for catalog atomic replacement, invalid reload retention, duplicate Story ID rejection, active
  fingerprint pinning, new execution using current definition, bounded queue overflow/shutdown, persistence ordering,
  command formatting, and Russian UTF-8/mojibake guard.
- Forge architecture tests for module direction, client class isolation, no mutable static host/server fields,
  no GameTest/test action in production artifact, no copied EchoScript, no absolute paths, no extra log files, package
  sizes and production JAR contents.
- GameTest source tests for host lifecycle/resource/execution/recovery/reload behavior.
- Dedicated-server smoke task/result documentation.

## Risks

- Forge 1.20.1 reload/GameTest APIs may require exact signatures different from current examples.
- Production packaging of non-mod plain Java libraries plus EchoScript may need Forge JarJar or explicit shaded/nested
  strategy; the current jar embeds only `story-contracts` and `story-core` classes.
- Asynchronous persistence could accidentally duplicate M3 save-before-dispatch semantics unless command exposure is
  explicitly delayed until save callback.
- GameTest server task may fail if local run assets/EULA/environment are not prepared.
- Russian messages require consistent UTF-8 through Java source, resources, Gradle and terminal output.

## Верификация

Required final commands:

- From EchoScript repository: `.\gradlew.bat clean check build publishToMavenLocal`.
- From Echo Story Engine repository:
  - `.\gradlew.bat projects`;
  - `.\gradlew.bat dependencies`;
  - `.\gradlew.bat test`;
  - `.\gradlew.bat architectureTest`;
  - `.\gradlew.bat check`;
  - `.\gradlew.bat build`;
  - `.\gradlew.bat checkWhitespace`;
  - `git diff --check`.
- Focused module tests:
  - `.\gradlew.bat :story-core:test`;
  - `.\gradlew.bat :story-definition:test`;
  - `.\gradlew.bat :story-persistence:test`;
  - `.\gradlew.bat :story-echoscript-adapter:test`;
  - `.\gradlew.bat :story-forge-1.20.1:test`.
- Determine and run actual GameTest server task path, expected `runGameTestServer` or ForgeGradle equivalent.
- Run normal dedicated server smoke only when EULA is already accepted; otherwise report exact EULA path/manual step.
- Inspect final distribution artifact contents and isolated production-JAR behavior.
- Explicit scans for platform leakage, Fabric imports, EchoScript leakage, copied sources, committed JARs, Maven Local
  runtime paths, client class references, mutable static server/host fields, additional log files, stale MDK identity,
  size limits, disabled/skipped checks, broad suppressions and cycles.

## Rollback

Manual rollback through the current diff. Do not use `git reset --hard`, destructive deletes, branch operations,
commits, tags or pushes without explicit user request.

## Документация

Update:

- `PROGRESS.md`;
- `README.md`;
- `docs/architecture/ARCHITECTURE.md`;
- `docs/architecture/DEPENDENCY_RULES.md`;
- `docs/architecture/PROJECT_MAP.md`;
- `docs/architecture/PORTABILITY.md`;
- `docs/architecture/PERSISTENCE.md`;
- `docs/architecture/DIAGNOSTICS.md`;
- `docs/architecture/ARCHITECTURE_GUARDRAILS.md`;
- `docs/development/TESTING.md`;
- `docs/development/DEFINITION_OF_DONE.md`;
- this plan.

Create focused docs:

- `docs/forge/FORGE_HOST.md`;
- `docs/forge/RESOURCE_LOADING.md`;
- `docs/forge/DEDICATED_SERVER_TESTING.md`;
- `docs/forge/GAMETEST.md`;
- `docs/forge/DISTRIBUTION_PACKAGING.md`;
- `docs/forge/DIAGNOSTIC_COMMANDS.md`.

## Final Status

Partially verified. Command tree, command tests, GameTest registration/execution, and production jar content checks
are implemented and locally verified. Normal dedicated server smoke and isolated production-jar server smoke remain
pending until a human accepts `story-forge-1.20.1/run/eula.txt`.
