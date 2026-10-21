# SeatZone Area

SE1 поддерживает только axis-aligned `Box(min, max)`.

`Dimension("minecraft:overworld")` обязателен. Одинаковые координаты в разных dimensions не сопоставляются.

Пример:

```csharp
Area(Box(Position(100, 64, 100), Position(104, 66, 104)));
```

Границы хранятся как `SeatZoneBox` с block positions. Runtime не сканирует блоки внутри box; box используется только для проверки уже resolved riding anchor.
