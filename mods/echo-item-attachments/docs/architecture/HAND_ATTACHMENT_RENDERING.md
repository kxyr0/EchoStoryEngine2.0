# Hand attachment rendering

The server synchronizes full-stack `AttachmentSnapshotPayload` values with a monotonically
increasing subject revision. `ClientAttachmentStore` ignores stale and duplicate revisions;
login receives a full resync. This client copy is only a synchronized representation of the
server-owned payload.

NPC rendering registers `NpcHandItemRenderer` through
`NpcBodyAttachmentRenderRegistrationEvent`. `context.applyAnchor` therefore follows either
the current vanilla arm or Echo Animation's replacement rig hand socket. Player third person
uses the current `PlayerModel` left/right arm transforms. First person intercepts only the
owned physical hand with `RenderHandEvent`, renders once, and cancels its vanilla pass.

All paths call Minecraft `ItemRenderer`; no fake visual-only stack exists. `ItemGripCatalog`
loads all `*/grips/*.json` resources on reload. Profiles provide left/right translation,
rotation, and scale plus an optional exact item matcher; `default.json` is the fallback.
