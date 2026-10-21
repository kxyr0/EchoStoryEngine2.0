# UI Image Render Modes

`UiImageRenderer` is the single entry point for UI image drawing. It accepts `UiImageDefinition`,
`UiImageBounds`, and chooses the painter from `UiImageRenderMode`.

Supported modes:

- `SIMPLE`: stretches the whole source region into the destination.
- `SLICED`: 9-slice; fixed corners, stretched edges and center.
- `TILED`: fixed corners, repeated edges and center; partial edge tiles are clipped by default.
- `FILLED`: linear clipping for progress/HUD; the image is not compressed.

The dialogue panel uses the same renderer through `ClientDialoguePanelBackground`. The legacy
`UiNineSliceRenderer` stays as a `SLICED` adapter so existing debug/dialogue code remains stable.

## Choosing a mode

- Background, icon, one-piece book/page: `SIMPLE`.
- Frame with fixed corners: `SLICED`.
- Repeated ornament or paper texture: `TILED`.
- Health/progress/hold timer: `FILLED`.

## Minimal Java Example

```java
UiImageDefinition image = UiImageDefinition.sliced(
        texture,
        29,
        28,
        new UiSliceBorders(6, 5, 6, 8));

new UiImageRenderer().render(graphics, image, new UiImageBounds(x, y, width, height), alpha);
```
