# VIS1 + TV1 implementation report

## Camera evidence

`ActiveCameraSnapshotCapture` runs from `RenderLevelStageEvent` at
`AFTER_CUTOUT_BLOCKS`. It reads `RenderSystem.getProjectionMatrix()`, the active
Minecraft `Camera` position/look/up/left vectors, framebuffer dimensions, POV
source and interpolated player eyes. Classification and debug both consume this
same immutable snapshot. No yaw-difference visibility shortcut exists.

## NPC visibility evidence

NPC server resolution uses the public Echo NPC `NpcNavigationBodyLookupEvent`
for one ActorId; client evaluation uses the armed packet's entity ID for O(1)
lookup and interpolated render AABB. Clip-space AABB classification runs before
bounded sample generation. `OCCLUDED` requires every in-viewport visual ray to
be conclusively blocked; uncertain rays remain visible conservatively.

## Rapid-turn evidence

Render-frame ordering is:

```text
frame F / AFTER_CUTOUT_BLOCKS
  camera captured
  VISIBLE -> OUTSIDE_FRUSTUM/OCCLUDED confirmed
  LOST_SIGHT transition created
  PENDING lease installed for exact NpcId
  report sent
frame F / entity rendering
  Echo NPC extension returns CANCELLED
frame F+1 and later
  PENDING/CLAIMED lease still returns CANCELLED even if camera turns back
server completion
  NPC body absent -> COMMITTED; ten-tick failure -> REJECTED
```

The unit suite verifies first-hidden-frame loss and that PENDING/CLAIMED leases
hide until commit. A live visual client run was not performed in this automated
pass, so an observed hardware frame count is not claimed.

## Zone evidence

Box definitions retain exact dimension and AABB, including negative
coordinates. They use the same frustum/sampling path and never create a generic
world concealment. `assert_out_of_view` rejects reports older than two server
ticks.

## Trigger evidence

The real Echo Trigger binder emits exact selectors for `LostSightOf`,
`GainedSightOf` and `TargetIsOutOfView`. The adapter index key is
`echo_visibility:events|<transition>|<watchId>`. Trigger owner identity includes
PlayerRef, watch ID and generation; command and Story execution IDs derive from
the stable VisibilityEventId. SavedData persists claim and terminal routing
result so a restart cannot execute the same event twice.

Echo Story Engine lowers the seven `View.*` intrinsics to neutral
`echo_visibility:*` external actions. The adapter provides idempotent recovery
for arm/disarm, durable-identity recovery for pending waits, boolean predicate
outputs, and the two-tick freshness assertion before environment mutation.

## Debug evidence

The separate Echo Debug Visibility Adapter consumes only
`VisibilityClientDebugView`. World rendering includes player-eye and camera
vectors, a 24-block frustum from the inverse exact projection, AABB, samples,
rays and occluder hit points. The clipped HUD shows source/FOV/aspect, state,
frustum reason, hysteresis, report/ack, transition and concealment.

## Performance

- full world/entity scans per frame: `0`;
- Trigger catalog lookups per frame: `0`;
- Trigger index rebuilds: only on catalog revision;
- reports while stable state is unchanged: `0` (unit tested);
- parser/binder calls after changing one watch: `1 / 1` (unit tested);
- active watch limit: `32` per player;
- samples and rays: bounded by authored `VisibilitySamplePolicy`.

## Source shape

The repository `sourceShapeCheck` enforces and reports:

- Java files over 180 logical LOC: `0`;
- leaf Java packages over 7 files: `0`;
- EchoScript files over 180 logical LOC: `0`.

## Honest acceptance boundary

Automated compilation, unit checks and static source constraints are recorded
here. A real Forge client visual pass, detached cutscene-camera integration run,
wall/partial-edge scene and isolated final-JAR client/server run still require a
Minecraft instance and are not represented as completed by this report.
