# Presentation mapping

When Echo NPC returns a usable actor:

- `NpcActorView.displayName()` becomes `ResolvedSpeakerPresentation.displayName()`;
- `NpcActorView.skinId()` becomes `SpeakerVisualRef.skinHead(skinId)`;
- absent skin keeps `request.authoredVisual()`.

N5 form-aware mapping keeps the same `ActorId` but may replace the visual from
the active form snapshot:

- `NpcService.getForm(actorId).portraitResourceId()` is the preferred dialogue
  portrait when present;
- `NpcFormSnapshot.faceProfileId()` is the facial profile selector;
- `NpcActorView.activeFormId()` is diagnostic context, not speaker identity.

This is required because Echo Dialogue replaces the full speaker visual when a
provider returns `RESOLVED`. Returning empty visual for a no-skin NPC would erase
the authored portrait.

`SpeakerPresentationRequest.kind()` can be `LINE` or `CHOICE`; the same mapping
is used for both.
