# Face Layers

`FaceLayerDefinition` binds:

- `FaceLayerId`;
- parent `BoneId`;
- `FaceTextureId`;
- semantic `LayerRole`;
- draw order;
- local placement transform;
- atlas rectangle;
- optional clip rectangle;
- default visibility.

`x`, `y`, `w` and `h` select pixels inside the PNG atlas. Placement fields move
the visible plane on the face:

```text
Layer("brow_left",
    bone: "brows_left",
    texture: "jdh:echo_animation/faces/john_father/brow_left",
    role: "BROW",
    order: 30,
    x: 0, y: 0, w: 8, h: 2,
    placeX: "-0.07",
    placeY: "0.30",
    placeZ: "0.63")
```

`LayerDrawPlanner` sorts by `order`, then by `role`, then by layer ID. `placeZ`
controls head-front depth placement but does not replace deterministic
`order`.

The focused A5 proof for pupil/sclera clipping is in
[`FACE_LAYER_AUDIT.md`](FACE_LAYER_AUDIT.md).

## Pupil/Sclera Policy

New canonical face rigs must author `left_pupil` as a child of `left_sclera`
and `right_pupil` as a child of `right_sclera`. The validator rejects a
canonical pupil bone when its matching sclera bone is missing or is not the
direct parent.

At render projection time, canonical pupils are clamped against the non-pupil
layer whose id matches their parent sclera bone before any fallback surface is
considered. Extra decoration layers on the same parent bone do not become the
pupil clip surface by accident.

Existing authored rigs that use layer IDs such as `pupil_left` and
`sclera_left` on a shared `face_root` are not migrated by the engine. The
renderer resolves those authored pairs at projection time and clips the pupil
draw command to the paired sclera surface, or clamps the pupil center there
when the pupil already fits, without changing `.face-rig.es`
coordinates or parent origins.

The validator also rejects known authored PUPIL layers (`pupil_left`,
`pupil_right`, `left_pupil`, `right_pupil`) when the matching sclera layer is
missing or is itself marked as `PUPIL`.

## A1 Rendering Boundary

Echo Animation no longer installs an NPC render layer. The production renderer
is exposed as public `AnimationClientService` in `animation-forge-api-1.20.1`.

NPC rendering is bridged by `echo-npc-animation-adapter`:

1. Echo NPC posts `NpcClientRenderRegistrationEvent`.
2. Adapter registers extension ID `echo_npc_animation_adapter:face_layers`.
3. Echo NPC calls the extension with `NpcHeadRenderContext`.
4. Adapter maps public `ActorId` to `AnimationSubjectId`.
5. Adapter calls `AnimationClientServiceBridge.lookup().renderFace(...)`.

The adapter uses `definitionId` first and `skinId` second as `FaceRigId`
candidates. That fallback behavior is outside Echo Animation runtime.
