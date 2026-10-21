# ADR 0005: authored fallback preserved

## Status

Accepted for N4.

## Context

Echo Dialogue replaces the full speaker presentation when a provider returns
`RESOLVED`. If N4 returned a text-only presentation for no-skin actors, authored
portraits would disappear.

## Decision

When an actor exists and has no `NpcSkinId`, N4 returns the dynamic NPC display
name together with `request.authoredVisual()`.

## Consequences

- Dynamic names work for no-skin actors.
- Existing authored portrait fallback remains visible.
- Missing actors still return `NOT_HANDLED`; this ADR applies only to found
  actors.
