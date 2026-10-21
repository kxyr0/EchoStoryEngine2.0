# Animator Editor Project Map

`/echo_animator open`
-> `AnimatorEditorCommands`
-> `AnimatorEditorClient`
-> `AnimatorForgeSession`
-> `AnimatorEditorScreen`
-> `AnimatorResponsiveLayout`
-> `AnimatorCameraController`
-> `EchoAnimationPublicApiProbe`
-> `EchoNpcPublicApiProbe`
-> cleanup через `AnimatorCameraStateSnapshot`.

`/echo_animator save`, Ctrl+S, undo/redo, autosave/recovery, preview and viewport gizmo drag route through
`EchoAuthoringDraftClient`. The client uses an optional reflection bridge to `echo-animation-authoring`:

`EchoAuthoringDraftClient`
-> `AnimationAuthoringServiceBridge`
-> `AnimationAuthoringService`
-> existing Echo Animation compiler probe
-> atomic `.animation-clip.es` workspace write.

`/echo_animator hot_reload` calls the authoring hook after save. The actual resource reload remains file-change driven
by the Echo Animation watcher; the editor does not ship a second parser/compiler/runtime.

Gizmo editing flows through `AnimatorSessionGizmo`, `AnimatorGizmoPicker` and `AnimatorGizmoDeltaMapper` before a
keyframe mutation is sent to authoring.
