# UI image project map

Модуль:

`dialogue-forge-1.20.1`

Основные package:

- `forge/client/ui/image`: public image definitions, bounds, renderer, draw commands.
- `forge/client/ui/settings`: tile/fill/sampling/snap settings.
- `forge/client/ui/image/paint`: mode-specific painters and blitter.
- `forge/client/ui/nineslice`: compatibility adapter for existing 9-slice callers.
- `forge/client/ui/dialogue`: dialogue panel background integration.
- `forge/client/ui/debug`: F9 debug screen.

Dialogue integration:

`ClientDialogueRenderer -> ClientDialoguePanelBackground -> UiImageDefinition -> UiImageRenderer`

Отдельный overlay:

`DialoguePanelBackgroundOverlay(...)`

Он не смешан с `DialogueOverlayStyle(...)`, который остаётся за text/layout/colors.
