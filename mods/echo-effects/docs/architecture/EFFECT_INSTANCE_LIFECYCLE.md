# Effect Instance Lifecycle

An active effect keeps a separate `EffectInstanceId`; it is never keyed only by `EffectId`.

Statuses:

```text
CREATED -> APPLYING -> ACTIVE -> FADING_OUT -> COMPLETED
```

Removal can transition to `REMOVED` or `CANCELLED`. Terminal transitions clear server contributions and client visual state.
