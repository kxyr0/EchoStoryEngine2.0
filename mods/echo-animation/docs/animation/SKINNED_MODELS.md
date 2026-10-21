# Smooth-skinned models

`SKINNED_MESH` is an optional render path next to the unchanged `SEGMENTED_HUMANOID` path. A rig named
`sample:people/father` resolves its mesh from
`assets/sample/echo_animation/models/people/father.skin.json`. The existing animation clips still provide local
translation, XYZ rotation and scale; only the way geometry consumes the final bone transforms changes.

## Format

Version 2 stores one skeleton and one or more triangulated meshes. Matrices are column-major. Positions, bind
translations and animation translations are Minecraft pixels (16 units per block). A vertex contains `position`,
`normal`, `uv`, `boneIndices` and `boneWeights`. The loader ignores zero weights, retains the four strongest positive
weights, normalizes them and attaches an unweighted vertex to the single root bone with a warning.

Each bone contains a lower `snake_case` `name`, numeric `parent`, local bind matrix and inverse global bind matrix.
For every frame the client evaluates:

```text
globalPose[bone] = globalPose[parent] * localAnimatedTransform
skinMatrix[bone] = globalPose[bone] * inverseBind[bone]
```

Omitting `formatVersion` selects legacy v1. A v1 mesh has a numeric `bone`; every vertex is adapted to that bone with
weights `[1, 0, 0, 0]`. Existing built-in segmented humanoids do not use this file reader and remain visually
unchanged.

## Blender export

Use ordinary deform bones, an Armature modifier and vertex groups whose names exactly match lower `snake_case` bone
names. Control/IK bones with **Deform** disabled are not exported. Run:

```text
blender --background character.blend --python tools/blender/echo_skinned_export.py -- father.skin.json Body
```

The exporter converts Blender axes to Minecraft axes, applies mesh/armature object transforms once, exports split
loop normals and UVs, triangulates through Blender's loop triangles, computes inverse bind matrices and reports
unknown groups, missing weights and truncation above four influences. It rejects multiple armatures, multiple deform
roots, invalid bind matrices and non-uniform object scale.

For a soft elbow or knee, use roughly 3–5 cross-sectional edge-loop rows around the joint and paint a progression
such as `1/0 → .75/.25 → .5/.5 → .25/.75 → 0/1`. `Ctrl+J` only combines objects; it does not weld two disconnected
cubes. Keep Flat Shading and a low segment count—Subdivision Surface is not required.

## Limits and diagnostics

The OpenGL 3.2 shader uses an interleaved VBO and a uniform palette. It reserves 48 matrices and lowers the accepted
bone count when `GL_MAX_VERTEX_UNIFORM_COMPONENTS` requires it. Loading logs format version, bones and vertices;
validation errors identify the model, mesh and vertex.

Debug JVM properties:

- `-Decho.animation.cpuSkinning=true` forces the reference CPU backend.
- `-Decho.animation.freezeBindPose=true` freezes bind pose.
- `-Decho.animation.skinDebug=dominant` displays the dominant bone as color.

The test model at `animation-forge-1.20.1/src/test/resources/skinning/elbow.skin.json` contains upper-arm/forearm
bones, five weighted rows and a 0–120 degree elbow fixture.
