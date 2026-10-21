# HUD Bars

`Bar(...)` links:

- `HudBarId`
- `HudMetricId`
- anchor
- authored offset
- authored size
- orientation
- fill direction
- gradient

The renderer requests `HudMetricSnapshot` from the registry. If `available=false`, the bar is not filled.

Default metric IDs:

- `echo_player_hud:health`
- `echo_player_hud:spark`
