# Clock and daylight

`EchoClock` is the authority. Its canonical value is `EchoInstant.absoluteMicroseconds`; `EchoDay` and `TimeOfDay` are derived views. It advances only from Forge server ticks. Loading a world restores `EchoTimeState`; elapsed wall time while the server was stopped is never consulted.

`TimeRate.compile(gameMinutes, realSeconds)` reduces a rational number of game microseconds per simulation tick. `TimeRateAccumulator` carries the integer remainder across ticks and rescales it when the rate changes, so fractional rates do not accumulate floating-point drift. Each actual clock change advances `TimeRevision`.

The live speed resource is `<gameDir>/story/configs/jdh/echo_time/time-speed.es`. `InitialDay` and `InitialTime` initialize only newly-created SavedData. A reload changes rate/running/daylight configuration and never resets an existing instant.

`DaylightOwnership` disables `doDaylightCycle` while `SyncSun(true)` is owned and restores the previous gamerule on orderly release. Every server tick it derives vanilla `dayTime` from the Echo instant:

```text
derivedDayTime = (Day - 1) * 24000
               + 18000
               + floor(timeOfDayMicroseconds * 24000 / microsecondsPerDay)
```

This value is monotonic across Echo day boundaries. Configured `SunDimension` levels are corrected to the derived value, so external `/time set` is diagnosed and overwritten rather than becoming authority. No client clock or vanilla `dayTime` feeds back into `EchoClock`.

Snapshots contain the instant, rate, running/paused flags, server tick, time revision, and config revision. They are sent on login, mutations/config changes, and every 100 server ticks—not every tick.

