# Effects Project Map

| Module | Responsibility | Minecraft imports | Forge imports |
| --- | --- | ---: | ---: |
| `effects-contracts` | Typed IDs, definitions, curves, diagnostics, active views | 0 | 0 |
| `effects-core` | Validation, curve compilation, sampling, stacking, lifecycle, contributions | 0 | 0 |
| `effects-forge-api-1.20.1` | Stable service/query/provider contracts | 0 | 0 |
| `effects-script-adapter-1.20.1` | EchoScript parser facade and semantic binder | 0 | 0 |
| `effects-forge-1.20.1` | Forge entrypoint, server tick, commands, movement, client visuals | yes | yes |
| `effects-testmod-1.20.1` | Development-only scenarios | yes | yes |

Pipeline:

```text
EffectResource .es
-> EchoScript parser
-> EffectResource binder
-> EffectDefinition
-> EffectService
-> ActiveEffectInstance
-> channel contributions
-> server/client runtime
-> cleanup
```
