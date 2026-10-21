# Effect Channels

Реальные channel IDs находятся в `EffectChannelId`:

- `movement.speed_multiplier`
- `camera.fov_offset`
- `camera.roll`
- `camera.shake`
- `screen.tint`
- `screen.vignette`
- `sound.play_at`
- `vanilla.mob_effect.apply`

Каналы компилируются как typed tracks/events, не как `Map<String, Object>`.
