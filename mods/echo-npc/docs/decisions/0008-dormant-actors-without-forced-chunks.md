# 0008 Dormant Actors Without Forced Chunks

## Status

Accepted for N2.

## Context

Forcing chunks for every NPC would introduce server performance and ownership
policy questions beyond N2.

## Decision

N2 never creates chunk tickets. ACTIVE actors whose target chunk is unloaded are
kept durable and marked `DORMANT_CHUNK_UNLOADED`.

## Consequences

- Startup recovery is bounded and does not load arbitrary terrain.
- Actor records survive chunk unload.
- Entity reconstruction happens when normal server activity loads the target
  chunk.
