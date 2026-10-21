# UI rendering: 9-slice

## Ответственности

`UiNineSliceBorders` отвечает только за fixed borders.

`UiNineSliceTexture` описывает source texture: `ResourceLocation`, полный размер PNG и borders.

`UiNineSliceBounds` описывает target rectangle в GUI coordinates.

`UiNineSliceRenderer` отвечает только за отрисовку. Он не знает про dialogue state, choices, fonts, animation, config
loading или speaker visuals.

`DialoguePanelBackgroundConfig` хранит hot-reload настройки background-а из отдельного
`DialoguePanelBackgroundOverlay(...)` в `.dialogue-overlay.es`.

`ClientDialoguePanelBackground` является boundary между dialogue overlay layout и конкретной реализацией background-а.
Он выбирает solid fill или 9-slice по отдельному `DialoguePanelBackgroundConfigSource`.

## Алгоритм renderer-а

Renderer строит 9 `DrawSlice`:

1. Верхний левый угол.
2. Верхний край.
3. Верхний правый угол.
4. Левый край.
5. Центр.
6. Правый край.
7. Нижний левый угол.
8. Нижний край.
9. Нижний правый угол.

Каждый slice содержит target rectangle и source rectangle. Углы получают одинаковый source/target размер. Края получают
фиксированную толщину и растянутую длину. Центр получает растянутую ширину и высоту.

## Guards

Texture validation:

- `sourceWidth > 0`
- `sourceHeight > 0`
- `left + right < sourceWidth`
- `top + bottom < sourceHeight`

Render validation:

- `targetWidth >= left + right`
- `targetHeight >= top + bottom`

Ошибки являются `IllegalArgumentException`, а не silent fallback. Это нужно, чтобы неверный layout не маскировался
сломано растянутой картинкой.

## Forge 1.20.1 draw path

Renderer использует `GuiGraphics.blit(ResourceLocation, x, y, width, height, u, v, sourceWidth, sourceHeight,
textureWidth, textureHeight)`. Этот же путь уже используется в client visual renderer для масштабирования портретов.

Для alpha renderer включает blend и временно выставляет shader color через `RenderSystem.setShaderColor`.
После draw shader color возвращается в `1,1,1,1`.

## Почему это не отдельный mini-framework

Код расположен рядом с существующим `forge.client` UI/render layer и не вводит дерево виджетов, lifecycle manager или
layout engine. Это узкий primitive для scalable panel/background плюс dialogue boundary, который читает hot-reload config.
