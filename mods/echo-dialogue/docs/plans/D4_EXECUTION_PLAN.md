# D4 Execution Plan: Choices, Branching Outcomes, Story Result Mapping

## Статус prerequisite

Пользователь подтвердил D2-D3 acceptance перед началом D4. Автоматические проверки D3 adapter и Story-side generic
output binding уже проходили отдельно; D4 больше не блокируется prerequisite gate.

## Audit Summary

Echo Dialogue:

- текущая compiled model линейная: `CompiledDialogue` хранит `startNodeId` и ordered `CompiledDialogueLine`;
- `DialogueSessionState` хранит `lineIndex`, `phase`, `revision`;
- `DialoguePresentation` всегда LINE;
- `DialogueInput` содержит только `ADVANCE`/`CANCEL`;
- persistence schema V1 хранит `currentNodeId`, `lineOrdinal`, `terminalResult` string;
- Forge host уже соблюдает save-before-presentation через `ForgeDialogueSaveFlow`;
- network protocol `echo_dialogue:main` version `1` содержит open/close/advance;
- client overlay уже компактный, centered и не содержит подсказок;
- public `DialogueSessionView` возвращает terminal result как `Optional<String>`;
- D3 public service bridge уже есть: `dialogue-forge-api-1.20.1`, `DialogueService`, terminal observers.

Story-Dialogue Adapter:

- adapter независим и зависит только от public Story/Dialogue artifacts;
- action type остаётся `echo_dialogue:play`;
- current mapping отправляет `dialogue.terminalResult` и пока не отправляет canonical `outcome`;
- D4 меняет только terminal mapping: `DialogueOutcomeId` -> Story output `outcome`.

Echo Story Engine:

- generic output binding уже реализован отдельным Story plan `0009-generic-external-action-output-binding.md`;
- Story accepts `Action(..., output: "outcome")`;
- Story Runtime сохраняет bound value в existing variables;
- Story не получает `ChoiceId`, `DialogueOutcomeId` или `echo_dialogue` semantics.

## Scope Firewall

Echo Dialogue owns:

- `ChoiceId`;
- `DialogueOutcomeId`;
- dialogue node graph;
- server-authoritative selection;
- choice persistence/networking/UI;
- terminal outcome persistence/public API.

Adapter owns:

- mapping completed `DialogueOutcomeId` to Story output `outcome`;
- no branching, UI or persistence implementation.

Story owns:

- generic string output binding and branching through existing Story variables/conditions only.

Non-goals:

- no NPC, portraits, animation, voice, camera, movement locking, conditional choices, skill checks, typewriter,
  inventory/quest effects, generic UI framework or Story-specific code in Echo Dialogue.

## Stable IDs

Add contracts:

- `ChoiceId`: source-authored, non-empty, max bounded, grammar `[a-z][a-z0-9_.-]*`.
- `DialogueOutcomeId`: namespaced source-authored ID backed by `NamespacedId`.

Default legacy outcome:

```text
echo_dialogue:completed
```

## DialogueResource Syntax

Existing linear syntax remains valid.

Add supported calls inside `NodeDef` methods:

```csharp
Line("father", "Ты поможешь мне?");
Choice("accept", "Да, помогу.", next: "accepted");
Choice("refuse", "Нет.", next: "refused");
Complete("echo_story_dialogue_test:accepted");
```

Semantic rules:

- one `DialogueResource` class;
- one namespaced dialogue id;
- one start node;
- unique node ids;
- aliases resolve for every line;
- ordered lines are preserved;
- choices are ordered and must appear after lines;
- no lines after first choice;
- `Choice` requires id, visible text and named `next`;
- choice target must exist;
- choice IDs unique per dialogue;
- terminal `Complete` requires valid `DialogueOutcomeId`;
- choices and complete cannot coexist in one node;
- no content after `Complete`;
- legacy node with only lines and no choices/complete uses default outcome when the dialogue finishes.

## Compiled Model

Replace the linear-only model with immutable graph data while preserving linear convenience:

- `CompiledDialogue`: id, start node, ordered node list/map;
- `CompiledDialogueNode`: node id, ordered lines, ordered choices, optional terminal completion;
- `CompiledDialogueLine`: node id, line ordinal, speaker id, display name, text;
- `CompiledDialogueChoice`: `ChoiceId`, display text, target `DialogueNodeId`;
- `CompiledDialogueCompletion`: `DialogueOutcomeId`.

Compatibility accessors may expose `lines()` for legacy tests, but canonical runtime should use node lookup.

## Transition Model

Add explicit presentation kind:

- `DialoguePresentationKind.LINE`;
- `DialoguePresentationKind.CHOICE`.

Extend session state:

- current node id;
- line ordinal;
- presentation kind;
- optional completed outcome.

Input:

- `ADVANCE` valid only during LINE;
- `SELECT_CHOICE` valid only during CHOICE and carries `ChoiceId`;
- `CANCEL` valid for ACTIVE LINE/CHOICE.

Flow:

- start resolves first stable presentation from start node;
- advancing within node increments line ordinal;
- advancing past last line:
  - if node has choices, transition to CHOICE with revision increment;
  - if node has completion, terminal COMPLETED with outcome;
  - if legacy terminal, COMPLETED with `echo_dialogue:completed`;
- selecting a choice validates revision/session/choice id and moves to target node first stable presentation;
- duplicate/stale/wrong-session/wrong-kind input mutates nothing.

## Persistence Schema

Introduce Dialogue Save Schema Version 2.

Persist:

- schema version;
- session id, dialogue id, audience id;
- compiled fingerprint;
- phase, revision;
- current node id;
- presentation kind;
- line ordinal for LINE;
- terminal `DialogueOutcomeId` for COMPLETED;
- cancellation reason for CANCELLED;
- metadata;
- repository revision.

V1 migration:

- ACTIVE V1 -> V2 `LINE` using current node id and line ordinal;
- COMPLETED V1 -> V2 `COMPLETED` with `echo_dialogue:completed` unless V1 terminal result already contains a
  namespaced outcome;
- CANCELLED V1 -> V2 `CANCELLED`;
- unsupported/malformed presentation kind is rejected.

## Fingerprint

Canonical fingerprint must include:

- dialogue id;
- start node id;
- ordered node ids;
- ordered line speaker/display/text;
- ordered choice id/text/target;
- terminal outcome id.

Formatting, comments and source path remain ignored.

## Network Protocol

Bump channel protocol to `2`.

Clientbound open/update:

- session id;
- revision;
- presentation kind;
- LINE: speaker display name, text;
- CHOICE: ordered visible choices containing only `ChoiceId` and display text.

Serverbound:

- existing advance message remains for LINE;
- add select-choice message: session id, expected revision, selected `ChoiceId`;
- client never sends target node or outcome.

## Client UI

Use the existing centered compact overlay.

LINE:

- render `[Speaker]: text`;
- Space/LMB advance only for LINE.

CHOICE:

- render ordered choices sized to content;
- no keyboard/space auto-selection for D4 except optional number keys if implemented and tested;
- mouse click selects one visible choice;
- wait lock blocks double-click and stale repeated packet;
- outside click does nothing.

## Public API

Update public contracts:

- `DialogueSessionView` should expose `Optional<DialogueOutcomeId>` for completed sessions;
- terminal observers emit durable completed outcome after save;
- existing status/cancellation/blocked fields remain.

Adapter compatibility:

- adapter rebuilds against updated public artifacts;
- completed terminal mapping sets Story output `outcome` to `DialogueOutcomeId.toString()`;
- cancellation/failure mappings do not send success outcome.

## Testing Strategy

Plain JVM:

- valid/invalid `ChoiceId`;
- valid/invalid `DialogueOutcomeId`;
- compile valid choice graph with Russian text;
- duplicate choice id, empty choice text, missing target, choices plus complete, line after choice, content after complete;
- legacy linear default outcome;
- immutable ordered choices and deterministic lookup;
- final line enters CHOICE;
- valid choice enters target branch;
- invalid/stale/duplicate selection rejected;
- terminal outcome stored;
- fingerprint changes on choice/outcome changes;
- schema V2 line/choice/completed round-trip and V1 migration.

Forge/GameTest:

- D4 choice resource compiles;
- choice presentation saved before dispatch;
- accepted/refused branches store expected outcomes;
- invalid/stale/duplicate selection mutates nothing;
- choice recovery restores same ids/revision/options;
- terminal outcome remains queryable.

Adapter:

- explicit outcome maps to Story SUCCESS output `outcome`;
- legacy default outcome maps correctly;
- cancelled/blocked/failed do not produce success outcome;
- no implementation imports/bundled API copies.

Story:

- no new Dialogue-specific Story code;
- reuse existing generic output binding checks.

Manual acceptance remains user-run for visual client behavior and restart windows.

## Packaging

Echo Dialogue final JAR must include:

- updated contracts/core/persistence/echoscript/forge API/forge runtime;
- client choice UI and new packets;
- sample legacy resource plus D4 test/sample resource only if explicitly production sample.

It must not include:

- Story Engine implementation;
- adapter implementation;
- GameTest/JUnit/Forge/Minecraft classes;
- root EchoScript packages outside Jar-in-Jar;
- test-only acceptance content in production JAR unless explicitly intended as sample.

## Risks

- Current source-shape seven-file package limit may force responsibility splits in `core.model`, `core.session`,
  `persistence.codec`, `forge.network.message`, and `forge.client`.
- Changing `CompiledDialogue` constructor affects many tests and fixtures.
- Protocol version bump requires all clients and server to run matching D4 jar.
- Schema V2 migration must not corrupt existing V1 terminal records.
- `runGameTestServer` guard currently expects exactly eight tests and must be updated with D4 markers.

## Rollback

- No commits, branches, tags or push.
- Revert D4 by targeted patches to new/changed D4 files.
- Keep D1/D2 linear behavior covered by compatibility tests.
- If schema migration fails verification, stop before isolated runtime acceptance and report exact failing command.

## Verification Commands

Echo Dialogue:

```powershell
.\gradlew.bat projects --console=plain
.\gradlew.bat dependencies --console=plain
.\gradlew.bat test --console=plain
.\gradlew.bat architectureTest --console=plain
.\gradlew.bat check --console=plain
.\gradlew.bat build --console=plain
.\gradlew.bat sourceShapeCheck --console=plain
.\gradlew.bat checkWhitespace --console=plain
.\gradlew.bat :dialogue-forge-1.20.1:runGameTestServer --console=plain
.\gradlew.bat :dialogue-forge-1.20.1:verifyProductionJarContents --console=plain
git diff --check
```

Adapter:

```powershell
.\gradlew.bat test architectureTest check build sourceShapeCheck checkWhitespace --console=plain
.\gradlew.bat buildAdapterAgainstPublishedArtifacts --console=plain
.\gradlew.bat runGameTestServer --console=plain
```

Story Engine only if touched again:

```powershell
.\gradlew.bat check build sourceShapeCheck checkWhitespace --console=plain
git diff --check
```
