# Workspace

Workspace root is the content-pack repository. In A0 it is configured with JVM property `echoAnimationAuthoring.workspace`.

A write target must resolve inside the configured workspace after absolute normalization.

If workspace is absent, authoring reports: `Workspace не настроен; запись запрещена.`

`/echo_animation_authoring workspace` reports the normalized workspace or records a diagnostic when it is absent/invalid.

The tool must never write generated skins, migrated EchoScript or editor state outside the workspace.
