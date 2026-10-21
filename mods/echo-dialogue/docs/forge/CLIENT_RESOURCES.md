# Client Resources D5

Speaker visuals resolve only on the physical client.

## Paths

```text
Portrait("sample:merchant")
-> assets/sample/textures/echo_dialogue/portraits/merchant.png

SkinHead("sample:father")
-> assets/sample/textures/echo_dialogue/skins/father.png

D6 provider SkinHead fallback path
-> assets/sample/textures/echo_npc/skins/father.png
```

Server reload compiles `.dialogue.es` and validates only neutral visual IDs. It does not decode PNG files and does not require client assets.

Client resource reload clears the bounded visual resolver cache. The active dialogue session, `DialogueSessionId`, revision, current node and choice state remain unchanged. On the next render frame the active visual is re-resolved:

- added resource starts rendering;
- removed/corrupt resource falls back to text-only;
- replaced PNG bytes update visually without changing compiled fingerprint.
- D6 dynamic provider may choose a different `SpeakerVisualRef`; `SKIN_HEAD`
  first resolves through the authored D5 path and then through the NPC skin path.

The resolver cache is client-owned, bounded to 64 entries and also cleared on logout/disconnect.
