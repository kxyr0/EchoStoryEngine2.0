# ADR 0003: actor-existence claim policy

## Status

Accepted for N4.

## Context

D6 lets multiple providers answer a speaker request. A provider should not claim
ordinary authored speakers unless it has authority for them.

## Decision

N4 claims only when Echo NPC public `NpcService.query(ActorId)` proves that the
durable actor exists, or when that known actor exists but currently cannot
produce a safe presentation.

When `NpcService` is unavailable or actor query returns `NOT_FOUND`, N4 returns
`NOT_HANDLED`.

## Consequences

- Missing actors and service outage preserve authored fallback.
- Blocked/corrupt known actors produce a bounded provider diagnostic and still
  fall back through Echo Dialogue.
- Provider ambiguity remains owned by Echo Dialogue.
