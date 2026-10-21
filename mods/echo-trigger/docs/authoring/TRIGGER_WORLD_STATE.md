# Измерения, режим игры, время и погода

Predicates:

- `InDimension("minecraft:the_nether")`
- `GameModeIs(ADVENTURE)`
- `TimeBetween(13000, 23000)`
- `DayAtLeast(3)`
- `WeatherIs(RAIN)`

Events:

- `DimensionChanged()`, `DimensionEntered(id)`, `DimensionLeft(id)`
- `GameModeChanged(to: ADVENTURE)`
- `TimeReached(13000)`, `DayReached(3)`
- `WeatherChanged(to: RAIN)`

`TimeBetween` uses vanilla day time `0..23999` and supports midnight wrap. `TimeReached` handles normal ticks, `/time
set`, `/time add`, sleep skip and large jumps without duplicate fire in the same world tick.
