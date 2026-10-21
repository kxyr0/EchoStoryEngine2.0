# 0004 World Scoped Actor Persistence

## Status

Accepted for N2.

## Context

N1 actors lived only in memory. N2 requires restart recovery without coupling
`npc-core` to Forge or filesystem classes.

## Decision

Store actor desired state per world under `data/echo_npc/actors`. `npc-core`
defines a repository port; `npc-persistence` owns the file implementation.

## Consequences

- Different Minecraft worlds do not share actor records.
- Repository implementation remains replaceable without changing public API.
- Manual deletion tools are not exposed in N2 commands.
