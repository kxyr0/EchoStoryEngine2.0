# Echo Effects

Echo Effects is a Forge 1.20.1 mod for declarative, EchoScript-defined player effects.

The EF1 implementation is split into independent modules:

- `effects-contracts`: neutral immutable IDs, definitions, curves, statuses, diagnostics, and active views.
- `effects-core`: plain Java validation, curve compilation, stacking, lifecycle, timeline dispatch, and contributions.
- `effects-forge-api-1.20.1`: public service and query boundaries for other Echo hosts.
- `effects-forge-1.20.1`: Forge runtime host, commands, movement contribution, client visual state, and content reload bridge.
- `effects-script-adapter-1.20.1`: semantic binder for `EffectResource` `.es` files and `Effect.*` command payloads.
- `effects-testmod-1.20.1`: development-only sample scenarios and acceptance resources.

User effects live in content packs under:

```text
<gameDir>/story/content-pack/<namespace>/echo_effects/effects/
```

Dialogue, Story, and Cutscene hosts should call the generic public API or emit the generic
`Effect.ApplyToPlayer`, `Effect.RemoveFromPlayer`, `Effect.WaitOnPlayer`, and predicate command payloads.
The Java runtime never requires one Java class per story effect.
