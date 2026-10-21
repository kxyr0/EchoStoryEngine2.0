# Autosave And Recovery

Autosave/recovery is implemented in 0.3.0 through an authoring sidecar file.

Autosave runs automatically every 600 client ticks while the editor draft is dirty. It can also be triggered manually
with Ctrl+Shift+S or `/echo_animator autosave`.

`/echo_animator recover` and Ctrl+R reopen the active clip from the sidecar draft state. Recovery does not parse
`.animation-clip.es` and does not write production animation resources.
