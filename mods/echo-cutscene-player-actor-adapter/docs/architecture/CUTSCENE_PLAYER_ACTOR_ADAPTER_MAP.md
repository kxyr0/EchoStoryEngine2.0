# Cutscene Player Actor Adapter map

## Responsibilities

- listen to `CutsceneLifecycleEvent`;
- acquire `PlayerControlLease` on playback accepted;
- release lease on completion/stop/failure;
- emergency release on logout.

## Dependencies

Allowed:

- `cutscene-contracts`;
- `cutscene-forge-api-1.20.1`;
- `player-actor-contracts`;
- `player-actor-forge-api-1.20.1`.

Forbidden:

- camera control;
- cutscene core imports;
- Player Actor runtime imports.
