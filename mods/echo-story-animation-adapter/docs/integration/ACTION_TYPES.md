# Story Action Types

`echo_animation:play_animation`

Required payload:

- `animation`: authored `AnimationId`.
- `target`: logical `AnimationSubjectId`.

Optional payload:

- `await`: `STARTED` or `COMPLETED`.

Default `await`:

- `ONCE` animation main mode: `COMPLETED`.
- `LOOP_UNTIL_STOP` and `HOLD_UNTIL_STOP`: `STARTED`.

Explicit `await: "COMPLETED"` is rejected for non-`ONCE` animations because the same
Story action would otherwise wait forever before a later stop action can run.

`echo_animation:stop_animation`

Required payload:

- `animation`
- `target`

The adapter requests graceful stop and completes Story after terminal outcome.

`echo_animation:wait_animation`

Required payload:

- `animation`
- `target`

The adapter waits for an unambiguous active playback to become terminal.

Example generic Story DSL:

```csharp
Action(
    "play_wave",
    type: "echo_animation:play_animation",
    animation: "jdh:wave",
    target: "jdh:john_father",
    await: "STARTED",
    output: "wave_start",
    next: "stop_wave");
```
