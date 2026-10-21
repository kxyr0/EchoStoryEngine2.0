# 0006 Entity Binding Id

## Status

Accepted for N2.

## Context

`ActorId` is logical identity and Minecraft UUID is platform identity. Recovery
needs a stable way to recognize the intended physical incarnation.

## Decision

Persist internal `EntityBindingId` and copy it to `EchoNpcEntity` NBT/synced
data. Recovery accepts only exact `ActorId + EntityBindingId`.

## Consequences

- Entity UUID may change across reconstruction.
- Duplicate or stale physical entities can be rejected without changing
  `ActorId`.
- `EntityBindingId` stays internal and is not public actor identity.
