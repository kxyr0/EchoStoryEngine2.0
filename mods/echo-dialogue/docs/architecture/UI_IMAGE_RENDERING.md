# UI image rendering

Render path:

`UiImageDefinition -> UiImageRenderMode -> UiImageRenderer -> mode painter -> UiTextureBlitter -> GuiGraphics`

Роли:

- `UiImageDefinition`: immutable описание texture, source region, mode и settings.
- `UiImageRenderer`: единая public точка render-а.
- `UiSimpleImagePainter`: SIMPLE geometry.
- `UiSlicedImagePainter`: SLICED geometry.
- `UiTiledImagePainter`: TILED geometry с partial tiles и tile limit.
- `UiFilledImagePainter`: FILLED clipping.
- `UiTextureBlitter`: единственная граница с `GuiGraphics.blit`.

`UiNineSliceRenderer` больше не содержит собственную geometry-реализацию. Он adapter к `UiImageRenderer` для старого API.
