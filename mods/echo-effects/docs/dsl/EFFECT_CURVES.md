# Effect Curves

Рабочая форма:

```csharp
Curve(
    Key(Seconds(0), 1.0),
    Key(Seconds(1), 0.8, SMOOTH),
    Key(Seconds(6), 1.0, LINEAR)
)
```

Поддерживаются `STEP`, `LINEAR`, `SMOOTH`, `CUBIC`. Ключи должны идти строго по возрастанию effect time.
