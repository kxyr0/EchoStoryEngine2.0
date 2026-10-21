# Effect Camera

Рабочие сигнатуры:

```csharp
Camera.FovOffset(Curve(Key(Seconds(0), 0.0), Key(Seconds(6), -8.0)));
Camera.Roll(Curve(Key(Seconds(0), 0.0), Key(Seconds(6), 4.0)));
Camera.Shake(amplitude: Curve(Key(Seconds(0), 0.0), Key(Seconds(6), 0.2)), frequency: 7.0, seed: STABLE);
```

Core компилирует каналы. Client-side визуальная синхронизация выделена отдельной частью Forge host.
