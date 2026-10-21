# Player Sleep Diagnostics

Diagnostics are stable enum values in `PlayerSleepFailure`.

Runtime stores recent diagnostics in `ForgePlayerSleepServices.diagnostics()` and exposes them through `/playersleep diagnostics`.

Important checks:

- scenario not found;
- already armed;
- vanilla confirmation timeout;
- stale transition revision;
- eyes not closed before dream entry;
- invalid dream dimension;
- safe wake position missing;
- recovery failure.

Diagnostics should name the source subject: player UUID, scenario id, command id, bed provider or dimension.
