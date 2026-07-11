# Progress

## Current Milestone

Initial architecture foundation for Echo Story Engine.

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

## In Progress

- Final report.

## Next

- Decide whether to replace or remove inherited Forge `LICENSE.txt` and `CREDITS.txt` with project-specific
  legal/credit files. This audit did not change licensing.
- Add a dedicated server smoke test once a minimal server scenario exists.

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
- Current Forge repository adapter is volatile and proves the port only; durable persistence is intentionally deferred.
- No durable persistence adapter exists yet; current repository adapter is intentionally volatile.

## Intentional Technical Debt

- Durable SavedData/NBT persistence is not implemented in the initial scaffold.
- No command, GUI, networking, GameTest, or dedicated server smoke test exists yet.

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
