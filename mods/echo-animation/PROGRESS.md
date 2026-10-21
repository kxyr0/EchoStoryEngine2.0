# Progress

## A6 unified animation timelines

2026-08-05: body clips and high-level animations gained procedural EchoScript builders, typed symbols, named
segments and pose points. Runtime playback samples native segment views, resolves durations from clip metadata,
supports optional lifecycle phases and fallback crossfades, and persists/synchronizes segment references. The
Blender exporter now emits one clip per Action. `humanoid_run` migrated from three clips to one 17-tick timeline.
Version advanced to `0.5.0-SNAPSHOT`; network protocol advanced to `2`.

## A5 generic server markers

2026-08-04: high-level animations gained neutral `AnimationMarkerId` and
`AnimationMarkerDefinition` contracts, EchoScript `Marker(...)` authoring,
archived-definition and playback-receipt persistence, and the public
server-authoritative `AnimationMarkerEvent`. No backpack-specific type or
dependency was added. Version advanced to `0.3.0-SNAPSHOT`.

## A0

A0 is complete:

- independent Gradle repository `echo-animation`;
- seven production modules;
- immutable public IDs and neutral transform contracts;
- FaceRig, FacePose and FacialAnimation definitions;
- EchoScript binder for `FaceRigResource`, `FacePoseResource` and
  `FacialAnimationResource`;
- Forge resource discovery, reload listener, commands and catalog diagnostics;
- public Forge service lookup API;
- production JAR verification;
- A0 face layer placement and draw planning.

## A1

A1 removes the hard Echo NPC dependency and adds the per-subject deterministic
state core.

Completed:

- removed Echo NPC runtime JAR `compileOnly` dependency from Echo Animation;
- removed Echo NPC imports from Echo Animation production sources;
- removed `EchoNpcFaceLayer` and `EchoNpcFaceLayerInstaller` from Echo Animation;
- removed `echo_npc` optional metadata dependency from Echo Animation
  `mods.toml`;
- added `AnimationSubjectId`;
- added public Animation client render API;
- added public Echo NPC client render extension API;
- created independent `echo-npc-animation-adapter`;
- adapter maps public `ActorId` to neutral `AnimationSubjectId`;
- adapter registers `echo_npc_animation_adapter:face_layers`;
- added graph/channel/state definitions and per-subject channel runtime;
- added ownership leases, explicit event transitions and logical ticking;
- added unit tests for state isolation, channel isolation, ownership conflicts,
  released leases, `HOLD_LAST_FRAME`, `WAIT_FOR_EXIT`, order selection and
  ambiguity.

User/client-pending:

- real integrated-client visual observation of adapter-installed face rendering;
- real adapter-absent fallback visual observation.

## Deferred

- A2: Animation Playback, Channel Blending and Client Synchronization;
- A3: persistence;
- Story/Dialogue animation adapters;
- complete NPC animation synchronization;
- navigation and locomotion.

## Blender authoring workflow 3.0

Completed:

- Action-local metadata and Action pose markers for unified clip authoring;
- procedural clip events, clip-level points and root-motion policy metadata;
- Blender 5.2 Action Editor panels, timeline overlay, preview controller and per-bone loop-seam validation;
- atomic protected export, output preview, batch export and legacy Enter/Loop/Exit Action merge;
- headless Blender export coverage for Idle, One Shot, Stateful Loop and migrated `humanoid_run`;
- production runtime JAR and Prism installation updated for generated `Event` and `RootMotion` intrinsics.

Known limitation:

- Blender preview does not simulate fallback crossfade blending; it previews hard lifecycle phase changes while the
  generated runtime resource still carries the authored `Fallback(Crossfade(...))` policy.
