# Animation timeline

`AnimationAuthoringService` exposes the public draft boundary for editor integrations. A draft accepts keyframe
mutations, supports undo/redo snapshots, keeps source dirty until save, and writes canonical `.animation-clip.es` files
under the configured workspace.

Save uses atomic file replacement after structural validation and an Echo Animation compiler probe when the runtime
compiler class is present. Echo Animation hot reload is file-change driven: when the workspace is the usual
`<gameDir>/story/content-pack`, the existing Echo Animation watcher reloads saved animation resources.

Animation files use `.face-animation.es` under `data/jdh/echo_animation/face_animations/`.

A0 supports `Frame`, `Track`, `STEP`, `LINEAR`, `HOLD`, `RETURN_TO_BASE` and `LOOP`.

Autosave/recovery uses an authoring sidecar properties file beside the target animation resource. It deliberately does
not parse `.animation-clip.es`, so recovery does not introduce a second animation parser.

Saved clips use the procedural `void Build()` form with `Id`, `Rig`, `Duration`, `Track` and integer-tick `Key`
statements. The editor no longer creates legacy `return AnimationClip(...)` resources; the runtime compiler remains
the semantic authority for the generated source.

Interactive timeline UI still needs manual Minecraft acceptance.
