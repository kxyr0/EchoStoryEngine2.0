# Echo Animation

Echo Animation is the independent production runtime `echo_animation` for Echo
animation resources on Forge 1.20.1.

## Resources

Production definitions load from:

- `data/<namespace>/echo_animation/face_rigs/**/*.face-rig.es`;
- `data/<namespace>/echo_animation/face_poses/**/*.face-pose.es`;
- `data/<namespace>/echo_animation/face_animations/**/*.face-animation.es`.

Code-oriented dialogue and portrait animation notes live in
`docs/animation/CODE_AUTHORING_AND_DIALOGUE.md`.

`.animation-graph.es` is the A1 graph-resource suffix documented for the
state model. Full graph EchoScript binding remains limited in A1.

## Modules

- `animation-contracts` - public IDs, graph contracts, subject IDs, vectors,
  transforms and diagnostics.
- `animation-core` - hierarchy/transform/clip evaluation and per-subject
  deterministic state runtime.
- `animation-definition` - immutable FaceRig, FacePose and FacialAnimation
  definitions.
- `animation-echoscript-adapter` - EchoScript semantic binder for A0 resources.
- `animation-forge-api-1.20.1` - public Forge service and client-render APIs.
- `animation-client-rendering` - draw command planning.
- `animation-forge-1.20.1` - Forge entrypoint, reload listener, commands and
  final JAR.

## A1 dependency rule

Echo Animation does not depend on Echo NPC. A1 removed the file-based
`compileOnly` dependency on the Echo NPC runtime JAR and removed the
NPC-specific `EchoNpcFaceLayer*` integration.

NPC face rendering now lives in the separate `echo-npc-animation-adapter`
repository. The adapter depends only on public Echo Animation artifacts and
public Echo NPC artifacts from `build/integration-maven`.

## Public client rendering

Echo Animation exposes public client rendering through:

- `AnimationClientServiceBridge`;
- `AnimationClientService`;
- `AnimationFaceRenderContext`;
- `AnimationFaceRenderResult`.

The service renders authored face layers into a supplied head render context.
It does not know about NPC entities or NPC renderers.

## Direct MAIN animations (0.6.2)

High-level animations no longer require synthetic Enter/Exit clips when the
author only needs the main motion. A resource may keep its allowed source state
while using `entries: []` and `exits: []`. Playback starts directly in MAIN.

Supported `MainMode` values are:

- `ONCE` — play Main once and finish;
- `LOOP_UNTIL_STOP` — repeat Main until Stop, used for idle/walk/run;
- `HOLD_UNTIL_STOP` — play Main once, clamp to its final frame and hold that pose until Stop.

`HOLD_UNTIL_STOP` is the semantic replacement for fake durations such as
100000 hold ticks. Full Enter -> Main -> Exit lifecycles remain supported when
entry/exit clips are actually authored.

## Per-subject state

A1 adds neutral `AnimationSubjectId` and plain-Java per-subject state runtime:

- shared immutable graph definitions;
- independent channel slots;
- explicit ownership leases;
- explicit transitions;
- logical ticks;
- `HOLD_LAST_FRAME`, `LOOP` and `STOP_AT_END` playback modes;
- `WAIT_FOR_EXIT` interruption policy.

Persistence, Story/Dialogue adapters, complete NPC synchronization, navigation
and locomotion are not implemented in A1.

## Commands

- `/echo_animation health`
- `/echo_animation face_rigs`
- `/echo_animation face_rig <id>`
- `/echo_animation face_poses`
- `/echo_animation face_animations`
- `/echo_animation diagnostics [limit]`

`/echo_animation catalog` remains a compatibility alias for the health summary.

## Verification

```powershell
.\gradlew.bat projects
.\gradlew.bat dependencies
.\gradlew.bat test
.\gradlew.bat architectureTest
.\gradlew.bat sourceShapeCheck
.\gradlew.bat checkWhitespace
.\gradlew.bat check
.\gradlew.bat build
```

Final JAR:
`animation-forge-1.20.1/build/libs/echo-animation-0.1.0-SNAPSHOT-all.jar`.

[История изменений](docs/changelogs/README.md)
