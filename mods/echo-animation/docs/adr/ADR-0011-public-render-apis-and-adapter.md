# ADR-0011: Public Render APIs And Adapter

## Decision

Echo NPC owns a public client render extension event and Echo Animation owns a
public client face render service. NPC-to-Animation rendering is implemented by
`echo-npc-animation-adapter`.

## Consequences

- Echo NPC exposes `NpcClientRenderRegistrationEvent` and `NpcHeadRenderContext`.
- Echo Animation exposes `AnimationClientServiceBridge` and
  `AnimationFaceRenderContext`.
- Adapter depends on public Maven artifacts only and does not bundle API copies.
