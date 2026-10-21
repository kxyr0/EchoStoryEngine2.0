# Changelog

## 0.1.0

- Created the first `echo-trigger-player-sleep-adapter` repository because no existing adapter was present.
- Added Forge listener for `PlayerSleepLifecycleEvent.sleepEvent()`.
- Added exact `PlayerSleep.Confirmed(scenario)` subscription indexing against Echo Trigger catalog.
- Added Story start, sleep Story claim, 20 tick missing-claim timeout, conflict handling and context inheritance.
