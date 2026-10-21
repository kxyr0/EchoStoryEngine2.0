# Blender item animation markers

`echo-animation-exporter-3.3.1-ru-system-sit.zip` now exposes four buttons in the
`Events and Points` panel: Attach Left, Attach Right, Release, and Detach. They write the exact
Action marker IDs `echo_item_attachments:attach_left_hand`,
`echo_item_attachments:attach_right_hand`, `echo_item_attachments:release`, and
`echo_item_attachments:detach` on the current frame.

During export, frame-to-tick conversion uses the add-on's existing timeline conversion. The
marker is assigned to ENTER, MAIN, or EXIT and emitted in the generated `.animation.es`, for
example:

```csharp
Marker("echo_item_attachments:attach_right_hand", phase: MAIN, tick: 8);
Marker("echo_item_attachments:release", phase: MAIN, tick: 14);
```

The procedural Echo Animation compiler accepts this generic `Marker` intrinsic and produces
`AnimationMarkerDefinition`; it is not item-specific runtime code. The add-on smoke test
`tests/test_item_markers.py` verifies exact IDs and ticks. Do not rename these markers after
binding: runtime validation also checks playback ID, playback revision, and phase.
