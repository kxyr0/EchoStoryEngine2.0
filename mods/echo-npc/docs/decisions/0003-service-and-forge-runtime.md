# 0003 Service And Forge Runtime

## Status

Accepted for N1.

## Context

Other Forge mods need access to NPC operations, but N1 must avoid global mutable
service patterns and reflection.

## Decision

Expose `NpcService` through `npc-contracts`. Forge access uses
`EchoNpcServiceBridge` and `EchoNpcServiceLookupEvent`. Runtime host is scoped to
the active `MinecraftServer`.

Forge representation uses one custom stationary humanoid entity with synced
presentation data and client-only renderer registration.

## Consequences

- Dedicated server does not load renderer classes.
- API consumers do not depend on full runtime module.
- N1 remains runtime-only; persistent actor reconstruction moves to N2.
