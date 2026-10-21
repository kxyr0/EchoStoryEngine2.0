# SeatZone Slots

Именованное место задается явным `SeatSlotId` и authored anchor.

```csharp
private SeatSlotId MothersSeat = "jdh:home/breakfast_table/seat_3";

private void Init()
{
    Slot(MothersSeat, Position(12, 65, 12));
    MatchRadius(0.85);
}
```

`seat_3` нельзя получать из порядка обхода, BlockPos hash, UUID vehicle entity или Java array index. Если anchor подходит к двум slots на одинаковом расстоянии, runtime пишет `SEATING_SLOT_MATCH_CONFLICT` и не публикует slot-specific event.
