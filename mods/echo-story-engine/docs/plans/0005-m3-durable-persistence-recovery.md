# 0005 M3 Durable Persistence And Recovery

## Goal

Implement M3: prove durable persistence and recovery for the M2 source-to-runtime lifecycle:

```text
valid source -> compiled immutable story -> deterministic runtime execution -> WAITING external action
-> durable save -> fresh runtime/repository -> durable load -> recovery request with the same ActionExecutionId
-> successful completion -> Finished -> final durable save
```

## Non-goals

- No M4 Forge host, world-directory binding, SavedData, Forge lifecycle listeners, commands, networking, GameTests,
  dedicated server smoke test, GUI, debug overlay, or asynchronous Forge scheduling.
- No M5 extension loading, plugin discovery, production third-party action adapters, or handler registry.
- No NPC, actor, navigation, pathfinding, vehicle, traffic, combat, animation, cinematic, magic, inventory, dialogue,
  editor, database, cloud save, encryption, compression, distributed locking, or event sourcing.
- No migration framework beyond rejecting unsupported schema versions.
- No commit.

## Current State

- M1 is committed as `da32de0 Complete M1 deterministic story runtime`.
- M2 source pipeline is implemented and verified in the current worktree, but not committed. This conflicts with the
  M3 prerequisite wording that M2 be committed, while this task also says not to create a commit. M3 will proceed on
  the current worktree and record this deviation.
- Current modules: `story-contracts`, `story-core`, `story-definition`, `story-forge-1.20.1`.
- Current persistence port is `ExecutionStateRepository` in `story-core`, with `Optional<StoryExecutionState> load`
  and `void save`. It cannot report load failures, save failures, or revision conflicts.
- Current `StoryRuntimeUseCase` exposes commands after calling `save`, but cannot suppress commands on save failure
  because `save` returns no result.
- Current duplicate-completion state is an unbounded set of completed action ids.
- Current Forge adapter is volatile and intentionally not durable.

## Constraints

- Preserve story-only scope.
- Keep `story-core` free of JSON, file paths, filesystem APIs, Gson, NBT, Forge, Minecraft, and logging frameworks.
- Keep runtime transition policy pure.
- Persist state before exposing external commands.
- Use explicit result values, not `null`, boolean-only results, or exceptions for expected persistence failures.
- Persist an explicit schema V1 outside core.
- Do not serialize domain objects directly through reflection.
- Keep all new classes within code-size, method-size, package-size, and dependency rules.

## Architecture Decision

Add one Java 17 module, `story-persistence`, for schema V1 snapshots, strict JSON codec, durable file repository,
definition compatibility checks, and recovery planning.

Dependency graph after M3:

```text
story-contracts <- story-core <- story-definition
story-contracts <- story-core <- story-persistence
story-contracts <- story-core <- story-forge-1.20.1
story-contracts <- story-forge-1.20.1
```

`story-definition` does not depend on `story-persistence`. `story-persistence` does not depend on `story-definition`.
`story-forge-1.20.1` does not need a production dependency on `story-persistence` in M3 because Forge host wiring is M4.

Refine `story-core`'s repository port to store an `ExecutionCheckpoint` with explicit load/save result types. The
checkpoint is a port boundary object, not the persisted JSON schema. It carries runtime state, compiled definition
fingerprint, and bounded pending-action recovery data.

Introduce `CompiledStoryFingerprint` in `story-core` as a deterministic SHA-256 fingerprint over canonical compiled
story meaning. Persist that value in every schema V1 snapshot.

## Snapshot Ownership

- `story-core` owns `ExecutionCheckpoint` as the storage port payload.
- `story-persistence` owns schema V1 persisted DTOs and JSON encoding.
- Runtime state remains `StoryExecutionState`.
- Source DTOs remain in `story-definition` and must not enter the save schema.

## Repository Port Ownership

`story-core` owns:

- `ExecutionStateRepository`
- `ExecutionCheckpoint`
- explicit load/save result and status types

The port exposes no `Path`, `File`, Gson, JSON tree, NBT, database, or Forge/Minecraft type.

## Durable Adapter Ownership

`story-persistence` owns:

- deterministic schema V1 codec;
- portable file repository with explicit root directory supplied by the caller;
- safe filename derivation;
- primary/backup/temp policy;
- persistence diagnostic catalog;
- recovery planner.

## Save Ordering

`StoryRuntimeUseCase.advance` will:

1. load checkpoint or create a new READY state;
2. call `RuntimeTransitionPolicy`;
3. build the next checkpoint;
4. save with expected previous revision;
5. return external commands only when save succeeds;
6. return no commands and emit persistence diagnostics when save fails or conflicts.

## Recovery Semantics

- WAITING restores as WAITING and produces one recovery command with the same `ActionExecutionId`.
- Repeating recovery for the same snapshot returns the same idempotency key.
- FINISHED and FAILED restore as terminal and emit no command.
- RUNNING is rejected at restore because M1 synchronous transitions should settle before durable save.
- READY restores without auto-starting.
- Missing or incompatible compiled story definitions reject restore.

## Definition Compatibility Policy

`CompiledStoryFingerprint` is deterministic for identical compiled meaning, independent of JVM identity, map order,
JSON field order, and file location. Restore requires exact fingerprint equality.

No story migration is implemented in M3. Different definitions reject restore.

## Schema Versioning Policy

M3 implements save schema version `1`. Future and older unsupported versions are rejected with structured diagnostics.
No fake migration chain is created.

## Crash Windows

- Crash after durable WAITING save but before command dispatch: reload emits recovery command with the original action id.
- Crash after dispatch but before completion save: reload emits the same recovery command again.
- Completion delivered twice: M1 duplicate-completion protection prevents double advancement.
- Save failure before dispatch: no command is exposed and previous durable state remains authoritative.
- Caller crash after observing no result: persisted state remains readable and retry uses revision checks.

## Failure Handling

Expected persistence failures return explicit result values and diagnostics. Corrupt snapshots never produce partially
restored state. Corrupt primary may fall back to one valid backup. Corrupt primary and backup return explicit failure.

## Security And Corruption Risks

- Execution ids are hashed into storage keys; raw ids are not used as paths.
- Full snapshots and absolute paths are not placed in diagnostics.
- One primary, one backup, and one temp file are used per execution.
- Stale temp files are never authoritative.

## Planned Production Types

- `story-core`: compiled fingerprint, checkpoint, load/save result/status types, persistence boundary diagnostics,
  updated repository port and runtime use case.
- `story-persistence`: schema V1 snapshot model, codec, file repository, diagnostics, recovery result/status,
  recovery planner.
- Forge volatile repository updated to the refined port.

## Planned Tests

- Core save-before-dispatch and save-failure suppression tests.
- Persistence success-path lifecycle tests with temporary directories.
- Crash-window tests.
- Corruption/failure tests for schema, primary/backup/temp, revisions, definition compatibility, invalid pending data.
- Architecture tests for module direction, no Forge/Minecraft, no Java serialization, no domain serialization
  annotations, no Path/File crossing core port, no DTO leakage.

## Verification

Required final commands:

- `.\gradlew.bat projects`
- `.\gradlew.bat test`
- `.\gradlew.bat architectureTest`
- `.\gradlew.bat check`
- `.\gradlew.bat build`
- `.\gradlew.bat checkWhitespace`
- `git diff --check`

Required scans:

- platform imports outside Forge;
- JSON/library leakage into core;
- serialization annotations/domain leakage;
- Java serialization APIs;
- Path/File in core port APIs;
- persistence DTOs in runtime APIs;
- source DTOs in persistence schema;
- mutable static fields;
- log file creation and raw absolute path logging;
- forbidden package names and stale MDK identifiers;
- TODO/FIXME;
- size/package limits;
- story-definition/story-persistence dependency direction;
- Gradle cycles.

## Rollback

Manual rollback through the current diff. Do not use destructive git commands without explicit user approval.

## Documentation Updates

Update `PROGRESS.md`, `README.md`, `PROJECT_MAP.md`, `ARCHITECTURE.md`, `DEPENDENCY_RULES.md`, `PORTABILITY.md`,
`DIAGNOSTICS.md`, `PERSISTENCE.md`, `TESTING.md`, ADR index, and this plan.

## Final Status

Completed.
