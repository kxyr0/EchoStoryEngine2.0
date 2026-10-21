# Visibility Debug Provider

Echo Visibility captures and classifies. Its public client lookup event exposes
the same immutable camera and watch snapshots used by runtime. This adapter is
the Echo Debug renderer/command provider only.

```text
VisibilityClientDebugLookupEvent
  -> exact runtime camera/watch snapshot
  -> world lines: eyes, camera, projection-derived frustum, bounds, rays, samples
  -> clipped HUD: state, reason, hysteresis, report/ack, concealment, performance
```

Debug rendering never changes a watch, creates a report, performs an occlusion
raycast, or derives a second approximate FOV.
