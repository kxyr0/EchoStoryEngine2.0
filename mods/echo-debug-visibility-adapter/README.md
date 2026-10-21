# Echo Debug Visibility Adapter

Development-only Echo Debug integration for VIS1. It consumes the public
`VisibilityClientDebugView`; it neither recomputes visibility nor owns a second
debug engine.

Client commands (no OP required):

- `/echo_visibility debug player on|off`
- `/echo_visibility debug watch <watch_id>|off`
- `/echo_visibility debug all on|off`
- `/echo_visibility debug status`

The world overlay draws player-eye and active-camera vectors, a 24-block
frustum derived by inverting the exact runtime projection matrix, target AABBs,
sample points, visual rays, hit points, and status-specific colors. The bounded
HUD shows actual camera source/FOV/aspect, state, frustum classification,
hysteresis counters, report acknowledgement, transition reason, and concealment.
