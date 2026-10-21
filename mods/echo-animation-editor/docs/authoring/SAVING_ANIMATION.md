# Saving Animation

Saving is enabled when `echo-animation-authoring` is loaded with its public draft API.

Ctrl+S and `/echo_animator save` call the authoring service through `EchoAuthoringDraftClient`. The authoring module
renders the draft as `.animation-clip.es`, validates it with the existing Echo Animation compiler and atomically replaces
the workspace resource.

`/echo_animator hot_reload` calls the authoring hot reload hook. The real reload is still performed by the Echo Animation
file watcher after the saved resource changes.

Autosave is separate from save. It writes an authoring sidecar and never replaces `.animation-clip.es`.
