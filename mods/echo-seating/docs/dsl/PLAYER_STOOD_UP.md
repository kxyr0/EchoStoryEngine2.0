# Player.StoodUp

Trigger selectors:

```csharp
Subscribe(Player.StoodUpIn(BreakfastTable));
Subscribe(Player.StoodUpFrom(BreakfastTable, MothersSeat));
```

`StoodUpIn` срабатывает при подтвержденном выходе игрока из occupancy в зоне. `StoodUpFrom` требует previous named slot.
