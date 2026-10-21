# Skin Heads D5

`SkinHead("sample:father")` resolves client-side as a modern Minecraft `64x64` skin.

For `SKIN_HEAD`, the client tries paths in this order:

```text
assets/sample/textures/echo_dialogue/skins/father.png
assets/sample/textures/echo_npc/skins/father.png
assets/sample/textures/echo_player/skins/father.png
assets/sample/textures/father.png
```

The extra roots let authored Dialogue skins, Echo NPC skins, Echo Player skins, and direct wardrobe-style
texture IDs work without duplicating PNGs.

The renderer draws a flat head:

- base face UV: `x=8, y=8, w=8, h=8`;
- outer head/hat UV: `x=40, y=8, w=8, h=8`;
- outer layer is drawn over the base face with the same bounds, preserving transparent pixels.

Legacy `64x32` skins are not supported; they fall back to text-only. Body, arms, legs, cape, armor,
3D model, and live entity lookup are not rendered by Echo Dialogue.
