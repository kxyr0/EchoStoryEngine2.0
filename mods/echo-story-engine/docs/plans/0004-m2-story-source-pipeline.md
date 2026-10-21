# 0004 M2 Story Source Pipeline

## Goal

Implement milestone M2: source definition -> parsing -> structural validation -> semantic validation -> compilation
into the existing immutable runtime-compatible story model -> execution by the deterministic M1 runtime.

## Non-goals

- No M3 persistence, runtime save/load, NBT, SavedData, filesystem watching, restart recovery, or migrations.
- No M4 Forge resource loading, datapack reload integration, networking, commands, GUI, GameTests, or server smoke.
- No M5 extension loading, plugin discovery, reflection registration, classpath scanning, or handler registry.
- No NPC, actor, navigation, vehicle, combat, animation, cinematic, magic, editor, or third-party mod logic.
- No custom DSL, JSON5, YAML, XML, scripting, embedded Java, Kotlin, Lombok, mixins, or DI framework.
- No commit.

## Current State

- M1 is committed as `da32de0 Complete M1 deterministic story runtime`.
- Current working tree is clean before M2 changes.
- `story-contracts` owns stable API/data contracts and structured diagnostic contracts.
- `story-core` owns the deterministic runtime, ports, diagnostics buffer, and M1 `StoryDefinition` model.
- `story-forge-1.20.1` owns Forge entrypoint, composition, logging/time/state adapters, and a programmatic hello story.
- `StoryNode.externalAction` currently carries a static `ActionExecutionId`; M2 source must not author runtime operation ids.

## Constraints

- Preserve story-only scope from `docs/architecture/SCOPE.md`.
- Preserve dependency direction and acyclic modules.
- `story-contracts` and `story-core` must not import JSON, Forge, Minecraft, filesystem, networking, GUI, or logging APIs.
- Runtime must execute only immutable compiled `StoryDefinition` instances, never JSON, source DTOs, or partial validation data.
- Invalid source must not produce executable compiled output.
- Diagnostics must reuse `StoryDiagnostic` and must not log directly or create log files.
- Structural and semantic validation should collect independent issues when deterministic and safe.

## Architecture Decision

Add one Java 17 module, `story-definition`, for source DTOs, strict JSON parsing, structural validation,
semantic graph validation, and compilation into `story-core`'s `StoryDefinition`.

Dependency graph after M2:

```text
story-contracts <- story-core <- story-definition
story-contracts <- story-core <- story-forge-1.20.1
story-contracts <- story-forge-1.20.1
```

`story-forge-1.20.1` will not depend on `story-definition` in M2 because no production Forge resource loading is
implemented yet. M4 can add that edge when a platform adapter has a concrete use.

Strict JSON is the first source adapter. Java 17 has no standard JSON parser, so `story-definition` adds Gson as an
implementation-scoped adapter dependency. Gson types must not appear in contracts, core, compiled representation,
runtime state, or public extension contracts.

`StoryDefinition` remains the one compiled runtime model. M2 hardens it as an immutable compiled representation
rather than creating parallel `ValidatedStoryDefinition`, `CompiledStoryDefinition`, or runtime duplicate types.

## Source Ownership

- `story-definition` owns source input identity, source DTOs, JSON parsing, validation result types, diagnostics
  catalog entries for source failures, source fixtures, and source pipeline tests.
- `story-core` owns immutable runtime nodes and deterministic execution.
- `story-contracts` owns shared identifiers, runtime input/output, commands, execution state, and diagnostics.
- `story-forge-1.20.1` remains platform composition and infrastructure only.

## Parser Boundary

- Parser accepts in-memory source content and source identity.
- Parser reports malformed JSON syntax with source location when Gson exposes it.
- Parser converts JSON into immutable source DTOs with optional raw fields and no runtime execution.
- Parser performs no Forge I/O, filesystem I/O, logging, semantic validation, or compilation.

## Validation Stages

Structural validation checks source shape:

- required root fields;
- supported story source format version;
- valid namespaced story id;
- valid start and node ids;
- non-empty node collection;
- duplicate node ids;
- supported node kind;
- required fields for start, set-variable, condition, external-action, and finish nodes;
- forbidden unexpected field combinations where practical.

Semantic validation checks graph meaning:

- start node exists;
- transition targets exist;
- finish is reachable;
- unreachable nodes are reported;
- synchronous cycles are rejected;
- action type ids are syntactically valid;
- handler availability is not required for source compilation.

## Diagnostic Strategy

- Reuse `StoryDiagnostic`.
- Add `ESE-SOURCE-*` codes for syntax, version, missing fields, invalid ids, duplicate nodes, missing start,
  missing targets, unreachable nodes, synchronous cycles, unsupported node types, invalid fields, and compilation
  invariant failures.
- Include source id, story id, node id, source line, and source column when available through diagnostic subject
  fields and bounded context.
- Treat expected invalid source as result diagnostics, not uncaught exceptions or direct logs.

## Steps

1. Completed: add ADR for the source module, strict JSON adapter, and source/compiled boundary.
2. Completed: add `story-definition` Gradle module and Gson dependency scoped to that module.
3. Completed: refactor core external-action nodes so action execution ids are generated per execution, not authored.
4. Completed: add immutable source input/model types and parse result.
5. Completed: add JSON parser.
6. Completed: add source diagnostic catalog.
7. Completed: add structural and semantic validators with explicit result values.
8. Completed: add compiler and pipeline result values.
9. Completed: add test fixtures and focused parser/validator/compiler/runtime tests.
10. Completed: add architecture coverage for definition/core/JSON/source boundaries.
11. Completed: update architecture, dependency, diagnostics, testing, README, project map, and progress docs.
12. Completed: run required verification commands and repository scans.

## Implementation Notes

- `StoryDefinition` remains the single runtime-compatible compiled model.
- `StoryNode` no longer stores `ActionExecutionId`; `RuntimeTransitionPolicy` derives action execution ids from the
  current execution id and external-action node id.
- External action payload arguments are immutable string maps in compiled `StoryNode`.
- `story-definition` owns `StorySourcePipeline`, `JsonStorySourceParser`, validators, source compiler, source DTOs,
  and `ESE-SOURCE-*` diagnostics.
- Unreachable nodes are warnings; all other implemented source diagnostics are errors.
- `story-forge-1.20.1` does not depend on `story-definition` yet because production Forge resource loading remains M4.

## Planned Files

- `settings.gradle`
- `gradle.properties`
- `build.gradle`
- `story-core/src/main/java/.../domain/StoryDefinition.java`
- `story-core/src/main/java/.../domain/StoryNode.java`
- `story-core/src/main/java/.../domain/RuntimeTransitionPolicy.java`
- `story-core/src/test/java/.../application/*`
- `story-forge-1.20.1/src/main/java/.../composition/ForgeStoryCompositionRoot.java`
- `story-forge-1.20.1/src/test/java/.../ArchitectureRulesTest.java`
- `story-definition/build.gradle`
- `story-definition/src/main/java/.../definition/**`
- `story-definition/src/test/java/.../definition/**`
- `story-definition/src/test/resources/**`
- `docs/decisions/0011-story-source-pipeline-boundary.md`
- `docs/decisions/README.md`
- `docs/architecture/ARCHITECTURE.md`
- `docs/architecture/DEPENDENCY_RULES.md`
- `docs/architecture/PROJECT_MAP.md`
- `docs/development/TESTING.md`
- `README.md`
- `PROGRESS.md`
- this plan

## Risks

- Source pipeline can grow too broad; M2 must keep only M1-proven node behavior.
- Gson must stay isolated to `story-definition`.
- Validation can produce noisy cascades; semantic validation will run only after structural success.
- M1 programmatic Forge composition still exists until M4; it must not become resource loading.
- The new module increases architecture-test scope and must be included in root verification tasks.

## Verification

- `.\gradlew.bat :story-definition:compileJava`: passed.
- `.\gradlew.bat :story-definition:checkstyleMain`: passed.
- `.\gradlew.bat :story-definition:checkstyleTest`: passed.
- `.\gradlew.bat :story-definition:test`: passed.
- `.\gradlew.bat :story-core:test`: passed.
- `.\gradlew.bat sourceShapeCheck`: passed.
- `.\gradlew.bat projects`: passed.
- `.\gradlew.bat test`: passed.
- `.\gradlew.bat architectureTest`: passed.
- `.\gradlew.bat check`: passed.
- `.\gradlew.bat build`: passed.
- `.\gradlew.bat checkWhitespace`: passed.
- `git diff --check`: passed with line-ending warnings only.
- Minecraft imports outside Forge module scan: passed.
- Forge imports outside Forge module scan: passed.
- JSON parser types inside `story-core` / `story-contracts` scan: passed.
- Source DTO references inside `story-core` / `story-contracts` scan: passed.
- Mutable static fields: ArchUnit rule passed; explicit static scan returned only static methods/factories.
- New log file creation or file-writing diagnostics scan: passed.
- Forbidden package names scan: passed.
- `examplemod` / `com.example` scan: passed.
- `TODO` / `FIXME` scan: passed.
- Production size scan: passed; no production file exceeds the 120 logical-line review threshold.

## Rollback

Manual rollback through the current diff. Do not use destructive git commands without explicit user approval.

## Documentation Updates

Update `README.md`, `PROGRESS.md`, `docs/architecture/ARCHITECTURE.md`,
`docs/architecture/DEPENDENCY_RULES.md`, `docs/architecture/PROJECT_MAP.md`, `docs/development/TESTING.md`,
ADR index, and this plan to describe only implemented M2 behavior.

## Final Status

Completed. M2 source parsing, validation, diagnostics, compilation, runtime execution coverage, documentation, and
verification are implemented without creating a commit.
