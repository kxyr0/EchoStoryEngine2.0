# ADR 0004: NPC skin resource reused for Dialogue head

## Status

Accepted for N4.

## Context

Echo NPC owns full Minecraft skins under:

```text
assets/<namespace>/textures/echo_npc/skins/<path>.png
```

Echo Dialogue D5 originally resolved authored skin heads under
`echo_dialogue/skins`.

## Decision

N4 returns neutral `SpeakerVisualRef.skinHead(npcSkinId)`. Echo Dialogue client
keeps the authored Dialogue skin path and then tries the NPC skin path for
`SKIN_HEAD`.

## Consequences

- No duplicate Dialogue skin PNG is required.
- The public visual descriptor remains generic and does not expose `ActorId`,
  raw paths, `ResourceLocation`, PNG bytes or NPC implementation types.
- The server does not decode skin PNG assets.
