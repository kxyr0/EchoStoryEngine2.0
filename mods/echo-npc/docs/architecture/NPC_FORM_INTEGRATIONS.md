# NPC Form Integrations

Интеграции должны читать форму через public API:

- `NpcActorView.activeFormId`;
- `NpcActorView.activeFormRevision`;
- `NpcService.getForm(ActorId)`;
- `NpcService.isForm(ActorId, NpcFormId)`;
- `NpcService.formCatalog(limit)`.

Animation adapter использует `animationGraphId` и semantic part ids. Dialogue
adapter использует `portraitResourceId` и `faceProfileId`. Cutscene/camera
системы используют anchors (`eyes`, `head`, `chest`, `feet`).

Интеграция не должна:

- создавать отдельный child/adult ActorId;
- читать Forge entity напрямую, если хватает public snapshot;
- менять scale или body geometry вне `Npc.SetForm`;
- менять texture напрямую на Forge entity вместо `Npc.ChangeSkin`;
- считать `NpcFormId` replacement identity.
