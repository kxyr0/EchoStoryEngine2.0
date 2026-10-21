# 0002 Identity And Definition Model

## Status

Accepted for N1.

## Context

NPC needs stable logical identity, while Minecraft entity UUID is platform-owned
and can change.

## Decision

Use separate IDs:

- `NpcDefinitionId` for authored resource definitions;
- `ActorId` for runtime logical actors;
- entity UUID only as internal platform handle.

`NpcDefinition` is immutable and fingerprinted from semantic fields.

## Consequences

- Duplicate spawn and conflict cases are explicit.
- Read models are portable.
- N2 can add recovery without changing public `ActorId`.
