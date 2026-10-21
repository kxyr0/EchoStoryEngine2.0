# Echo Trigger Visibility Adapter

TV1 adapter for Forge 1.20.1. It converts accepted Echo Visibility lifecycle
events into exact indexed Echo Trigger subscriptions and starts Story through
the existing public bridges. It does not contain a second trigger runtime,
EchoScript parser, visibility engine, or Story engine.

Supported selectors:

- `Player.LostSightOf(watchId)`
- `Player.GainedSightOf(watchId)`
- `Player.TargetIsOutOfView(watchId)`

Claims are persisted by `VisibilityEventId`, while the Trigger owner includes
the visibility watch generation. NPC concealment is claimed before Story start,
committed after the public NPC body lookup reports the actor absent, and rejected
after ten server ticks if removal did not occur.

`/echo_visibility inspect <watch_id>` reports the armed state/generation,
matching indexed Trigger subscriptions, active leases and recent diagnostics.

The same adapter registers the Story action types produced by EchoScript
`View.*` sugar: `Arm`, `Disarm`, both recoverable waits, both boolean queries,
and the freshness-checked `AssertStillOutOfView`. Query results bind to
`visibility.isOutOfView` / `visibility.isVisible` by default.

The current player is inherited from `VisibilityStoryContextBridge`. An
explicit `player: "<uuid>"` named argument is also accepted for Story hosts
that start outside a visibility event. Visibility-derived execution IDs retain
the UUID, so pending waits can recover after a restart.
