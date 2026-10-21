# Effect Screen

Рабочие сигнатуры:

```csharp
Screen.Tint(color: Color("#6A7180"), opacity: Curve(Key(Seconds(0), 0.0), Key(Seconds(6), 0.24)), layer: BELOW_UI);
Screen.Vignette(color: Color("#11131A"), opacity: Curve(Key(Seconds(0), 0.0), Key(Seconds(6), 0.30)), layer: BELOW_UI);
```

Цвет проверяется как `#RRGGBB`. Opacity хранится как channel curve.
