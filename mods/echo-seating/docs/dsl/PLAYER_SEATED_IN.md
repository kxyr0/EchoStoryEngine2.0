# Player.SeatedIn

Trigger selector:

```csharp
Subscribe(Player.SeatedIn(BreakfastTable));
```

Срабатывает для любого publishable seating event внутри `SeatZoneId`. Если seat unnamed, selector все равно подходит. Для named seat этот selector также подходит, поэтому generic breakfast story и точный seat trigger могут запуститься на одном event.
