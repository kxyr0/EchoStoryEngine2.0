# Seating Hot Reload

`/echo_seating reload` recompiles discovered `SeatZoneResource` files.

Successful compile swaps the catalog snapshot. Failed compile keeps the previous runtime catalog and exposes diagnostics. Existing occupancy remains tied to its current zone revision until a future revalidation pass updates it.
