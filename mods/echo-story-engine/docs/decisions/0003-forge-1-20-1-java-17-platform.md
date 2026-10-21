# 0003 Forge 1.20.1 And Java 17 As First Platform

## Status

Accepted.

## Context

The first implementation target is Minecraft Java Edition 1.20.1 with Forge 1.20.1 and Java 17.

## Decision

Use the existing Forge MDK wrapper and ForgeGradle only in `story-forge-1.20.1`. Keep Minecraft and Forge versions
centralized in `gradle.properties`.

## Alternatives

- Upgrade Minecraft, Forge, or Gradle now. Rejected because the task is an initial scaffold, not dependency migration.
- Support multiple loaders immediately. Rejected because there is no second adapter yet.

## Consequences

Forge setup cost is isolated to the platform module. Pure modules compile with the normal Java plugin and Java 17.

## Enforcement

Gradle module configuration and architecture tests that forbid platform imports in contracts/core.

## Links

- `gradle.properties`
- `story-forge-1.20.1/build.gradle`
- `docs/architecture/PORTABILITY.md`
