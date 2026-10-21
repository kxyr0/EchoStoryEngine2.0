# Player HUD Quickstart

Echo Player HUD draws the default player bars:

- `echo_player_hud:health` - vanilla health snapshot on the client.
- `echo_player_hud:spark` - provided by the Content Player adapter.

The HUD does not modify health, Spark, fatigue, or hunger. If a provider is missing, a bar can draw only its background when `drawWhenUnavailable: true`.

Runtime config:

```text
<gameDir>/config/echo_player_hud/player_vitals_hud.es
```

The client hot-reloads `.es` after the file timestamp changes.

Commands:

- `/echo_hud reload`
- `/echo_hud validate`
- `/echo_hud inspect`
- `/echo_hud metrics`
- `/echo_hud diagnostics`
- `/echo_hud bounds`
