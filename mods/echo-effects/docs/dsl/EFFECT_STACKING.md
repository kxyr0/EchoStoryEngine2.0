# Effect Stacking

Рабочая сигнатура:

```csharp
Stacking(RESTART);
```

Поддерживаются `IGNORE_NEW`, `REFRESH_DURATION`, `RESTART`, `STACK_INTENSITY`, `ALLOW_MULTIPLE`, `REPLACE_OLD`.

`EffectId` не является единственным ключом active instance: runtime создает отдельный `EffectInstanceId`.
