# ADR 0002: canonical SpeakerId-to-ActorId mapping

## Status

Accepted for N4.

## Context

The content pack already uses stable canonical IDs for Dialogue speakers and NPC
actors. Adding a second manifest would create duplicated authoring state.

## Decision

N4 maps only:

```text
SpeakerId canonical string == ActorId canonical string
```

No alias, display name, `NpcDefinitionId`, file path or entity UUID can become an
actor identity.

## Consequences

- Authoring is simple and deterministic.
- Ordinary non-NPC speakers return `NOT_HANDLED`.
- A future explicit mapping system must be a separate feature, not hidden N4
  behavior.
