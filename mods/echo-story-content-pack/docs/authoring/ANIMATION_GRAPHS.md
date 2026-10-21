# Animation Graphs

A1 introduces the Echo Animation graph model used by per-subject runtime state.

## Resource Path

Graph resources use:

```text
data/<namespace>/echo_animation/animation_graphs/**/*.animation-graph.es
```

Existing A0 resources remain:

- `data/<namespace>/echo_animation/face_rigs/**/*.face-rig.es`;
- `data/<namespace>/echo_animation/face_poses/**/*.face-pose.es`;
- `data/<namespace>/echo_animation/face_animations/**/*.face-animation.es`.

## Authoring Rules

Graphs are explicit:

- declare channels;
- declare states;
- declare playback mode;
- declare interruption policy;
- declare transitions by event;
- do not rely on implicit idle transitions.

Existing subjects remain pinned to their graph revision after reload. New
subjects receive the newest valid revision.

## A1 Limitation

The runtime state core exists in Echo Animation A1. Full content-pack
`.animation-graph.es` EchoScript binding and exact incremental parser-count
acceptance remain limited and should be completed before authoring production
graph resources.
