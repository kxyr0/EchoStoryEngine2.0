# Metric providers

`HudMetricProviderRegistry` связывает `HudMetricId` с `HudMetricProvider`.

Provider возвращает:

```text
HudMetricSnapshot(current, maximum, normalized, revision, available)
```

Provider не возвращает `ServerPlayer`, mutable profile или renderer object.

Duplicate provider ID даёт diagnostic:

```text
PLAYER_HUD_METRIC_PROVIDER_CONFLICT
```

Если provider отсутствует, bar не рисуется, diagnostic bounded, остальные bars продолжают работать.

Адаптеры:

- `echo-player-hud-vitals-adapter` регистрирует `echo_player_hud:stamina`;
- `echo-player-hud-content-player-adapter` регистрирует `echo_player_hud:spark`.
