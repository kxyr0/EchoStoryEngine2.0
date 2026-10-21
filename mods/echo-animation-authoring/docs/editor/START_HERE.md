# START_HERE

Use this mod only in a development Minecraft client.

Expected workflow:

1. Configure the content-pack workspace with JVM property `echoAnimationAuthoring.workspace`.
2. Run `/echo_animation_authoring workspace` to confirm the path.
3. Run `/echo_animation_authoring editor` in the client.
4. Open a FaceRig with `/echo_animation_authoring face open jdh:john_father`.
5. Open a legacy input with `/echo_animation_authoring migrate <legacyPath>` when migration is needed.
6. Check `/echo_animation_authoring diagnostics`.
7. Open a FaceRig from `data/jdh/echo_animation/face_rigs/`.
8. Edit bones, pivots, layers, poses and animation clips.
9. Save `.face-pose.es` or `.face-animation.es`.
10. Export neutral skin to `assets/jdh/textures/echo_npc/skins/`.

When workspace is not configured, writes are blocked.
