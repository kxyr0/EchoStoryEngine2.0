# 0002 Plain Java 17 Library

## Status

Accepted.

## Context

The first milestone needs a portable language frontend, not a mod loader artifact.

## Decision

EchoScript is a plain Java 17 Gradle library.

## Alternatives

- Forge mod.
- Fabric mod.
- Executable application.

## Consequences

There are no mod descriptors, lifecycle hooks, networking hooks, or Minecraft dependencies.

## Enforcement

Architecture tests scan for loader and Minecraft packages.

## Related Documents

- `docs/architecture/PORTABILITY.md`
- `docs/development/TESTING.md`
