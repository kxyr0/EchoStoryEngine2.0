# Граница будущего Echo Wardrobe

Echo Content Player не зависит от Echo Wardrobe и не хранит одежду.

Будущий поток:

```text
echo-wardrobe -> player-forge-api -> EchoPlayerId -> PlayerAppearanceSnapshot
```

Public boundary предоставляет:

- `EchoPlayerId`;
- immutable base appearance snapshot;
- skin model;
- appearance revision;
- client extension registration point.

Boundary не предоставляет:

- internal repository;
- mutable profile;
- SavedData;
- renderer internals;
- persistence internals.

Поля `hat`, `shirt`, `pants`, `shoes`, `outfit` не входят в P1.
