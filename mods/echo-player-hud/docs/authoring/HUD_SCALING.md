# Масштабирование HUD

HUD использует reference canvas `1920x1080` и текущий framebuffer.

Поток:

```text
reference canvas
-> current framebuffer
-> anchor
-> scaled offset
-> pixel snap
-> final framebuffer bounds
```

Minecraft GUI Scale влияет только на перевод framebuffer bounds в координаты `GuiGraphics`. Поэтому при одинаковом
разрешении final framebuffer X/Y/W/H не меняются для GUI Scale `1`, `2`, `3`, `4` и `Auto`.

Проверка:

```text
/echo_hud bounds
```
