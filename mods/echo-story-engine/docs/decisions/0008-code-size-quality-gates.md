# 0008 Code Size And Quality Gates

## Status

Accepted.

## Context

Architecture rules must fail the build when violated. Markdown alone is not enough.

## Decision

Use JUnit 5, ArchUnit, Checkstyle, PMD, JaCoCo reports, `sourceShapeCheck`, and `checkWhitespace`.

## Alternatives

- Rely on manual review only. Rejected because boundaries must be executable.
- Add many overlapping tools. Rejected because the scaffold should stay simple.

## Consequences

`.\gradlew.bat check` is the main quality entrypoint. Tool versions are centralized in `gradle.properties`.

## Enforcement

Root and module Gradle tasks.

## Links

- `config/checkstyle/checkstyle.xml`
- `config/pmd/ruleset.xml`
- `docs/architecture/ARCHITECTURE_GUARDRAILS.md`
