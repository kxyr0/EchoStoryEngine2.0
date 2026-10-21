# Echo Visibility 0.1.0

Forge 1.20.1 visibility watches evaluated from the player's actual active
render camera. Echo Visibility is independent of Trigger, Story, Dialogue and
Debug; integrations use its public Forge events and service views.

Content lives under:

`<gameDir>/story/content-pack/<namespace>/echo_visibility/watches/**/*.es`

The runtime supports stable Echo NPC targets and authored world-space boxes,
actual projection matrices, first/third-person and detached cameras, clip-space
frustum classification, bounded visual multi-ray occlusion, conservative partial
visibility, frame hysteresis, armed per-player lifecycles, protected reports,
same-frame NPC concealment leases, hot-reload last-known-good, reconnect stream
reset, and SavedData recovery.

Production commands:

- `/echo_visibility arm <watch_id>`
- `/echo_visibility disarm <watch_id>`
- `/echo_visibility assert_out_of_view <watch_id>`
- `/echo_visibility reload`
- `/echo_visibility status`

Development rendering and no-OP client commands are supplied by the separate
`echo-debug-visibility-adapter`.

Story authoring uses the single `View.*` intrinsic surface lowered by Echo
Story Engine and executed by `echo-trigger-visibility-adapter`: `Arm`,
`Disarm`, both waits, both queries, and `AssertStillOutOfView`.
