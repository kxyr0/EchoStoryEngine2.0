# Effect.RemoveFromPlayer

Рабочие формы:

```csharp
Effect.RemoveFromPlayer("jdh:first_morning/daze");
Effect.RemoveFromPlayer("jdh:first_morning/daze", fadeOut: Ticks(10));
```

Команда неблокирующая после подтвержденного перехода в removal/fade-out. Target берется только из execution context host-а.
