# Undo Redo

Undo/redo is wired in 0.3.0 through the authoring draft history.

- Ctrl+Z or `/echo_animator undo` restores the previous authoring draft snapshot.
- Ctrl+Y or `/echo_animator redo` reapplies the next snapshot.

The editor marks the local draft dirty after successful undo/redo and reapplies the preview hook.
