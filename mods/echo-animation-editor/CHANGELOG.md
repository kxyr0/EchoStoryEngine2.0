# Changelog

## 0.3.0

- Added axis, plane, rotate-ring and scale-handle gizmo picking.
- Added authoring undo/redo, autosave/recovery, discovery and preview bridge calls.
- Added `/echo_animator autosave|undo|redo|recover|preview|clear_preview|select` commands.
- Added core tests for gizmo picking and delta mapping.

## 0.2.0

- Added optional bridge to the `echo-animation-authoring` public draft API.
- Added viewport gizmo drag keyframe mutation for selected bone, defaulting to `body`.
- Added `/echo_animator save`, `/echo_animator hot_reload` and Ctrl+S save handling.
- Updated docs from the old save/keyframe gap to the 0.2.0 authoring workflow.

## 0.1.0

- Создан первый самостоятельный срез Echo Animator Editor.
- Добавлены contracts/core/Forge/adapters/testmod modules.
- Добавлены команды `/echo_animator open|close|toggle|status`.
- Добавлены layout and GUI scale unit tests.
