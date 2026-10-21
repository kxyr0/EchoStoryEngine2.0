# 0007 Save Before Materialization

## Status

Accepted for N2.

## Context

Crash-safe spawn, teleport and despawn require durable intent to be written
before Minecraft world side effects.

## Decision

All mutating operations save desired state first. Forge applies repository
results on the server thread and only then creates, moves or removes entities.

## Consequences

- Mutating public API returns `QUEUED` operation submission results.
- Operation terminal status is queryable separately.
- Failed repository writes do not create or move entities.
