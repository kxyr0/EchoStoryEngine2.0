# Effect Build And Init

`Build()` задает единственный `EffectId`.

```csharp
public override EffectResourceDef Build()
{
    return Effect("jdh:first_morning/daze");
}
```

`Init()` содержит только декларативные вызовы: duration, stacking, lifetime, end conditions, channels и timeline events. Binder не выполняет пользовательский код.
