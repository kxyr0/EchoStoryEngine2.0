# ADR 0010: Speaker Visuals Are Neutral IDs Resolved On The Client

Status: accepted for D5.

Echo Dialogue supports two authored speaker visual modes:

- `Portrait("namespace:path")`;
- `SkinHead("namespace:path")`.

`dialogue-core` stores only neutral immutable `SpeakerVisualRef` values: kind and bounded namespaced ID. Forge client code maps those IDs to Minecraft resources:

- portraits: `assets/<namespace>/textures/echo_dialogue/portraits/<path>.png`;
- skins: `assets/<namespace>/textures/echo_dialogue/skins/<path>.png`.

Reason:

- visual identity is authored dialogue meaning and must participate in compiled fingerprint/recovery compatibility;
- PNG bytes, resource availability and dimensions are client presentation state and must not be persisted;
- dedicated servers must compile visual IDs without loading client renderer/image classes;
- Story Engine and Story-Dialogue Adapter must not gain portrait/skin concepts.

Consequences:

- network protocol `3` sends only optional visual kind/id in `DialogueOpenMessage`;
- client-to-server packets remain unchanged;
- Save Schema V2 remains unchanged;
- replacing PNG bytes under the same ID does not block recovery;
- changing authored visual kind or ID changes fingerprint and blocks incompatible ACTIVE recovery;
- missing/corrupt client assets fall back to text-only layout;
- live NPC/entity/player skin lookup, Mojang API, remote URLs and generic provider plugins are deferred.
