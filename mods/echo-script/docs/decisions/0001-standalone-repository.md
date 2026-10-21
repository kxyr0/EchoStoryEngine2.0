# 0001 EchoScript as a Standalone Repository

## Status

Accepted.

## Context

EchoScript must be reusable by multiple consumers without inheriting a game, Story, or Dialogue runtime.

## Decision

EchoScript is a standalone repository and build.

## Alternatives

- Implement inside Echo Story Engine.
- Implement inside Echo Dialogue.

## Consequences

Consumers must integrate through adapters. The language frontend remains independently testable.

## Enforcement

Architecture tests and dependency scans reject Story, Dialogue, Minecraft, Forge, and Fabric dependencies.

## Related Documents

- `docs/architecture/PROJECT_MAP.md`
- `docs/architecture/DEPENDENCY_RULES.md`
