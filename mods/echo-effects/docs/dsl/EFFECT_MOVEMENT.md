# Effect Movement

Рабочая сигнатура:

```csharp
Movement.SpeedMultiplier(Curve(Key(Seconds(0), 1.0), Key(Seconds(6), 0.82)));
```

Forge runtime применяет итоговый multiplier через собственный transient `AttributeModifier`. Чужие modifiers не перезаписываются.
