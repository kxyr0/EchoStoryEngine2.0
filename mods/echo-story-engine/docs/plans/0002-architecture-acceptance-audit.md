# 0002 Architecture Acceptance Audit

## Goal

Accept the initial scaffold architecture after replacing all Forge MDK example identity with the canonical
Echo Story Engine identity.

## Non-goals

- No new Story Engine functionality.
- No persistence, DSL, parser, networking, commands, GUI, integrations, NPC, navigation, vehicles, combat,
  animation, or cinematics.
- No quality-gate weakening.
- No commit.

## Current State

- The scaffold has three Gradle modules: `story-contracts`, `story-core`, `story-forge-1.20.1`.
- Several files still used Forge MDK example identity before this audit.
- Existing checks passed before this audit, but they validated the pre-rename structure.

## Constraints

- Preserve the existing architecture direction: `story-contracts <- story-core <- story-forge-1.20.1`.
- Keep production type count within the initial scaffold guidance.
- Do not add or remove features just to make the audit look larger.
- Update documentation only where structure or identity changed.

## Architecture Decision

Rename project identity to:

- Mod ID: `echo_story_engine`
- Display name: `Echo Story Engine`
- Maven group: `io.github.kxyr0.echostoryengine`
- Base Java package: `io.github.kxyr0.echostoryengine`
- Artifact name: `echo-story-engine`
- Root project name: `echo-story-engine`

Keep the same module split and production type responsibilities unless the audit finds a speculative abstraction.

## Planned Files

- Gradle identity files: `settings.gradle`, `gradle.properties`, `story-forge-1.20.1/build.gradle`.
- Java package paths under all three modules.
- Tests and architecture tests.
- Docs that reference old package paths or old identity.
- `PROGRESS.md`, `README.md`, and `docs/architecture/PROJECT_MAP.md`.
- ADR links that reference package paths.

## Steps

1. Rename Gradle identity and artifact configuration.
2. Rename Java packages and update source/test imports.
3. Replace story namespace fixtures and stale docs metadata.
4. Audit production types for placeholders/speculation/duplicates.
5. Verify dependency graph and stale identifier searches.
6. Run required checks.
7. Record final audit status in `PROGRESS.md`.

## Risks

- Manual package rename can leave stale paths in ADR links or tests.
- `git diff --check` is weak in a no-commit repository because most files are untracked, so `checkWhitespace` must
  be used as the project text-file whitespace gate.
- ForgeGradle may recreate ignored runtime logs during verification.

## Verification

- `.\gradlew.bat projects`
- `.\gradlew.bat test`
- `.\gradlew.bat architectureTest`
- `.\gradlew.bat check`
- `.\gradlew.bat build`
- `.\gradlew.bat checkWhitespace`
- `git diff --check`
- Repository searches for stale identity and forbidden dependency leaks.

## Rollback

Manual rollback through the current diff. Do not use destructive git commands without explicit user approval.

## Documentation Updates

Update `PROJECT_MAP.md`, `PROGRESS.md`, `README.md`, and affected ADR links after the final package/artifact layout
is verified.

## Final Status

Implemented.

Verification completed:

- `.\gradlew.bat projects`: passed.
- `.\gradlew.bat :story-contracts:dependencies --configuration compileClasspath`: passed, no dependencies.
- `.\gradlew.bat :story-core:dependencies --configuration compileClasspath`: passed, depends on `story-contracts`.
- `.\gradlew.bat :story-forge-1.20.1:dependencies --configuration compileClasspath`: passed, depends on
  `story-core`, `story-contracts`, and Forge/Minecraft.
- `.\gradlew.bat test`: passed.
- `.\gradlew.bat architectureTest`: passed.
- `.\gradlew.bat check`: passed.
- `.\gradlew.bat build`: passed.
- `.\gradlew.bat checkWhitespace`: passed.
- `git diff --check`: passed.
- Stale identity and forbidden dependency scans passed.

Audit result:

- No production type was empty, speculative-only, duplicated, or named with a forbidden vague name.
- No production type was removed or merged.
- `README.txt` and `changelog.txt` were removed as stale Forge MDK distribution documents.
- `LICENSE.txt` and `CREDITS.txt` were left unchanged because this task did not authorize a license/notice decision.
