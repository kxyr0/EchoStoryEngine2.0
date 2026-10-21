# Player Seating Lifecycle

`PlayerRidingObserver` listens to `EntityMountEvent` and confirms the result on the following server tick.

Transitions:

- no occupancy → resolved seat: `SEATED`
- occupancy → no vehicle: `STOOD_UP`
- occupancy A → resolved seat B: `CHANGED_SEAT`

Continuous riding does not create another `SEATED` event.
