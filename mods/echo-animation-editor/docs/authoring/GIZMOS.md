# Gizmos

The viewport draws axes, plane boxes, a diagonal Z handle and rotate rings. In 0.3.0, LMB drag starts only when the
gizmo picker resolves a concrete handle.

Supported handles:

- translate X/Y/Z and XY/XZ/YZ planes;
- rotate X/Y/Z rings;
- scale X/Y/Z and uniform scale.

Each completed drag writes a keyframe mutation to the authoring draft and applies the authoring preview hook.
