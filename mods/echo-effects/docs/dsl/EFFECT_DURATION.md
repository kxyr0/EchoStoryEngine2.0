# Effect Duration

Рабочие сигнатуры:

```csharp
Duration(Ticks(120));
Duration(Seconds(6));
Duration(InfiniteDuration());
```

Finite duration измеряется server ticks. Нулевые и отрицательные значения отклоняются validator-ом.
