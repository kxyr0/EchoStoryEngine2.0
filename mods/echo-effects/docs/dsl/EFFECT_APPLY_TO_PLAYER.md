# Effect.ApplyToPlayer

`Effect.ApplyToPlayer(effectId)` starts an effect on the current player from the current EchoScript execution context.

The command is non-blocking after the server accepts the effect instance. The current player must come from the host execution context; the implementation must not pick the first, nearest, random, or last connected player.

Supported payload fields:

- `effectId`: typed `EffectId`.
- `intensity`: optional finite positive scalar, default `1.0`.
- `commandId`: deterministic `EffectCommandId` supplied by the host or derived from execution lineage.
