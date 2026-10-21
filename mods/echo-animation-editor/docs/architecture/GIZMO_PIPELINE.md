# Gizmo Pipeline

0.3.0 has a real handle pipeline:

`AnimatorEditorScreen`
-> `AnimatorSessionGizmo`
-> `AnimatorGizmoPicker`
-> `AnimatorGizmoDeltaMapper`
-> `AnimatorForgeSession`
-> `EchoAuthoringDraftClient.applyKeyframe`.

The picker resolves translate axes, translate planes, rotate rings and scale handles against the GUI-space viewport
rectangle. The delta mapper keeps axis and plane components separate before the session converts them into the
authoring transform.
