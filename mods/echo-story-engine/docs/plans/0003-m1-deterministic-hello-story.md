# 0003 M1 Deterministic Hello Story

## Goal

Implement milestone M1: one deterministic programmatic story definition that proves a complete Story Runtime vertical
slice without adding game-domain functionality.

## Non-goals

- No DSL, JSON parser, generic workflow framework, production persistence, networking, GUI, Forge commands,
  GameTests, integrations, NPC, navigation, vehicles, combat, animation, cinematics, reflection registration,
  or speculative extension points.
- No commit.
- No quality-gate weakening.

## Current State

- `story-contracts` exposes identifiers, runtime input/result/state, abstract command, and diagnostics.
- `story-core` has one simple transition policy that accepts a neutral input and advances to `runtime.accepted`.
- `story-forge-1.20.1` wires the runtime and logs diagnostics through a Forge adapter.
- Current production type counts are:
  - `story-contracts`: 12
  - `story-core`: 6
  - `story-forge-1.20.1`: 5

## Constraints

- Preserve dependency direction: `story-contracts <- story-core <- story-forge-1.20.1`.
- Keep `story-contracts` and `story-core` free of Forge, Minecraft, filesystem, networking, GUI, and logging backend.
- Runtime state must be immutable data, not threads, callbacks, futures, lambdas, handlers, platform objects,
  or global mutable state.
- External actions remain abstract namespaced commands.
- Missing action support fails only the affected execution and emits a structured diagnostic.
- Transition budget must stop accidental synchronous loops.

## Architecture Decision

Extend the current data-first runtime instead of introducing a framework:

- Contracts add explicit execution phase, action execution id, and action result category.
- Runtime input remains neutral and gains factory methods for start and external action completion inputs.
- Runtime state records variables, pending action id, completed action ids, phase, and revision as immutable data.
- Core adds a minimal programmatic story definition model with node kinds needed only for M1:
  start, set variable, condition, external action, and finish.
- `RuntimeTransitionPolicy` interprets the definition deterministically with a strict transition budget.

## Planned Files

- `story-contracts/src/main/java/.../StoryExecutionState.java`
- `story-contracts/src/main/java/.../StoryRuntimeInput.java`
- `story-contracts/src/main/java/.../command/ExternalCommand.java`
- new contract types under `.../identity` and `.../api`
- `story-core/src/main/java/.../domain/*`
- `story-core/src/main/java/.../application/StoryRuntimeUseCase.java`
- `story-core/src/test/java/.../application/*`
- `story-forge-1.20.1/src/main/java/.../composition/ForgeStoryCompositionRoot.java`
- docs: `docs/architecture/PROJECT_MAP.md`, `docs/architecture/ARCHITECTURE.md`, `PROGRESS.md`, this plan

## Steps

1. Add minimal contract data for phases and external action completion categories.
2. Add minimal core story definition/node model.
3. Replace the placeholder transition policy with deterministic sequence execution.
4. Update application use case wiring only as needed.
5. Update Forge composition so it still compiles without adding Forge behavior.
6. Add required core tests.
7. Update docs to describe actual M1 behavior.
8. Run required verification commands and fix failures without weakening gates.

## Implementation Notes

- Added explicit immutable execution phases and external action completion contracts.
- Added one programmatic story definition model with only the M1 node kinds.
- Added a deterministic transition policy with a strict synchronous transition budget.
- Added `RuntimeDiagnostics` as a focused catalog for stable runtime diagnostic codes and messages.
- Added story-core tests for happy path, waiting state, completion routing, duplicate and unknown completions,
  missing action support, deterministic replay, transition budget failure, and absence of Forge/Minecraft classes.

## Risks

- Too many node abstractions could become a generic workflow framework. Keep only M1 node kinds.
- Transition budget behavior can accidentally hide definition errors; tests must prove it fails the execution.
- Public contract expansion must remain Java-friendly and platform-neutral.
- `git diff --check` is weak in this no-commit repository, so `checkWhitespace` remains the effective text-file gate.

## Verification

- `.\gradlew.bat test`: passed.
- `.\gradlew.bat architectureTest`: passed.
- `.\gradlew.bat check`: passed.
- `.\gradlew.bat build`: passed.
- `.\gradlew.bat checkWhitespace`: passed.
- `git diff --check`: passed.
- Search contracts/core for Forge, Minecraft, logging backend, filesystem imports: passed.
- Count production types before and after:
  - before: `story-contracts` 12, `story-core` 6, `story-forge-1.20.1` 5;
  - after: `story-contracts` 15, `story-core` 10, `story-forge-1.20.1` 5.

## Rollback

Manual rollback through the current diff. Do not use destructive git commands without explicit user approval.

## Documentation Updates

Update `PROJECT_MAP.md`, `PROGRESS.md`, this plan, and architecture documentation only for the runtime behavior
actually introduced by M1.

## Final Status

Completed. M1 deterministic hello story is implemented and verified.
