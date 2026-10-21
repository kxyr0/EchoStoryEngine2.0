# Seating Errors

Основные diagnostics:

- `SEATING_ZONE_DIMENSION_MISSING`: зона не указала exact dimension.
- `SEATING_ZONE_BOUNDS_INVALID`: неверный или отсутствующий `Area(Box(...))`.
- `SEATING_ZONE_OVERLAP_CONFLICT`: зоны пересекаются в одном dimension.
- `SEATING_SLOT_DUPLICATE`: один `SeatSlotId` объявлен повторно.
- `SEATING_SLOT_OUTSIDE_ZONE`: authored slot вне box.
- `SEATING_SLOT_MATCH_CONFLICT`: anchor одинаково подходит нескольким slots.
- `SEATING_UNKNOWN_SEAT_REJECTED`: policy запретила unnamed seat.
- `TRIGGER_SEATING_SELECTOR_INVALID`: TriggerResource написал неверный `Player.Seated*` selector.

Diagnostics должны быть queryable через `/echo_seating diagnostics`.
