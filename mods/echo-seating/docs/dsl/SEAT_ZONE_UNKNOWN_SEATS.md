# UnknownSeats

`UnknownSeats(ALLOW)` разрешает `Player.SeatedIn(zone)` для riding anchor внутри зоны, даже если anchor не совпал с authored slot.

`UnknownSeats(REJECT)` требует совпадения с authored slot.

Unknown seat event содержит zone, player, vehicle и anchor, но не содержит `SeatSlotId`. Runtime не придумывает `seat_5`.
