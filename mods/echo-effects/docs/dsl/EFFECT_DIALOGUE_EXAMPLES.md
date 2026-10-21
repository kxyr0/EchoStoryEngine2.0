# Effect Dialogue Examples

Dialogue usage:

```csharp
private EffectId FirstMorningDaze = "jdh:first_morning/daze";

Effect.ApplyToPlayer("jdh:first_morning/daze");
Line("...");
```

Для именованных variables host должен передать resolved `EffectId` в generic command payload. Runtime не выбирает случайного player.
