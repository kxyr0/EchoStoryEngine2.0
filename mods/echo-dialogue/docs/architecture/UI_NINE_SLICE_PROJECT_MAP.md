# UI 9-slice: карта проекта

## Модуль

Изменения находятся в репозитории `mods/echo-dialogue`, модуль `dialogue-forge-1.20.1`.

9-slice зависит от Minecraft client GUI API, поэтому он живёт только в Forge client module. Core dialogue contracts,
persistence и EchoScript compilation не получают зависимость на Minecraft или Forge.

## Production code

- `forge/client/ui/nineslice/UiNineSliceBorders.java`: хранит fixed `left/right/top/bottom`.
- `forge/client/ui/nineslice/UiNineSliceBounds.java`: хранит target `x/y/width/height`.
- `forge/client/ui/nineslice/UiNineSliceTexture.java`: хранит `ResourceLocation`, размер PNG и borders.
- `forge/client/ui/nineslice/UiNineSliceRenderer.java`: строит 9 slices и рисует их через `GuiGraphics.blit`.
- `forge/client/config/DialoguePanelBackgroundConfig.java`: данные panel mode, texture, size и borders.
- `forge/client/config/DialoguePanelBackgroundConfigParser.java`: читает `DialoguePanelBackgroundOverlay(...)`.
- `forge/client/config/DialoguePanelBackgroundConfigSource.java`: hot-reload source отдельного background overlay.
- `forge/client/ui/dialogue/ClientDialoguePanelBackground.java`: boundary для background-а dialogue panel.
- `forge/client/ui/debug/UiNineSliceDebugScreen.java`: F9-preview текущего background overlay.
- `forge/client/ui/debug/UiNineSliceDebugInput.java`: client key binding `F9`.

## Runtime config

Обычный overlay остаётся для текста/layout/colors:

`story/configs/jdh/echo_dialogue/client_overlay/dialogue_overlay.dialogue-overlay.es`

Background вынесен в отдельный overlay:

`story/configs/jdh/echo_dialogue/client_overlay/dialogue_panel_background.dialogue-overlay.es`

Поля внутри `DialoguePanelBackgroundOverlay(...)`:

```es
mode: "nine_slice",
texture: "jdh:textures/echo_dialogue/gui/panel/test_dialogue_panel.png",
textureWidth: 29,
textureHeight: 28,
sliceLeft: 6,
sliceRight: 5,
sliceTop: 6,
sliceBottom: 8
```

Если отдельного background overlay нет, используется прежний solid background.

## Resources

Встроенная dev-текстура:

`dialogue-forge-1.20.1/src/main/resources/assets/echo_dialogue/textures/gui/panel/test_dialogue_panel.png`

Для быстрой проверки в Prism texture лучше класть в content pack namespace:

`story/content-pack/jdh/assets/jdh/textures/echo_dialogue/gui/panel/test_dialogue_panel.png`

И ссылаться из отдельного background overlay так:

`jdh:textures/echo_dialogue/gui/panel/test_dialogue_panel.png`

## Dialogue render path

`ClientDialogueRenderer` имеет две ветки панели:

- `renderLine(...)`: обычная реплика.
- `renderChoicePanelContent(...)`: prompt + choices.

Обе ветки вызывают `ClientDialoguePanelBackground.render(...)`. Этот boundary читает отдельный
`DialoguePanelBackgroundOverlay(...)`, поэтому замена background-а не меняет layout, animation state, text wrapping,
choice hitboxes или speaker visual rendering.
