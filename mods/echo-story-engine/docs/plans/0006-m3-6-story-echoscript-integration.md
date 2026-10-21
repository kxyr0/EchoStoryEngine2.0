# 0006 M3.6 Story-EchoScript Integration

## Goal

Implement a replaceable Story-owned source compiler that consumes published EchoScript `.es` source, binds only the
StoryResource profile, compiles into the existing immutable `StoryDefinition`, and proves the compiled story still
executes and recovers through M1-M3 runtime and persistence.

## Non-goals

- No changes to the separate EchoScript repository.
- No copied EchoScript source, committed EchoScript jars, Git submodules, absolute filesystem dependencies, reflection,
  classpath scanning, or optional runtime loading.
- No DialogueResource, NPC, UI, networking, Forge resource loading, datapack loading, commands, GameTests, dedicated
  server smoke, Minecraft resource lifecycle, or concrete gameplay systems.
- No persistence schema bump and no runtime snapshot storage of `.es` source or EchoScript AST.
- No commit.

## Current State

- `story-definition` owns the neutral source DTOs, validators, diagnostics, compiler interface, and
  `SourceStoryCompiler` into `StoryDefinition`.
- `story-echoscript-adapter` is the current production frontend for `.es` StoryResource source.
- The former strict JSON source production frontend and Gson dependency were removed from `story-definition`.
- `story-core` owns the compiled model, runtime transition policy, deterministic fingerprint, and repository port.
- `story-persistence` owns save schema V1, file persistence, and recovery planning.
- `story-forge-1.20.1` has no production dependency on source compilation or durable file persistence yet.
- EchoScript is consumed from Maven local as
  `io.github.kxyr0.echoscript:echo-script-language:0.1.0-SNAPSHOT`.

## Constraints

- Keep EchoScript as an external Maven dependency and centralize its version in `gradle.properties`.
- Use `mavenLocal()` only as the local development source for the snapshot; production and CI need a reproducible Maven
  repository containing the same coordinates.
- Keep EchoScript types out of Story public APIs, compiled stories, persistence DTOs, and runtime APIs.
- Keep Gson out of `story-definition` once JSON source production code is removed.
- Keep `story-core`, `story-definition`, and `story-persistence` independent of `story-echoscript-adapter`.
- Keep M3 compatibility based on compiled meaning fingerprint only.

## Architecture Decision

Add one Java 17 module, `story-echoscript-adapter`, with this dependency direction:

```text
story-contracts <- story-core <- story-definition <- story-echoscript-adapter
story-contracts <- story-core <- story-persistence
story-contracts <- story-core <- story-forge-1.20.1
```

`story-echoscript-adapter` depends on `story-definition` and the published EchoScript language artifact using
`implementation`. Story Engine public APIs expose only `StorySourceInput`, `StoryCompilationResult`,
`StoryDiagnostic`, and `StoryDefinition`.

## StoryResource Profile

M3.6 supports one declarative profile:

- resource class base type must be exactly `StoryResource`;
- `public override StoryResourceDef Build()` must return `Story("namespace:path", start: "node")`;
- `private void Nodes()` declares nodes using only these intrinsics:
  - `Start("id", next: "id")`;
  - `Set("id", variable: "name", value: "value-or-bool", next: "id")`;
  - `Require("id", variable: "name", value: "value-or-bool", next: "id")`;
  - `Action("id", type: "namespace:path", next: "id")`;
  - `Finish("id")`.

The adapter reads declarations in deterministic phases, not textual execution order. Method order and independent
declaration order must not affect compiled meaning.

## Semantic Binding Design

1. Parse source through EchoScript V1.
2. Adapt EchoScript diagnostics into `StoryDiagnostic` with original language code, source id, line, column, context,
   and hint.
3. Stop on syntax/language errors to avoid semantic cascades.
4. Validate the class profile and required signatures.
5. Bind allowlisted StoryResource intrinsics into neutral `SourceStory` and `SourceNode` values.
6. Reuse `StructuralStoryValidator`, `SemanticStoryValidator`, and the neutral compiler into `StoryDefinition`.
7. Return a `StoryCompilationResult`; invalid source never returns a compiled story.

## JSON Migration

Remove strict JSON source production code and Gson from `story-definition`. Keep persistence JSON in
`story-persistence`. Keep neutral source DTOs because they are Story-owned semantic input, not JSON-specific data.
Migrate source pipeline tests from JSON frontend behavior to `.es` adapter behavior.

## Fingerprint And Persistence Compatibility

The adapter produces the existing `StoryDefinition`. `StoryDefinitionFingerprint` remains the compatibility authority.
Whitespace, comments, LF/CRLF, source identity, and independent declaration order are excluded from fingerprints
because they do not appear in the compiled model. Incompatible semantic changes still change the fingerprint and
therefore reject M3 recovery.

## Planned Files

- `settings.gradle`, `build.gradle`, `gradle.properties`.
- `story-definition/build.gradle` and source compiler boundary classes.
- New `story-echoscript-adapter/build.gradle`.
- New adapter production code under `story-echoscript-adapter/src/main/java/.../echoscript`.
- Adapter fixtures and tests under `story-echoscript-adapter/src/test`.
- Architecture tests in `story-echoscript-adapter` plus updates to existing architecture tests.
- Documentation: README, PROGRESS, architecture docs, testing docs, this plan.

## Risks

- EchoScript is a local Maven snapshot today; CI must publish or resolve the same coordinates reproducibly.
- The StoryResource profile is deliberately narrow and rejects many syntactically valid EchoScript programs.
- Binding diagnostics can become noisy; syntax failures stop semantic binding and semantic binding stops before graph
  validation when it has already found profile/signature/intrinsic errors.
- Removing the JSON frontend requires updating older M2 tests without weakening validator/compiler coverage.

## Verification

Final commands run:

- `.\gradlew.bat projects`: passed.
- `.\gradlew.bat :story-echoscript-adapter:dependencies --configuration runtimeClasspath`: passed.
- `.\gradlew.bat :story-definition:dependencies --configuration runtimeClasspath`: passed.
- `.\gradlew.bat :story-persistence:dependencies --configuration runtimeClasspath`: passed.
- `.\gradlew.bat test`: passed.
- `.\gradlew.bat architectureTest`: passed.
- `.\gradlew.bat check`: passed.
- `.\gradlew.bat build`: passed.
- `.\gradlew.bat checkWhitespace`: passed.
- `git diff --check`: passed with LF/CRLF conversion warnings only.

Final scans passed for forbidden platform imports, EchoScript production leakage outside the adapter, copied EchoScript
source, committed jars, absolute Gradle paths, JSON source frontend leftovers, persistence JSON isolation, mutable
static fields, direct logging APIs, Dialogue semantics, TODO/FIXME, dependency graph, source shape, and package-size
limits.

## Rollback

Manual rollback through the current diff. Do not use destructive git commands without explicit user approval.

## Documentation Updates

Update `PROGRESS.md`, `README.md`, `PROJECT_MAP.md`, `ARCHITECTURE.md`, `DEPENDENCY_RULES.md`, `PORTABILITY.md`,
`PERSISTENCE.md`, `DIAGNOSTICS.md`, `TESTING.md`, and this plan after implementation and verification.

## Final Status

Complete. M3.6 is implemented without modifying the separate EchoScript repository and without committing changes.
