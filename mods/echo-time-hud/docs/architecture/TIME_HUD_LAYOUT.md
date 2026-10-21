# Time HUD layout

`echo-time-hud` owns no clock. `TimeHudSnapshotStore` accepts revisioned `EchoTimeClientSnapshotEvent` values from `echo_time`; `TimeHudRenderer` formats the latest server snapshot. There are no HUD network packets and no per-frame config parsing.

`TimeHudLayout` receives physical framebuffer width/height from `Window.getWidth()` and `Window.getHeight()`. Minecraft GUI-scaled dimensions are used only for the final draw-coordinate transform. For reference canvas `(Rw, Rh)` and framebuffer `(Fw, Fh)`:

```text
uniformScale = min(Fw / Rw, Fh / Rh)
canvasSize   = (Rw, Rh) * uniformScale
canvasOrigin = (framebufferSize - canvasSize) / 2
safeRect     = canvas inset by SafeMargin * uniformScale
```

Text width comes from the real Minecraft font for the current string. Its physical bounds are font bounds multiplied by `TextScale * uniformScale`. The selected anchor places those bounds inside the safe rect, authored offsets are multiplied by `uniformScale`, and `ClampToScreen(true)` clamps the final rectangle back into the safe rect.

All nine anchors are implemented: `TOP_LEFT`, `TOP_CENTER`, `TOP_RIGHT`, `CENTER_LEFT`, `CENTER`, `CENTER_RIGHT`, `BOTTOM_LEFT`, `BOTTOM_CENTER`, and `BOTTOM_RIGHT`. For `TOP_RIGHT`, `Offset(x: 0, ...)` keeps the text's right edge on the safe right edge; a negative X moves left. Because anchoring uses the measured width, the right edge remains fixed when glyph width changes.

Drawing converts the final physical origin independently through framebuffer-pixels-per-GUI-unit X/Y and applies the physical text scale through the pose matrix. Consequently GUI scale 1/2/3/4/Auto changes draw coordinates but not the authored physical rectangle. Resize and aspect-ratio changes recompute from the current framebuffer every frame without resetting or resynchronizing time.

The overlay is registered above the vanilla hotbar. Echo Dialogue registers above all, so the Time HUD remains below Dialogue. Rendering requires a loaded world and player; `RespectHideHud(true)` honors F1. `SHOW_DURING_GAME_SCREENS` keeps it visible over in-world screens, while `WORLD_ONLY` requires no open screen.

