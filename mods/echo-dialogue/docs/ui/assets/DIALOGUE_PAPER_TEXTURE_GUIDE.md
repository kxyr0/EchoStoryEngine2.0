# Dialogue paper texture guide

Текущая тестовая texture:

`jdh:textures/echo_dialogue/gui/panel/test_dialogue_panel.png`

Live Prism путь:

`story/content-pack/jdh/assets/jdh/textures/echo_dialogue/gui/panel/test_dialogue_panel.png`

Размер: `29x28 px`.

Базовые borders для текущего asset:

- `left = 6`
- `right = 5`
- `top = 6`
- `bottom = 8`

ASCII-сетка:

```text
x 0..5   | x 6..23 | x 24..28
y 0..5   top-left    top      top-right
y 6..19  left        center   right
y 20..27 bottom-left bottom   bottom-right
```

В `SLICED` edge/center regions растягиваются. В `TILED` эти же regions повторяются.

Для бесшовного `TILED` художнику нужно сделать:

- `x=6..23, y=0..5` бесшовным по X;
- `x=6..23, y=20..27` бесшовным по X;
- `x=0..5, y=6..19` бесшовным по Y;
- `x=24..28, y=6..19` бесшовным по Y;
- `x=6..23, y=6..19` бесшовным по X и Y.

Уникальные угловые детали рисовать только в corner regions. Уникальные царапины внутри repeatable edge будут
повторяться в `TILED` и растягиваться в `SLICED`.
