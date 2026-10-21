# SeatZoneResource

`SeatZoneResource` описывает authored зону посадки в `.es` без `Build`, `Nodes`, callbacks и ручных `return`.

Файлы загружаются из:

`<worldRoot>/story/content-pack/<namespace>/echo_seating/zones/**/*.es`

Минимальная форма:

```csharp
class BreakfastTableSeats : SeatZoneResource
{
    private SeatZoneId BreakfastTable = "jdh:home/breakfast_table";

    private void Init()
    {
        Id(BreakfastTable);
        Dimension("minecraft:overworld");
        Area(Box(Position(10, 64, 10), Position(14, 66, 14)));
        Detect(RIDING_ANCHOR);
        UnknownSeats(ALLOW);
        MatchRadius(0.85);
    }
}
```

Binder: `SeatZoneDefinitionBinder`. Runtime catalog: `SeatZoneCatalog`.
