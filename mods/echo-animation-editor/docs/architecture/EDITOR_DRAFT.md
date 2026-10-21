# Editor Draft

`AnimatorDraftState` tracks local editor revision and dirty flag. The real animation draft is owned by
`echo-animation-authoring` and is accessed through `EchoAuthoringDraftClient`.

Viewport LMB drag applies an authoring keyframe mutation, marks the local draft dirty and increments the editor draft
revision. Ctrl+S or `/echo_animator save` saves the authoring draft and marks the local draft clean.
