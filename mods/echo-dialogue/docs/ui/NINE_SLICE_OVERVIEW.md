# 9-slice UI: обзор

9-slice нужен для UI-панелей, которые меняют размер без растягивания углов. Исходная текстура делится на 9 областей:
4 угла, 4 края и центр.

Углы рисуются в исходном размере. Верхний и нижний края растягиваются только по X, левый и правый края растягиваются
только по Y, центр растягивается по двум осям.

## Где лежит код

- Renderer: `dialogue-forge-1.20.1/src/main/java/io/github/kxyr0/echodialogue/forge/client/ui/nineslice/UiNineSliceRenderer.java`
- Borders: `dialogue-forge-1.20.1/src/main/java/io/github/kxyr0/echodialogue/forge/client/ui/nineslice/UiNineSliceBorders.java`
- Texture def: `dialogue-forge-1.20.1/src/main/java/io/github/kxyr0/echodialogue/forge/client/ui/nineslice/UiNineSliceTexture.java`
- Target bounds: `dialogue-forge-1.20.1/src/main/java/io/github/kxyr0/echodialogue/forge/client/ui/nineslice/UiNineSliceBounds.java`
- Dialogue boundary: `dialogue-forge-1.20.1/src/main/java/io/github/kxyr0/echodialogue/forge/client/ui/dialogue/ClientDialoguePanelBackground.java`
- Dev screen: `dialogue-forge-1.20.1/src/main/java/io/github/kxyr0/echodialogue/forge/client/ui/debug/UiNineSliceDebugScreen.java`

## Первый asset

Встроенная dev-текстура:

`dialogue-forge-1.20.1/src/main/resources/assets/echo_dialogue/textures/gui/panel/test_dialogue_panel.png`

Размер: `29x28 px`.

Текущие borders:

- `left = 6`
- `right = 5`
- `top = 6`
- `bottom = 8`

Эти значения подобраны под реальные пиксели текущей картинки. Вариант `4/4/4/4` режет скругления и нижнюю тень.

## Hot reload

Живая игра читает настройки панели из:

`story/configs/**/*.dialogue-overlay.es`

Источник конфига сканирует папку примерно раз в 300 мс. Для проверки нужно изменить отдельный
`DialoguePanelBackgroundOverlay(...)`, сохранить `.es`, затем открыть диалог заново или нажать `F9` для debug-preview.

## Невозможные размеры

Renderer не чинит невозможный размер молча. Если итоговая ширина меньше `left + right`, или итоговая высота меньше
`top + bottom`, будет `IllegalArgumentException`.

Для текущей текстуры минимальный target:

- ширина: `11 px`
- высота: `14 px`
