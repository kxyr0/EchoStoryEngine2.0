# Player.SeatedAt

Trigger selector:

```csharp
Subscribe(Player.SeatedAt(BreakfastTable, MothersSeat));
```

Срабатывает только когда event содержит exact `SeatSlotId`. `SeatedIn` не доказывает совпадение с slot; для seat-specific story logic всегда используйте `SeatedAt`.
