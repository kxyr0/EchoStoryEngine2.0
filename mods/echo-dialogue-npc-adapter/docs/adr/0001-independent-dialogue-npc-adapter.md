# ADR 0001: independent Dialogue-NPC adapter

## Status

Accepted for N4.

## Context

Echo Dialogue and Echo NPC must remain independent mods. Dialogue must not import
NPC implementation packages, and NPC must not import Dialogue runtime packages.

## Decision

Create `echo-dialogue-npc-adapter` as a separate Forge mod and Gradle repository.
It consumes only public Dialogue and NPC artifacts from sibling generated Maven
repositories.

## Consequences

- The adapter JAR can be removed without corrupting Dialogue or NPC state.
- Story integration remains outside N4.
- Runtime dependencies are explicit in `mods.toml`: `echo_dialogue`, `echo_npc`,
  `forge`, `minecraft`.
