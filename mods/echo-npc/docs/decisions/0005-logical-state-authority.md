# 0005 Logical State Authority

## Status

Accepted for N2.

## Context

Minecraft entity NBT can be stale, duplicated, missing or loaded out of order.

## Decision

Durable actor record is the authority. Entity NBT contains only technical
linkage needed to validate and adopt or reject a physical entity.

## Consequences

- Stale entity NBT cannot recreate actors by itself.
- Recovery can remove orphan or malformed entities deterministically.
- Display name and skin are resolved from the pinned definition, not from entity
  storage.
