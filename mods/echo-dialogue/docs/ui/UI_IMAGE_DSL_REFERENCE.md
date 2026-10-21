# UI image DSL reference

Dialogue panel background использует отдельный overlay:

`story/configs/jdh/echo_dialogue/client_overlay/dialogue_panel_background.dialogue-overlay.es`

Choice row backgrounds use the same image DSL through a separate overlay:

`story/configs/jdh/echo_dialogue/client_overlay/dialogue_choice_background.dialogue-overlay.es`

Use `DialogueChoiceBackgroundOverlay(...)` with the same fields as `DialoguePanelBackgroundOverlay(...)`.
If the choice overlay is absent or uses `mode: "solid"`, choices fall back to `choiceBackgroundColor`
and `choiceBorderColor` from `DialogueOverlayStyle(...)`.

Пример:

```es
return DialoguePanelBackgroundOverlay(
    mode: "sliced",
    texture: "jdh:textures/echo_dialogue/gui/panel/test_dialogue_panel.png",
    textureWidth: 29,
    textureHeight: 28,
    sliceLeft: 6,
    sliceRight: 5,
    sliceTop: 6,
    sliceBottom: 8,
    tileScaleX: "1.0",
    tileScaleY: "1.0",
    tileOriginX: 0,
    tileOriginY: 0,
    tileFullOnly: false,
    fillDirection: "LEFT_TO_RIGHT",
    fillAmount: "0.55"
);
```

`mode` поддерживает:

- `"simple"`
- `"sliced"`
- `"tiled"`
- `"filled"`

`"nine_slice"` пока принимается как compatibility alias для `sliced`, но новый config должен использовать `sliced`.
