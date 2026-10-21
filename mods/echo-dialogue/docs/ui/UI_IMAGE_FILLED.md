# UI image FILLED

`FILLED` нужен для progress/HUD изображений. Это linear clipping, а не сжатие картинки.

Поля:

- `fillDirection`: `LEFT_TO_RIGHT`, `RIGHT_TO_LEFT`, `TOP_TO_BOTTOM`, `BOTTOM_TO_TOP`;
- `fillAmount`: число от `0.0` до `1.0`.

Пример: `fillAmount = 0.35`, `LEFT_TO_RIGHT`.

Renderer сначала считает полную geometry, затем оставляет левую часть на 35% ширины. Source region тоже обрезается на
35%, поэтому видимая часть сохраняет тот же масштаб.

`FILLED` в текущей версии работает как SIMPLE geometry. Для sliced progress bar нужно использовать два image:

- background: `SLICED`;
- foreground: `FILLED`.
