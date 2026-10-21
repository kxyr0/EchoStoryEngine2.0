# UI Image Debug Screen

Press `F9` in game.

The screen shows four rows:

- `SIMPLE`
- `SLICED`
- `TILED`
- `FILLED`

Each row draws several panel sizes. This is the manual acceptance screen for one `UiImageRenderer`
across the supported modes.

What to check:

- `SIMPLE`: the whole texture stretches; corners are expected to distort.
- `SLICED`: corners are preserved; wide/tall samples do not stretch corners.
- `TILED`: edges and center repeat; partial edge tiles fill the panel by default.
- `FILLED`: only part of the image is visible; scale is not compressed.

If the PNG changes, Minecraft usually needs `F3+T`. If `.es` fields such as mode, borders, tile,
or fill settings change, save the overlay and reopen the `F9` screen.
