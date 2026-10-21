# Anchors HUD

Поддерживаемые anchors:

- `TOP_LEFT`;
- `TOP_RIGHT`;
- `BOTTOM_LEFT`;
- `BOTTOM_RIGHT`;
- `CENTER`.

Offset задаётся в координатах reference canvas. После масштабирования к framebuffer применяется pixel snap.
Anchor считается в framebuffer coordinates, а не в Minecraft GUI-scaled coordinates.
