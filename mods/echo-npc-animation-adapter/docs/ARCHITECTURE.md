# Adapter Architecture

`echo-npc-animation-adapter` is an independent Forge 1.20.1 repository.

## Dependencies

Production compile dependencies are public Maven artifacts only:

- `io.github.kxyr0.echoanimation:animation-contracts`;
- `io.github.kxyr0.echoanimation:animation-forge-api-1.20.1`;
- `io.github.kxyr0.echonpc:npc-contracts`;
- `io.github.kxyr0.echonpc:npc-forge-api-1.20.1`.

There are no Gradle project dependencies, no `libs` API JARs and no bundled
copies of Echo NPC or Echo Animation APIs.

## Render Bridge

1. Echo NPC posts `NpcClientRenderRegistrationEvent`.
2. Adapter registers `echo_npc_animation_adapter:face_layers`.
3. Echo NPC calls the extension with `NpcHeadRenderContext`.
4. Adapter maps `ActorId.value()` to `AnimationSubjectId`.
5. Adapter resolves `AnimationClientServiceBridge`.
6. Adapter sends `AnimationFaceRenderContext` to Echo Animation.

## N5 Form Contract

Echo NPC N5 keeps `ActorId` stable while `NpcFormId` changes body and
appearance. Animation integration must treat `ActorId` as the animation subject
identity and use active form snapshots only for graph/profile selection.

Form-aware playback should read `NpcService.getForm(ActorId)` when the N5
contract artifact is present, then use `NpcFormSnapshot.animationGraphId()`.
If no active form snapshot exists, the adapter keeps the existing
definition-based fallback.

## Diagnostics

Invalid `definitionId` or `skinId` values are ignored at the adapter boundary.
Missing Animation client service is a no-op so Echo NPC fallback rendering
continues.

## Packaging

`verifyAdapterJarContents` rejects bundled public API classes, NPC
implementation classes, Animation implementation classes and source files.
