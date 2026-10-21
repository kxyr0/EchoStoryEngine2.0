# 0001 Initial Architecture Foundation

## Goal

Create the first Echo Story Engine architecture foundation: documentation, accepted ADRs,
automated guardrails, and a minimal compiling scaffold for Minecraft Forge 1.20.1 on Java 17.

## Non-goals

- No full story DSL, parser, editor, GUI, networking protocol, production persistence, or plugin discovery.
- No NPC, combat, navigation, vehicle, magic, cinematic, world generation, or third-party mod logic.
- No Kotlin, Lombok, DI framework, mixins, or extra language loader.
- No commit, branch, tag, push, or license change.

## Current State

- Repository is a fresh Forge MDK-style tree with no commits on `main`.
- All current files are untracked.
- The repository initially contained Forge MDK example identity and example gameplay/config code.
- Current scaffold identity is canonical Echo Story Engine identity after plan `0002`.
- Current Gradle build is a three-module project with ForgeGradle isolated to `story-forge-1.20.1`.

## Constraints

- Use Minecraft Java Edition 1.20.1, Forge 1.20.1, Java 17, and the existing Gradle wrapper.
- Use the canonical mod id, display name, Maven group, artifact name, root project name, and base package.
- Keep story-contracts and story-core free of Minecraft, Forge, filesystem, networking, GUI, and logging framework dependencies.
- Apply ForgeGradle only to the Forge platform module.
- Keep production dependencies minimal and documented.
- Keep the scaffold small, concrete, and testable.

## Architecture Decision

Create three Gradle modules:

```text
story-contracts <- story-core <- story-forge-1.20.1
```

- `story-contracts`: Java-only public API/SPI/value contracts.
- `story-core`: Java-only runtime/application/domain logic and ports.
- `story-forge-1.20.1`: Forge entrypoint, composition root, and platform adapters.

The minimal vertical slice is:

```text
neutral runtime input -> core state transition -> result + diagnostic -> Forge diagnostic sink
```

## Planned Files

- Root build/config: `settings.gradle`, `build.gradle`, `gradle.properties`,
  `.gitignore`, `config/checkstyle/checkstyle.xml`, `config/pmd/ruleset.xml`.
- Documentation: `AGENTS.md`, `PLANS.md`, `PROGRESS.md`, `README.md`,
  `docs/**`.
- ADRs: `docs/decisions/0001-*.md` through `0010-*.md`.
- Contracts module: `story-contracts/build.gradle`, `story-contracts/src/main/java/**`.
- Core module: `story-core/build.gradle`, `story-core/src/main/java/**`,
  `story-core/src/test/java/**`.
- Forge module: `story-forge-1.20.1/build.gradle`, `story-forge-1.20.1/src/main/java/**`,
  `story-forge-1.20.1/src/main/resources/**`.
- Remove obsolete MDK example code from root `src/main/java` after replacement.

## Steps

1. Create documentation skeleton and ADR set.
2. Convert Gradle to the three-module structure.
3. Replace MDK example gameplay/config code with story-only scaffold.
4. Add the public API, core runtime use case, ports, diagnostics, and Forge adapters.
5. Add JUnit 5 behavior tests and ArchUnit architecture tests.
6. Add Checkstyle, PMD, source-shape checks, and whitespace checks.
7. Run verification commands and fix failures.
8. Update `PROJECT_MAP.md` and `PROGRESS.md` to match the final state.
9. Review the diff against the scope firewall and definition of done.

## Risks

- ForgeGradle may be sensitive to multi-project configuration.
- Static analysis rules can be too broad for records or Forge entrypoint classes.
- Full server/GameTest execution may not be available in the local environment.
- ForgeGradle may create ignored runtime logs during verification.

## Verification

Run the available Windows wrapper commands:

- `.\gradlew.bat check`
- `.\gradlew.bat test`
- `.\gradlew.bat architectureTest`
- `.\gradlew.bat build`
- `git diff --check`

Dedicated server/GameTest will be attempted only if the environment is prepared enough to run it reliably.

## Rollback

Because there are no commits yet, rollback is manual: remove the new modules/docs/config and restore the original
single-project MDK files from the current diff. Do not use destructive git reset commands without an explicit request.

## Documentation Updates

Update `docs/architecture/PROJECT_MAP.md` after the final module/package layout exists.
Update `PROGRESS.md` only with checks that actually ran.

## Final Status

Implemented.

Verification completed:

- `.\gradlew.bat test`: passed.
- `.\gradlew.bat architectureTest`: passed.
- `.\gradlew.bat check`: passed.
- `.\gradlew.bat build`: passed.
- `git diff --check`: passed.

Deviation found during implementation:

- Architecture tests that inspect Forge adapters were moved from `story-core` tests to `story-forge-1.20.1` tests.
  This keeps the Gradle dependency direction correct because Forge can see core/contracts, while core cannot see Forge.
- `dependencyResolutionManagement.repositoriesMode = FAIL_ON_PROJECT_REPOS` was not used because ForgeGradle adds
  required project repositories during userdev setup.
- `-Xlint:all` is applied to pure Java modules only. Forge userdev compilation expands external Minecraft/Forge
  sources and produced irrelevant external warnings; Checkstyle, PMD, and ArchUnit still run for the Forge module.
