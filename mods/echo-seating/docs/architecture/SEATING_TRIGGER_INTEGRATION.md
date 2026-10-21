# Seating Trigger Integration

`echo-trigger` adds typed selectors:

- `Player.SeatedIn(zoneId)`
- `Player.SeatedAt(zoneId, slotId)`
- `Player.StoodUpIn(zoneId)`
- `Player.StoodUpFrom(zoneId, slotId)`
- `Player.ChangedSeatIn(zoneId)`

`echo-trigger-seating-adapter` listens to `PlayerSeatingLifecycleEvent`, matches indexed `TriggerStorySubscription`, applies a trigger event, starts/reset trigger runtime, and starts Story through `EchoStoryExecutionBridge`.
