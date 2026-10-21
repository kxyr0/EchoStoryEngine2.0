# Client Render Extensions

Echo NPC owns the NPC entity and renderer. External mods may extend head and
generic body-anchor rendering only through the public client API in
`npc-forge-api-1.20.1`.

## Public API

- `NpcClientRenderExtensionId`
- `NpcClientRenderExtensionDescriptor`
- `NpcClientRenderExtensionFactory`
- `NpcClientRenderExtension`
- `NpcHeadRenderContext`
- `NpcClientRenderRegistrationEvent`
- `NpcBodyAttachmentRenderContext`
- `NpcBodyAttachmentRenderExtension`
- `NpcBodyAttachmentRenderRegistrationEvent`
- `NpcVisualAttachmentAnchor`

`NpcClientRenderResult.CANCELLED` is an exclusive terminal result. It stops
later extensions and suppresses the complete NPC render: default model, name,
shadow and layers. It is intended for explicit client-side concealment leases;
ordinary cosmetic extensions should use `RENDERED_OVERLAY` or
`REPLACED_DEFAULT_MODEL`.

The context exposes only:

- public `ActorId`;
- `PoseStack`;
- `MultiBufferSource`;
- packed light;
- overlay;
- partial tick;
- safe `ModelPart head`;
- visibility flag;
- bounded `definitionId` and `skinId` metadata.

It does not expose `EchoNpcEntity`, `EchoNpcEntityRenderer`, persistence,
entity binding internals or mutable renderer collections.

## Lifecycle

Echo NPC posts `NpcClientRenderRegistrationEvent` on the client Forge bus while
building render layers. Registrations are explicit, sorted by extension ID and
closed into an immutable renderer-local catalog.

Duplicate IDs are rejected. Extension failures are caught at the boundary; Echo
NPC continues rendering the NPC and reports a bounded warning.

Dedicated server lifecycle does not register or call client render extensions.

## Body attachments

The body attachment context exposes stable `TORSO_BACK`, `LEFT_HAND` and
`RIGHT_HAND` model anchors plus public actor/form metadata. Extensions own their
local transforms and rendered item; they do not receive the NPC entity or
renderer internals. Registration is explicit, deterministic and failure-isolated
with the same lifecycle as head extensions.

## Animation Adapter

Echo Animation integration is implemented by separate
`echo-npc-animation-adapter`. Echo NPC does not depend on Echo Animation.
