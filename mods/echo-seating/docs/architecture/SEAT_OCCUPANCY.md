# Seat Occupancy

`SeatOccupancyRegistry` tracks one active occupancy per `PlayerRef`.

Exact named slot conflicts produce `SEATING_SLOT_OCCUPANCY_CONFLICT`. The registry does not silently choose the first player.

Occupancy is read-only through `SeatOccupancyView`.
