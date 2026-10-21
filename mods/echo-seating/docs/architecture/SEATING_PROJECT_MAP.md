# Seating Project Map

Runtime path:

`EntityMountEvent`
→ `PlayerRidingObserver`
→ `ForgeSeatAnchorResolver`
→ `SeatZoneCatalog`
→ `SeatZoneSpatialIndex`
→ `SeatSlotMatcher`
→ `SeatOccupancyRegistry`
→ `SeatEventLedger`
→ `PlayerSeatingLifecycleEvent`
→ `SeatingTriggerEventProvider`
→ `TriggerEventSelector`
→ `TriggerRuntimeHost`
→ Story execution context.

Repositories:

- `mods/echo-seating`
- `mods/echo-trigger-seating-adapter`
- touched: `mods/echo-trigger`
