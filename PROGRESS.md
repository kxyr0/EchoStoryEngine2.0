# Progress

## Current Milestone

M3: Durable Persistence & Recovery completed. Next milestone is M4 Forge persistence host integration.

## Completed

- Execution plan created: `docs/plans/0001-initial-architecture-foundation.md`.
- Gradle module split created: `story-contracts`, `story-core`, `story-forge-1.20.1`.
- Minimal story-only vertical slice implemented:
  neutral input -> state transition -> result + diagnostic -> Forge logging adapter.
- Existing MDK example gameplay/config code removed from active sources.
- Forge MDK example identity replaced with canonical Echo Story Engine identity.
- Documentation and ADR set created.
- Quality gates created: JUnit 5, ArchUnit, Checkstyle, PMD, source-shape check, whitespace check.
- Generated Forge runtime logs from verification were removed and future `logs` artifacts are ignored.
- Architecture acceptance audit completed:
  canonical identity applied, dependency graph verified, production type audit completed.
- M1 deterministic hello story implemented:
  start input -> variable write -> condition -> external action request -> Waiting -> completion -> Finished.
- External action completion categories, duplicate completion protection, missing action diagnostics,
  and transition budget failure are covered by story-core tests.
- M2 execution plan created: `docs/plans/0004-m2-story-source-pipeline.md`.
- M2 source pipeline ADR accepted: `docs/decisions/0011-story-source-pipeline-boundary.md`.
- `story-definition` module added for strict JSON source parsing, structural validation, semantic validation,
  source diagnostics, and compilation into core `StoryDefinition`.
- Runtime external action execution ids are now generated per execution instead of being authored in source or
  compiled nodes.
- M2 verification completed through root Gradle commands, explicit repository scans, and diff review.
- M3 execution plan created: `docs/plans/0005-m3-durable-persistence-recovery.md`.
- M3 durable persistence ADR accepted: `docs/decisions/0012-durable-persistence-recovery.md`.
- `story-persistence` module added for save schema V1, strict JSON checkpoint codec, durable file repository,
  persistence diagnostics, and recovery planning.
- `ExecutionStateRepository` now uses explicit checkpoint load/save result values and optimistic revision checks.
- Runtime save-before-dispatch ordering implemented: commands are exposed only after checkpoint save success.
- Durable recovery returns a WAITING recovery command with the same `ActionExecutionId`.
- Bounded duplicate-completion data now stores the last completed action id instead of an unbounded set.
- `CompiledStoryFingerprint` added as a neutral contracts value; core computes deterministic SHA-256 fingerprints
  from compiled story definitions.
- M3 tests cover file persistence success/failure paths, crash windows, recovery invariants, fingerprint stability,
  command dispatch suppression, and architecture boundaries.

## In Progress

- None.

## Next

- M4 Forge persistence host integration: select a world-bound root, lifecycle wiring, and server-thread scheduling
  policy for durable saves.
- Decide whether to replace or remove inherited Forge `LICENSE.txt` and `CREDITS.txt` with project-specific
  legal/credit files. This task did not change licensing.
- Add a dedicated server smoke test once a minimal server scenario exists.
- Decide the next milestone after M1 verification.

## Blockers

- None known.

## Accepted Decisions

- Canonical identity: `echo_story_engine`, `Echo Story Engine`, `io.github.kxyr0.echostoryengine`,
  `echo-story-engine`.
- Keep ForgeGradle only in `story-forge-1.20.1`.
- Use Java-only contracts/core for the initial scaffold.
- Use JUnit 5, ArchUnit, Checkstyle, PMD, and custom source-shape checks.

## Known Risks

- ForgeGradle may create local build/debug artifacts during verification; they must not be committed.
- Current Forge repository adapter is volatile and proves the port only; the durable file adapter is not wired into
  Forge yet.
- M3 file persistence is synchronous and portable; M4 must decide where and how Forge schedules durable file work.
- The M1 external action support list is explicit wiring, not a production adapter registry.

## Intentional Technical Debt

- Durable SavedData/NBT persistence is not implemented in Forge.
- No command, GUI, networking, GameTest, or dedicated server smoke test exists yet.
- No production external action handler dispatch, timeout scheduler, or durable completion inbox exists yet.
- Forge resource loading for source files is not implemented yet.
- Forge resource loading for source files and Forge world-bound runtime persistence are not implemented yet.

## Verification Status

- `.\gradlew.bat projects`: passed; root is `echo-story-engine` with three modules.
- `.\gradlew.bat :story-contracts:dependencies --configuration compileClasspath`: passed; no dependencies.
- `.\gradlew.bat :story-core:dependencies --configuration compileClasspath`: passed; depends on `story-contracts`.
- `.\gradlew.bat :story-forge-1.20.1:dependencies --configuration compileClasspath`: passed; depends on
  `story-core`, `story-contracts`, and Forge/Minecraft.
- `.\gradlew.bat test`: passed after canonical package rename.
- `.\gradlew.bat architectureTest`: passed.
- `.\gradlew.bat check`: passed.
- `.\gradlew.bat build`: passed; Forge jar artifact is `echo-story-engine-0.1.0.jar`.
- `.\gradlew.bat checkWhitespace`: passed.
- `git diff --check`: passed.
- Stale identity scan for old MDK identifiers: passed.
- contracts/core forbidden import scan: passed.
- non-build log artifact scan: passed.
- Dedicated server/GameTest: not run as a separate smoke test because no GameTest or server smoke scenario exists
  in the initial scaffold.
- M1 `.\gradlew.bat test`: passed.
- M1 `.\gradlew.bat architectureTest`: passed.
- M1 `.\gradlew.bat check`: passed.
- M1 `.\gradlew.bat build`: passed.
- M1 `.\gradlew.bat checkWhitespace`: passed.
- M1 `git diff --check`: passed.
- M1 contracts/core forbidden import scan: passed.
- M1 production type count after implementation: `story-contracts` 15, `story-core` 10,
  `story-forge-1.20.1` 5.
- M2 `.\gradlew.bat :story-definition:compileJava`: passed.
- M2 `.\gradlew.bat :story-definition:checkstyleMain`: passed.
- M2 `.\gradlew.bat :story-definition:checkstyleTest`: passed.
- M2 `.\gradlew.bat :story-definition:test`: passed.
- M2 `.\gradlew.bat :story-core:test`: passed.
- M2 `.\gradlew.bat sourceShapeCheck`: passed.
- M2 `.\gradlew.bat architectureTest`: passed.
- M2 `.\gradlew.bat projects`: passed; root now contains `story-contracts`, `story-core`, `story-definition`,
  and `story-forge-1.20.1`.
- M2 `.\gradlew.bat test`: passed.
- M2 `.\gradlew.bat check`: passed.
- M2 `.\gradlew.bat build`: passed; Forge jar build still succeeds.
- M2 `.\gradlew.bat checkWhitespace`: passed.
- M2 `git diff --check`: passed with line-ending warnings only.
- M2 forbidden Minecraft import scan outside Forge: passed.
- M2 forbidden Forge import scan outside Forge: passed.
- M2 JSON parser type scan in contracts/core: passed.
- M2 source DTO reference scan in contracts/core runtime code: passed.
- M2 log-file creation scan in production code: passed.
- M2 forbidden package-name scan: passed.
- M2 `examplemod` / `com.example` scan: passed.
- M2 TODO/FIXME scan: passed.
- M2 mutable static field rule: passed through ArchUnit; explicit static scan returned only static methods/factories.
- M2 production size scan: passed; no production type exceeds the 120 logical-line review threshold.
- M2 production type count: `story-contracts` 15, `story-core` 10, `story-definition` 16,
  `story-forge-1.20.1` 5.
- M3 `.\gradlew.bat projects`: passed; root now contains `story-contracts`, `story-core`, `story-definition`,
  `story-forge-1.20.1`, and `story-persistence`.
- M3 focused `.\gradlew.bat :story-core:compileJava :story-persistence:compileJava`: passed.
- M3 focused `.\gradlew.bat :story-persistence:test`: passed.
- M3 focused `.\gradlew.bat :story-core:test :story-persistence:test :story-definition:test
  :story-forge-1.20.1:test`: passed.
- M3 `.\gradlew.bat test`: passed.
- M3 `.\gradlew.bat architectureTest`: passed.
- M3 `.\gradlew.bat check`: passed.
- M3 `.\gradlew.bat build`: passed.
- M3 `.\gradlew.bat checkWhitespace`: passed.
- M3 `git diff --check`: passed with CRLF conversion warnings only.
- M3 `.\gradlew.bat sourceShapeCheck`: passed.
- M3 forbidden platform import scan outside Forge production code: passed.
- M3 JSON/storage API leakage scan in contracts/core: passed.
- M3 Java object serialization API scan: passed.
- M3 Path/File/JSON/NBT token scan in core port APIs: passed.
- M3 persistence DTO scan in contracts/core runtime APIs: passed.
- M3 source DTO scan in persistence module: passed.
- M3 mutable static field scan: passed.
- M3 log-file/raw absolute path production scan: passed.
- M3 TODO/FIXME scan: passed.
- M3 stale MDK identity production/docs scan: passed.
- M3 dependency reports passed: `story-definition` and `story-persistence` depend on `story-core`,
  `story-contracts`, and Gson; Forge depends on `story-core`, `story-contracts`, and Forge/Minecraft.
- M3 production type count: `story-contracts` 16, `story-core` 19, `story-definition` 16,
  `story-forge-1.20.1` 5, `story-persistence` 19.
