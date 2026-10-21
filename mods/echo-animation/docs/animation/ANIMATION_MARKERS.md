# Server animation markers

High-level `Animation(...)` resources may declare generic lifecycle markers:

```echoscript
markers: [
    Marker("example:take", phase: "MAIN", tick: 6)
]
```

`AnimationMarkerId` is a neutral namespaced ID. A marker belongs to `ENTER`,
`MAIN` or `EXIT` and uses a positive phase-local logical tick. Marker IDs must
be unique inside one animation definition.

On the authoritative server tick, Echo Animation posts `AnimationMarkerEvent`
with subject, animation, playback, owner, marker, phase/time, server tick and
playback revision. `authority()` is always `SERVER`. Delivery receipts are
stored in the active playback record and its SavedData representation, so a
marker is not delivered twice after a normal save/reload cycle.

Consumers must still use `(playbackId, markerId, playbackRevision)` as an
idempotency key. Markers carry no feature-specific map payload; integrations
interpret semantic IDs in their own adapter repositories.
