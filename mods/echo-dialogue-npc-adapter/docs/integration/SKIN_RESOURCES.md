# Skin resources

N4 returns:

```java
SpeakerVisualRef.skinHead(npcSkinId.toString())
```

For content pack `jdh:john_father`, the actual PNG stays in Echo NPC resource
ownership:

```text
assets/jdh/textures/echo_npc/skins/john_father.png
```

Echo Dialogue client resolves `SKIN_HEAD` by trying:

1. `assets/<namespace>/textures/echo_dialogue/skins/<path>.png`;
2. `assets/<namespace>/textures/echo_npc/skins/<path>.png`.

The second path is what lets N4 use the existing NPC skin without duplicating it
under `echo_dialogue/skins`.
