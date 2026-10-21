# D3 Execution Plan

## Цель

Создать маленький независимый Forge 1.20.1 adapter mod `echo_story_dialogue_adapter`, который регистрирует Story
extension `echo_story_dialogue_adapter:bridge`, обрабатывает единственный action type `echo_dialogue:play`, запускает
или переиспользует Echo Dialogue session с deterministic `DialogueSessionId`, наблюдает durable terminal result и
возвращает completion в Story Engine.

## Non-goals

- Нет choices, branching outcomes, NPC integration, entity speaker lookup, portraits, heads, animation, voice, sound,
  camera, movement locking, cinematics, quest/inventory/wardrobe integration или generic UI.
- Нет Story Engine implementation dependency.
- Нет Dialogue implementation dependency в adapter.
- Нет EchoScript dependency в adapter.
- Нет reflection, ServiceLoader, classpath scanning, mixins, Kotlin, Lombok или DI framework.
- Нет commits, branches, tags или push.

## Multi-repository audit summary

### Echo Story Engine

Repository: `D:\JDHSeason\mods\echostoryengine`.

Текущий `git status --short` показывает большой незакоммиченный M5 набор, включая `story-extension-api`,
`story-extension-forge-1.20.1`, `docs/extensions/` и `integration-tests/echo-story-extension-test-mod/`.

Модули:

- `story-contracts`
- `story-core`
- `story-definition`
- `story-echoscript-adapter`
- `story-extension-api`
- `story-extension-forge-1.20.1`
- `story-persistence`
- `story-forge-1.20.1`

Публичные Story Maven artifacts из текущего root publication task:

- `io.github.kxyr0.echostoryengine:story-contracts:0.1.0`
- `io.github.kxyr0.echostoryengine:story-extension-api:0.1.0`
- `io.github.kxyr0.echostoryengine:story-extension-forge-1.20.1:0.1.0`

Build-local repo: `../echostoryengine/build/integration-maven`.

Actual Story public types to use:

- `io.github.kxyr0.echostoryengine.story.api.identity.ActionExecutionId`
- `io.github.kxyr0.echostoryengine.story.api.identity.ExecutionId`
- `io.github.kxyr0.echostoryengine.story.api.identity.StoryId`
- `io.github.kxyr0.echostoryengine.story.api.identity.NamespacedId`
- `io.github.kxyr0.echostoryengine.story.api.command.ExternalActionResultCategory`
- `io.github.kxyr0.echostoryengine.story.extension.api.descriptor.StoryExtensionDescriptor`
- `io.github.kxyr0.echostoryengine.story.extension.api.descriptor.StoryExtensionApiVersion`
- `io.github.kxyr0.echostoryengine.story.extension.api.descriptor.ActionHandlerDescriptor`
- `io.github.kxyr0.echostoryengine.story.extension.api.descriptor.ActionHandlerCapabilities`
- `io.github.kxyr0.echostoryengine.story.extension.api.execution.StoryExtensionSession`
- `io.github.kxyr0.echostoryengine.story.extension.api.execution.StoryActionHandler`
- `io.github.kxyr0.echostoryengine.story.extension.api.execution.ActionStartRequest`
- `io.github.kxyr0.echostoryengine.story.extension.api.execution.ActionRecoveryRequest`
- `io.github.kxyr0.echostoryengine.story.extension.api.execution.ActionCancelRequest`
- `io.github.kxyr0.echostoryengine.story.extension.api.completion.ActionCompletion`
- `io.github.kxyr0.echostoryengine.story.extension.api.completion.ActionCompletionSink`
- `io.github.kxyr0.echostoryengine.story.extension.api.completion.ActionCompletionAcceptance`
- `io.github.kxyr0.echostoryengine.story.extension.api.outcome.ActionInvocationResult`
- `io.github.kxyr0.echostoryengine.story.extension.api.outcome.ActionInvocationStatus`
- `io.github.kxyr0.echostoryengine.story.extension.forge.EchoStoryExtensionRegistrationEvent`
- `io.github.kxyr0.echostoryengine.story.extension.forge.ForgeStoryExtensionRegistrar`
- `io.github.kxyr0.echostoryengine.story.extension.forge.ForgeStoryExtensionFactory`
- `io.github.kxyr0.echostoryengine.story.extension.forge.ForgeStoryExtensionContext`

Story contract observations:

- `ActionExecutionId.asString()` returns canonical `namespace:path`.
- `ActionStartRequest` and `ActionRecoveryRequest` carry `payload: Map<String, String>`.
- `ActionCancelRequest` carries no cancellation reason in current public API.
- `ActionCompletionSink.complete(ActionCompletion)` is thread-safe and returns acceptance only for queueing.
- `ForgeServerHost.applyExtensionCompletion` now forwards `ActionCompletion.outputs()` through neutral runtime input.
- Story runtime applies outputs only through the generic `Action(..., output: "...")` binding and stores the bound
  value in existing Story variables.
- D3 may send outputs, and Story-side durable consumption is covered by Echo Story Engine plan
  `docs/plans/0009-generic-external-action-output-binding.md`.
- No durable completion acknowledgement exists in Story M5; adapter must not acknowledge Dialogue terminal records
  based on sink acceptance.

### Echo Dialogue

Repository: `D:\JDHSeason\mods\echo-dialogue`.

Current `git status --short`: clean at audit time.

Модули:

- `dialogue-contracts`
- `dialogue-core`
- `dialogue-persistence`
- `dialogue-echoscript-adapter`
- `dialogue-forge-1.20.1`

Existing public Dialogue Maven artifacts: none published to build-local Maven at audit time.

Existing public Dialogue contract types:

- `io.github.kxyr0.echodialogue.api.identity.DialogueId`
- `io.github.kxyr0.echodialogue.api.identity.DialogueSessionId`
- `io.github.kxyr0.echodialogue.api.identity.AudienceId`
- `io.github.kxyr0.echodialogue.api.identity.NamespacedId`
- `io.github.kxyr0.echodialogue.api.identity.DialogueNodeId`
- `io.github.kxyr0.echodialogue.api.identity.SpeakerId`
- `io.github.kxyr0.echodialogue.api.diagnostic.DialogueDiagnostic`
- `io.github.kxyr0.echodialogue.api.diagnostic.DiagnosticCategory`
- `io.github.kxyr0.echodialogue.api.diagnostic.DiagnosticSeverity`
- `io.github.kxyr0.echodialogue.api.diagnostic.DiagnosticSubject`

Internal types that prove capability exists but must not be imported by adapter:

- `io.github.kxyr0.echodialogue.core.host.ActiveDialogueSessions`
- `io.github.kxyr0.echodialogue.core.session.DialogueSessionEngine`
- `io.github.kxyr0.echodialogue.core.session.DialogueSessionState`
- `io.github.kxyr0.echodialogue.core.session.DialogueSessionPhase`
- `io.github.kxyr0.echodialogue.core.persistence.DialogueSessionRepository`
- `io.github.kxyr0.echodialogue.core.persistence.DialogueCheckpoint`
- `io.github.kxyr0.echodialogue.core.persistence.DialogueRepositoryLoadResult`
- `io.github.kxyr0.echodialogue.core.persistence.DialogueRepositoryWriteResult`
- `io.github.kxyr0.echodialogue.core.persistence.DialogueRepositoryRevision`
- `io.github.kxyr0.echodialogue.forge.host.ForgeDialogueHost`

Dialogue API gaps for D3:

- no public start request with caller-provided `DialogueSessionId`;
- no public query by `DialogueSessionId`;
- no public cancel by `DialogueSessionId`;
- no public terminal result view;
- no public terminal observation/subscription;
- no public observation handle/unregister contract;
- no public server-scoped Forge service lookup event;
- no public service availability result for zero/duplicate providers;
- no build-local Maven publication for `dialogue-contracts`;
- no `dialogue-forge-api-1.20.1` module.

## Minimal Echo Dialogue public API additions

Preferred additions:

1. Keep neutral immutable value contracts in `dialogue-contracts`.

2. Add one public Forge bridge module:

```text
dialogue-contracts <- dialogue-forge-api-1.20.1
dialogue-forge-api-1.20.1 <- dialogue-forge-1.20.1
```

3. Publish these to `../echo-dialogue/build/integration-maven`:

- `io.github.kxyr0.echodialogue:dialogue-contracts:0.1.0-SNAPSHOT`
- `io.github.kxyr0.echodialogue:dialogue-forge-api-1.20.1:0.1.0-SNAPSHOT`

Planned neutral public packages:

- `io.github.kxyr0.echodialogue.api.session`: public session phase/status and immutable session view.
- `io.github.kxyr0.echodialogue.api.service`: service interface, start/query/cancel requests and results.
- `io.github.kxyr0.echodialogue.api.observation`: terminal event, observer and close handle.

Planned Forge API package:

- `io.github.kxyr0.echodialogue.forge.api`: synchronous server-scoped service lookup event.

The public API must expose no `ForgeDialogueHost`, repository implementation, compiled dialogue, `ServerPlayer`,
network packets, client overlay, path, Gson or EchoScript types.

## Canonical adapter identity

- mod id: `echo_story_dialogue_adapter`
- display name: `Echo Story Dialogue Adapter`
- Maven group: `io.github.kxyr0.echostorydialogueadapter`
- base package: `io.github.kxyr0.echostorydialogueadapter`
- version: `0.1.0-SNAPSHOT`
- extension id: `echo_story_dialogue_adapter:bridge`
- action type: `echo_dialogue:play`

## Action argument schema

Required `ActionStartRequest.payload()` and `ActionRecoveryRequest.payload()` keys:

- `dialogueId`: required Echo Dialogue `DialogueId` in `namespace:path` form.
- `audienceId`: required Echo Dialogue `AudienceId`; for Minecraft players use UUID string.

Unknown argument policy: reject with explicit bounded diagnostic/result. D3 does not add optional UI, portrait,
speaker entity, timeout or metadata arguments.

## Output schema

Adapter completion outputs:

- `dialogueSessionId`: deterministic `DialogueSessionId.toString()`.
- D3 baseline used `outcome`: `completed`.
- D4 maps public `DialogueOutcomeId` to canonical `outcome`.

Story-side durable consumption of `ActionCompletion.outputs` is covered by the generic Echo Story Engine
`Action(..., output: "...")` binding from `docs/plans/0009-generic-external-action-output-binding.md`. Adapter tests
prove the emitted completion payload; D4 covers explicit DialogueOutcomeId mapping to `outcome`.

## Deterministic DialogueSessionId mapping

Algorithm:

1. Canonical input: `ActionExecutionId.asString()`.
2. Namespace: fixed adapter-owned UUID literal documented in adapter code.
3. Hash: UUID-v5-style SHA-1 over namespace UUID bytes plus UTF-8 canonical input.
4. Set RFC 4122 version `5` and variant bits.
5. Public result: lowercase `UUID.toString()` wrapped in `DialogueSessionId`.

Properties:

- same `ActionExecutionId` gives same `DialogueSessionId` for start/recovery/cancel;
- different `ActionExecutionId` should not intentionally share a session;
- no time, random, object identity, player name, line index or Story revision input;
- mapping changes are persisted compatibility changes.

## Start flow

1. Receive `ActionStartRequest` on Story server thread.
2. Verify action type `echo_dialogue:play`.
3. Decode `dialogueId` and `audienceId`.
4. Derive deterministic `DialogueSessionId`.
5. Resolve current Echo Dialogue public service through Forge event for the current `MinecraftServer`.
6. Query derived session.
7. If NOT_FOUND: start using caller-provided `DialogueSessionId`, `DialogueId`, `AudienceId`, then register terminal
   observation.
8. If ACTIVE and ids match: register terminal observation idempotently.
9. If COMPLETED: submit Story success completion from durable terminal result.
10. If CANCELLED: submit mapped cancellation/failure.
11. If BLOCKED: reject or report blocked according to Story recovery context.
12. If mismatch: reject idempotency conflict and mutate nothing.

## Recovery flow

Recovery receives `ActionRecoveryRequest`, derives the same session id and repeats query:

- ACTIVE: validate ids, register observation, return accepted.
- COMPLETED: submit success completion from durable terminal result.
- CANCELLED: submit mapped cancellation/failure.
- BLOCKED: preserve Dialogue and Story WAITING; return rejected/blocked.
- NOT_FOUND: valid pre-dispatch crash window; start deterministic session using original payload.
- MISMATCH: reject conflict and mutate nothing.

## Cancellation flow

Current Story `ActionCancelRequest` provides `storyId`, `executionId`, `actionExecutionId` and `actionType`, but no
reason. Adapter maps cancellation by deterministic session id:

- ACTIVE: call public Dialogue cancel by `DialogueSessionId`.
- COMPLETED: do not replace success with cancellation.
- CANCELLED: idempotent already terminal.
- BLOCKED: cancel only if public Dialogue contract supports safe durable cancellation.
- NOT_FOUND: do not create a session.

## Terminal observation flow

- Track observations by `ActionExecutionId`.
- Duplicate registration for same action is idempotent.
- Terminal callback may repeat; duplicate Story completion remains harmless through Story completion queue.
- Observer callback must catch exceptions and must not mutate Story state directly.
- Close observation after terminal callback and during adapter session shutdown.
- Do not poll every server tick when public Dialogue observation exists.

## Terminal acknowledgement policy

Do not call Dialogue terminal acknowledgement automatically. Story M5 has no explicit durable-consumption acknowledgement.
Sink acceptance is only queue acceptance. Echo Dialogue D2 retention/cleanup remains authoritative.

## Publication strategy

Adapter root resolves public artifacts from sibling build-local Maven repositories:

```text
../echostoryengine/build/integration-maven
../echo-dialogue/build/integration-maven
```

Adapter root orchestration task will:

1. run Story `publishExtensionIntegrationArtifacts`;
2. run Dialogue public publication task;
3. validate expected coordinates;
4. build adapter against those artifacts;
5. verify adapter JAR contents.

No Maven Local-only acceptance, no remote publishing, no committed API JARs.

## Adapter production package plan

Planned packages:

- `...adapter.forge`: Forge entrypoint and Story extension registration.
- `...adapter.story`: descriptor, session and handler.
- `...adapter.dialogue`: public Dialogue service lookup and operation mapping.
- `...adapter.action`: action argument decoder and result mapping.
- `...adapter.identity`: deterministic session id mapper.
- `...adapter.observation`: bounded observation tracking.
- `...adapter.diagnostic`: stable bounded diagnostics.

Avoid vague names such as `AdapterManager`, `BridgeManager`, `CommonHandler`, `Utils`, `Helper` or `Impl`.

## Tests

Plain-JVM required focus:

- exact action type;
- valid/missing/invalid `dialogueId`;
- valid/missing/invalid `audienceId`;
- explicit unknown argument policy;
- deterministic id mapping stability;
- same/different `ActionExecutionId` mapping;
- start/recovery/cancel mapping over NOT_FOUND, ACTIVE, COMPLETED, CANCELLED, BLOCKED and mismatch;
- duplicate terminal callback;
- completion sink rejection;
- session close unsubscribes observers;
- no public method returns null.

Architecture tests:

- only public Story imports;
- only public Dialogue imports;
- no Story/Dialogue implementation imports;
- no EchoScript imports;
- no reflection, ServiceLoader or classpath scanning;
- no direct file I/O;
- no client overlay package;
- no API classes bundled in adapter JAR;
- package seven-file limits.

Forge/GameTest and acceptance:

- three production mods load;
- adapter visible in `/echo_story extensions`;
- `echo_dialogue:play` listed under extension;
- Dialogue service resolves through public bridge;
- full Story WAITING -> Dialogue ACTIVE -> Dialogue COMPLETED -> Story FINISHED;
- recovery windows A-D from D3 spec;
- isolated three-production-mod server and client;
- manual visual overlay remains user-pending unless observed directly.

## Risks

- Echo Dialogue public API additions are required before adapter production code can compile without implementation
  imports.
- Story completion outputs are consumed only through the generic Story output binding; adapter code must keep
  Dialogue-specific outcome mapping outside Story Engine.
- Public Dialogue service start may need server-scoped player lookup for online audience presentation while preserving
  D2 host lifecycle constraints.
- Async save-before-presentation means public start result must not claim presentation was already opened.
- Terminal acknowledgement must stay conservative to avoid losing completion across crash window.

## Rollback

- No commit/branch/tag/push.
- Revert D3 changes with targeted patches only.
- Keep Story and Dialogue accepted behavior intact.
- If public API gap blocks implementation, report the exact missing prerequisite and keep adapter production code out
  until the gap is closed.

## Verification plan

Run in adapter repository:

- `.\gradlew.bat projects`
- `.\gradlew.bat dependencies`
- `.\gradlew.bat test`
- `.\gradlew.bat architectureTest`
- `.\gradlew.bat check`
- `.\gradlew.bat build`
- `.\gradlew.bat sourceShapeCheck`
- `.\gradlew.bat checkWhitespace`
- `git diff --check`

Run public publication tasks in Story and Dialogue, adapter orchestration task, focused Dialogue API tests, Forge
GameTests, dedicated-server smoke, isolated three-mod server/client, acceptance content run, restart/recovery
acceptance and final adapter JAR inspection.

## Post-implementation status

Implemented:

- public Echo Dialogue service gap closed by `dialogue-contracts` additions and `dialogue-forge-api-1.20.1`;
- adapter Gradle/Forge project scaffolded as an independent repository root;
- canonical `echo_dialogue:play` handler implemented;
- start, recover, cancel and terminal observation mapped through public APIs;
- adapter build-local orchestration implemented and run successfully;
- JVM tests, architecture tests, source shape, whitespace and JAR content checks added;
- adapter Forge GameTest added and passed: `discovered=3`, `executed=3`, `passed=3`;
- adapter GameTest covers mod/command registration, public Dialogue terminal cancellation -> Story completion and
  terminal recovery after the initial observer is closed -> Story completion.

Known remaining acceptance:

- `git diff --check` cannot run until adapter root is initialized as a git repository or moved under one;
- Isolated three-production-mod server smoke passed on 2026-07-17 with Story/Dialogue Jar-in-Jar runtime artifacts.
- isolated three-production-mod client, acceptance-content server and full runtime restart acceptance still need explicit runtime execution;
- manual visual overlay acceptance remains user-run.
