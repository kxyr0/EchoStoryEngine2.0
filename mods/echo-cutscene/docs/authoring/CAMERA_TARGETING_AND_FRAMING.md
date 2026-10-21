# Camera Targeting And Framing

Echo Cutscene 0.2.x keeps legacy `.camera.es` files compatible. A file with only
`key <time_ms> <x> <y> <z> <yaw> <pitch> <fov>` lines is treated as `FREE`.

Schema 2 may also contain:

```text
schema 2
mode 0 LOOK_AT_WITH_OFFSET
target 0 PLAYER EYES LIVE
framing 0 0.15 -0.1
key 0 10 65 10 0 0 70
key 1000 12 66 11 0 0 70
```

Modes:

- `FREE`: mouse controls camera yaw and pitch.
- `LOOK_AT`: camera position is authored, yaw and pitch are computed from the target.
- `LOOK_AT_WITH_OFFSET`: camera looks at the target and mouse changes screen-space framing.

Authoring commands from Echo Debug:

- `/echo_debug cutscene enter jdh:story_telling_sister_1 0.45`
- `/echo_debug cutscene mode LOOK_AT`
- `/echo_debug cutscene mode LOOK_AT_WITH_OFFSET`
- `/echo_debug cutscene target player EYES`
- `/echo_debug cutscene target player HEAD`
- `/echo_debug cutscene target fixed <x> <y> <z>`
- `/echo_debug cutscene target clear`

The same authoring actions are also available directly on Echo Cutscene:

- `/echo_cutscene enter jdh:story_telling_sister_1 0.45`
- `/echo_cutscene mode LOOK_AT`
- `/echo_cutscene target player EYES`
- `/echo_cutscene target fixed <x> <y> <z>`
- `/echo_cutscene target clear`

`F4` starts and stops recording. `F6` keeps the existing pitch-flight toggle. The
`F7` key sets a `FIXED_POINT` target from the detached camera raycast. The hit
uses the exact block hit position; if the ray misses, the point is placed forward
along the camera view direction. The camera mode key is registered as
`key.echo_debug.cutscene_camera_mode` and is unbound by default, so bind it
manually if you want key cycling.
