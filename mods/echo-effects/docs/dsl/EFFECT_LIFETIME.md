# Effect Lifetime

Рабочая сигнатура:

```csharp
Lifetime(INDEPENDENT);
```

Поддерживаются `INDEPENDENT`, `OWNER_BOUND`, `SESSION_BOUND`, `UNTIL_REMOVED`. В EF1 runtime хранит policy в definition; host решает, когда завершать owner-bound effects.
