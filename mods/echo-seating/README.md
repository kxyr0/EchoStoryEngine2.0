# Echo Seating

Echo Seating adds authored seat zones for Forge 1.20.1.

The runtime does not scan furniture blocks and does not import furniture mods. It listens to server riding transitions, resolves the riding vehicle anchor, checks the anchor against authored `SeatZoneResource` definitions, matches optional named slots, and publishes typed seating lifecycle events.

Modules:

- `seating-contracts`: typed IDs, anchors, zones, occupancy and events.
- `seating-core`: EchoScript binding, validation, spatial index, matching and transition logic.
- `seating-forge-api-1.20.1`: public Forge lookup and lifecycle events.
- `seating-forge-1.20.1`: runtime observer, resource loading and commands.
- `seating-testmod-1.20.1`: manual acceptance helper.

Primary commands:

- `/echo_seating zones`
- `/echo_seating zone inspect <zone_id>`
- `/echo_seating player inspect <player>`
- `/echo_seating occupancy`
- `/echo_seating slot inspect <zone_id> <slot_id>`
- `/echo_seating diagnostics`
- `/echo_seating reload`
- `/echo_seating debug zone <zone_id>`
- `/echo_seating debug off`
