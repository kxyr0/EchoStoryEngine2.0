# Echo Trigger

Reactive geometry and vanilla gameplay trigger provider pack for Echo Platform.

Version `0.3.0` keeps the T1/T2 sequence contract and adds reactive geometry callbacks for enter, exit, and stay.
Version `0.2.0` added predicate/event expressions for inventory, hands, equipment, player state, effects, progression,
blocks, dimensions, time, and weather.

Public contracts live in `trigger-contracts`; runtime binding and state are plain Java in `trigger-core`.
Forge-specific hosting is isolated in `trigger-forge-1.20.1`, and development tools belong to
`trigger-testmod-1.20.1`.

Start with `docs/authoring/TRIGGER_REACTIVE_GEOMETRY.md`,
`docs/authoring/TRIGGER_VANILLA_REFERENCE.md`, and `docs/architecture/TRIGGER_PROJECT_MAP.md`.

[История изменений](docs/changelogs/README.md)
