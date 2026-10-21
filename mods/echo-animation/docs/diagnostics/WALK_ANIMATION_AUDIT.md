# Walk Animation Audit

Date: 2026-07-28

## Findings

- Body animation sampling was render-visible at server tick granularity because the client pose resolver accepted only `long sampleTick`.
- Playback advancement also floored scaled elapsed ticks, so non-1.0 speeds could not produce smooth in-between poses.
- Normalized body clips only supported `STEP` and `LINEAR`; walk clips therefore looked like segmented linear poses rather than vanilla-style limb swing.
- The NPC locomotion adapter could start/stop the walk animation from Echo Navigation lifecycle events, but it had no public way to speed-sync the loop to actual movement.
- Echo Debug was receiving movement snapshots through the navigation lifecycle, but the in-game trail pipeline had no health counters to distinguish integration, packet, collector, and renderer failures.

## Changes

- Added normalized interpolation curves: `STEP`, `LINEAR`, `SMOOTH`, `CUBIC`.
- Added `AnimationClipDefinition` metadata: `schemaVersion`, `loopSeam`, `authoredSpeedBlocksPerSecond`.
- Added EchoScript support for `AnimationClip(... schemaVersion, loopSeam, authoredSpeedBlocksPerSecond ...)`.
- Updated body and face samplers to use non-linear curves; body `CUBIC` uses auto Hermite tangents and shortest-path Euler angles.
- Updated client pose resolving to sample with `double` render ticks and Forge `partialTick`.
- Added `AnimationService.playAnimation(..., playbackSpeed)` and `updateAnimationPlaybackSpeed(...)`.
- Added persistent `PlaybackSpeed` to animation runtime NBT and snapshot writing.
- Updated the NPC locomotion adapter to derive actual blocks/second from `NavigationProgressSnapshot`, start walk with that multiplier, and update active playback speed through a reflection bridge.
- Updated `jdh:humanoid_walk_loop` to schema 2, `loopSeam: "SMOOTH"`, `authoredSpeedBlocksPerSecond: "1.35"`, and cubic limb keyframes.
- Added Echo Debug trail health counters and `/echo_debug trails status`.

## In-Game Checks

1. Start an NPC move from a dialogue or story action.
2. Verify `latest.log` contains `ENPC-ANIM-LOCOMOTION-START ... speed=...`.
3. While the NPC moves, run `/echo_debug trails status`.
4. Expected status: `integration=available`, `packets` increasing, `accepted` increasing, `render calls` increasing, and `segments` greater than zero when the trail has at least two visible points.
5. If trails are missing, use the status output to identify the failed stage before changing renderer/path code.

## Remaining Manual Acceptance

Manual Minecraft acceptance must still be done in the Prism instance because this audit was produced from code and build verification, not from a live rendered client capture.
