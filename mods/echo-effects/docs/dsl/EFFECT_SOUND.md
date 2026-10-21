# Effect Sound

Рабочая сигнатура:

```csharp
Sound.PlayAt(time: Seconds(0), sound: "minecraft:block.beacon.ambient", volume: 0.25, pitch: 0.65);
```

`Sound.PlayAt` компилируется как one-shot timeline event. Повторное срабатывание должен блокировать ledger runtime/host.
