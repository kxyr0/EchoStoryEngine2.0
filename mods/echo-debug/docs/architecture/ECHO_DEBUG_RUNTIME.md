# Echo Debug runtime

## Modules

`debug-core` — plain Java:

- shape cycle;
- server-side selection state machine;
- immutable selection snapshots;
- TriggerId/path planning;
- source rendering;
- render plan construction.

`debug-forge-1.20.1` — Forge adapter:

- `EchoDebugMod`;
- `TriggerWandItem`;
- key mappings;
- server interaction events;
- packets;
- creation screen;
- atomic writer;
- Echo Trigger authoring service bridge.

## Development-only usage

Debug tools доступны, когда установлен `echo-debug`; отдельный runtime enabled-флаг не используется.

Server повторно проверяет permission и wand-held для каждой операции.
