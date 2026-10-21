# Echo Story Animation Adapter

Independent Forge adapter that lets Echo Story actions control semantic Echo Animation
`AnimationId` values through public APIs only.

Action types:

- `echo_animation:play_animation`
- `echo_animation:stop_animation`
- `echo_animation:wait_animation`

The adapter does not expose clips, channels, leases, graph state, renderer classes, or NPC
implementation classes to Story authoring.

Authoring uses the existing generic Story `Action(...)` syntax. See
`docs/integration/ACTION_TYPES.md` and the content-pack example
`data/jdh/echo_story/stories/chapter_1/home/home_animation_lifecycle.es`.
