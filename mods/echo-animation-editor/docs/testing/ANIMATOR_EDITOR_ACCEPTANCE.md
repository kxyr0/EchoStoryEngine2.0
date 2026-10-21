# Animator Editor Acceptance

Automated in 0.1.0:

- layout matrix has no overlaps, overflow or negative sizes;
- GUI scale 1-4 keeps identical physical panel rectangles;
- timeline frame/pixel mapping clamps to clip boundaries;
- JAR verification rejects bundled Echo Animation, Echo Animation Authoring, Echo NPC, Minecraft and Forge classes.

Automated in 0.2.0:

- authoring adapter wraps open, keyframe mutation, save and hot reload outcomes without a compile-time dependency;
- GUI input routes Ctrl+S and viewport LMB drag into the active editor session.

Automated in 0.3.0:

- gizmo picker resolves axes, planes, rotate rings and scale handles;
- delta mapper keeps translate/rotate/scale components separate;
- authoring tests cover undo and autosave/recovery sidecar state.

Manual Minecraft acceptance should verify drag handles, selected bone/face commands, Ctrl+Z/Ctrl+Y, autosave recovery,
preview clear/apply and hot reload feedback in an interactive client.
