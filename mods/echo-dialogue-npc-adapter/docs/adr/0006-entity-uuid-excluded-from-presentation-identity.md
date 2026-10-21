# ADR 0006: Entity UUID excluded from presentation identity

## Status

Accepted for N4.

## Context

Echo NPC `ActorId` is durable logical identity. Minecraft entity UUID belongs to
materialized runtime entities and can change across recovery or reconstruction.

## Decision

N4 never reads entity UUID, entity id or entity implementation state. It uses only
public `NpcActorView` returned by `NpcService.query(ActorId)`.

## Consequences

- Dormant actors can still resolve presentation.
- Restart does not require the same entity UUID.
- Presentation identity remains independent from materialization.
