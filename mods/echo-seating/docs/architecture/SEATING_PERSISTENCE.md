# Seating Persistence

SE1 keeps bounded event idempotency in `SeatEventLedger` and exposes stable event IDs derived from player, vehicle, anchor, event kind and revision.

Full SavedData-backed durable occupancy recovery is not expanded beyond the current in-memory registry in this pass. The architecture keeps `SeatOccupancy` and `SeatingRevision` explicit so restart recovery can be backed by persisted snapshots later without changing Trigger selectors.
