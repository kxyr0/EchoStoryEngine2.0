# Player Sleep Runtime

Runtime разделён на модули:

- `player-sleep-contracts`: immutable neutral contracts;
- `player-sleep-core`: arm registry, scenario catalog, state machine, session factory, transition timeline;
- `player-sleep-forge-api-1.20.1`: public service bridge;
- `player-sleep-forge-1.20.1`: Forge hooks, bed provider, overlay, networking;
- `player-sleep-script-adapter-1.20.1`: Story external actions.

Core не импортирует Minecraft или Forge. Forge runtime владеет только loader-specific деталями.
