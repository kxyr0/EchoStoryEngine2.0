# UI image geometry

Все режимы генерируют deterministic `UiImageDrawCommand`.

Команда содержит:

- texture;
- mode;
- destination rect;
- source rect;
- full texture size.

Unit tests сравнивают эти команды без GPU screenshot.

## Partial tile

Для последнего tile renderer вычисляет остаток destination и source span:

```text
sourceSpan = ceil(destinationSpan / tileScale)
```

При scale `1.0` destination `6 px` использует source `6 px`. Tile не растягивается на остаток.

## Fill clipping

`FILLED` сначала считает полную SIMPLE geometry, затем обрезает source и destination по `amount`. Масштаб изображения
остаётся прежним.
