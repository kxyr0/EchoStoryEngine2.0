# Echo Animator: Epic Fight Adapter

Client-side Forge adapter for Minecraft 1.20.1 that renders Echo Animation player faces on Epic Fight's final `Head` joint pose.

## Problem

Echo Animation renders player facial elements through `io.github.kxyr0.echoanimation.forge.client.EchoContentPlayerFaceLayer`. Epic Fight replaces the vanilla player model pose with its own armature, so the vanilla head transform no longer contains the final combat animation head pose. Without an adapter, eyes, eyelids, brows, and expressions can lag behind or separate from the animated head.

## Supported Versions

- Minecraft: `1.20.1`
- Forge: `47.4.10`
- Java: `17`
- Epic Fight: mod metadata `20.14.17`, Modrinth Maven coordinate `20.14.17-mc1.20.1-forge`
- Echo Animation: installed target mod id is `echo_animation`, version `0.2.1-SNAPSHOT`

The task text names the dependency as `echo-animator`; the installed Forge mod metadata exposes it as `echo_animation`, so `mods.toml` uses the real mod id.

## Dependencies

Epic Fight is resolved through Modrinth Maven:

```gradle
implementation fg.deobf("maven.modrinth:epic-fight:${epicfight_version}")
```

Echo Animation is resolved from the target Prism instance `mods` directory through a `flatDir` repository as a deobfuscated external compile/runtime dependency. This project does not shade, jarJar, shadow, or copy Echo Animation or Epic Fight classes into the adapter jar.

## Architecture

- Registers on the client Forge mod bus for `PatchedRenderersEvent.Modify`.
- Finds the vanilla Echo face layer class by name: `EchoContentPlayerFaceLayer`.
- Replaces Epic Fight's default patched mapping for that vanilla layer with `EchoEpicFightFaceLayer`.
- Uses the `poseMatrices` passed to `PatchedLayer` after Epic Fight has called `setArmaturePose`.
- Resolves and caches the Epic Fight joint id for `Head` per `Armature` class.
- Calls Echo Animation's public `AnimationClientServiceBridge.lookup()` and `AnimationClientService.renderFace(...)`.
- Passes a neutral `ModelPart` head so Echo does not re-apply the vanilla `PlayerModel.head` transform.

## Coordinate Transform

The adapter applies:

1. `MathUtils.mulStack(poses, headMatrix)` with the final Epic Fight `Head` matrix.
2. `poses.scale(-1.0F, -1.0F, 1.0F)`.
3. Echo face rendering in local head coordinates.

The scale is the same Minecraft-model-space conversion used by Epic Fight's `RenderOriginalModelLayer` before it invokes vanilla `RenderLayer` instances. There are no idle-specific offsets or Euler-angle reconstruction.

## Double Render Protection

Epic Fight normally maps unknown vanilla layers to `RenderOriginalModelLayer` on `Root`. The adapter uses `addPatchedLayerAlways(...)` for the exact Echo face layer class, so when the Epic Fight renderer is active that vanilla Echo layer is handled by the adapter once.

If the adapter cannot resolve `Head`, cannot get Echo's render service, has no active face rig, or Echo returns anything other than `RENDERED`, it delegates to the same Epic Fight default fallback path instead of disabling the original layer early.

## Build

```text
gradlew build
```

The build includes checks for:

- no wildcard imports;
- Java file size limits;
- no embedded `yesman.epicfight`, `io.github.kxyr0.echoanimation`, Minecraft, or Forge classes in the adapter jar.

## Manual Test Checklist

Test in third person with Echo player skin/face data and Epic Fight installed:

- idle, walk, run, sneak, jump/fall;
- head look up, down, and sideways;
- Epic Fight battle mode on/off;
- normal and heavy/special attacks;
- dodge/roll, block, damage/stun, death;
- sleep, riding, world rejoin;
- blink and expression animation during combat motion;
- another player or supported remote player skin;
- invisibility.

Known limitation: the adapter currently targets the player renderer because the discovered Echo face layer is player-specific. NPC support can be added if Echo Animation exposes an NPC face layer with the same public render service contract.
