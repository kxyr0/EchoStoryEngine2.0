# Rendering HUD

Renderer получает immutable layout snapshot и immutable metric snapshots.

Поток:

```text
HudMetricProvider
-> HudMetricSnapshot
-> HudLayoutSnapshot
-> HudReferenceCanvasTransform
-> HudBarFill
-> GuiGraphics rectangles
```

Renderer не отправляет network requests каждый frame и не обращается к server runtime. Все gameplay values приходят
через providers, а providers обновляются owning модами или адаптерами.

Vanilla hearts и hunger icons скрываются в client pre-render boundary. Dialogue и cutscene overlays могут оставаться
выше HUD через собственный overlay ordering.
