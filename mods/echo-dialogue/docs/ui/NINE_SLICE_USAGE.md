# 9-slice UI: использование

## Отдельный hot-reload overlay

Фон dialogue panel настраивается не в `DialogueOverlayStyle(...)`, а в отдельном overlay-файле:

`story/configs/jdh/echo_dialogue/client_overlay/dialogue_panel_background.dialogue-overlay.es`

Choice row backgrounds use a separate hot-reload overlay:

`story/configs/jdh/echo_dialogue/client_overlay/dialogue_choice_background.dialogue-overlay.es`

`DialogueChoiceBackgroundOverlay(...)` supports the same `mode`, `texture`, slice, tile and fill fields.
Without that overlay, choice rows fall back to `DialogueOverlayStyle(...)` choice colors.

Оба client overlay source-а сканируют `story/configs/**/*.dialogue-overlay.es` примерно раз в 300 мс:

- `DialogueOverlayStyle(...)` отвечает за текст, layout, цвета и typewriter;
- `DialoguePanelBackgroundOverlay(...)` отвечает только за background панели.

Минимальный background overlay:

```es
return DialoguePanelBackgroundOverlay(
    mode: "nine_slice",
    texture: "jdh:textures/echo_dialogue/gui/panel/test_dialogue_panel.png",
    textureWidth: 29,
    textureHeight: 28,
    sliceLeft: 6,
    sliceRight: 5,
    sliceTop: 6,
    sliceBottom: 8
);
```

`mode` принимает:

- `"solid"`: рисовать `backgroundColor` из `DialogueOverlayStyle(...)`;
- `"nine_slice"`: рисовать `texture` по slice-настройкам.

Если отдельного `DialoguePanelBackgroundOverlay(...)` нет, используется прежний solid background.

## Как добавить новую панель

1. Положить PNG в assets content pack, например:
   `story/content-pack/jdh/assets/jdh/textures/echo_dialogue/gui/panel/my_panel.png`.
2. В `texture` указать полный texture `ResourceLocation`, включая `textures/...png`.
3. В `textureWidth` и `textureHeight` указать полный размер PNG в пикселях.
4. В `sliceLeft`, `sliceRight`, `sliceTop`, `sliceBottom` указать borders.
5. Сохранить отдельный `.dialogue-overlay.es` и открыть диалог заново или нажать `F9`.

## Java boundary

Dialogue UI вызывает один boundary:

```java
ClientDialoguePanelBackground.render(
        graphics,
        UiNineSliceBounds.fromEdges(panel.left(), panel.top(), panel.right(), panel.bottom()),
        ClientDialogueOverlayStyle.current(),
        state.panel().alpha());
```

`ClientDialoguePanelBackground` сам читает отдельный background overlay. `DialogueOverlayStyleConfig` остаётся style
config-ом и не хранит 9-slice texture fields.

## Как выбирать borders

Borders нужно выбирать так, чтобы:

- скругления и фиксированный декор целиком попадали в углы;
- верхний и нижний края могли растягиваться по горизонтали;
- левый и правый края могли растягиваться по вертикали;
- центр мог растягиваться по двум осям.

Для текущей тестовой текстуры `29x28 px`:

- `left = 6`
- `right = 5`
- `top = 6`
- `bottom = 8`

Renderer растягивает slice-области. Он не тайлит повторяющийся орнамент.

## Типичные ошибки

- Target width меньше `left + right`, или target height меньше `top + bottom`.
- Borders режут скруглённый угол.
- Декоративные пиксели попали в center, поэтому на больших панелях они растягиваются.
- `texture` указан без namespace или без `textures/...png`.
- PNG лежит в mod JAR, а `.es` ссылается на namespace content pack, или наоборот.
