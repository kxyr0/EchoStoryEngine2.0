# Time HUD config

Live path: `<gameDir>/story/configs/jdh/echo_time_hud/hud.es`. The packaged default is copied only when that file is missing. A WatchService marks edits, client ticks provide debounce, and `HudConfigBinder` parses only after a change. Invalid reloads retain the last-known-good `HudConfig` and expose diagnostics through `/echo_time_hud status`.

```echoscript
namespace JDH.Config.TimeHud;

class MainTimeHud : TimeHudConfigResource {
    private void Init() {
        Id("jdh:main_time_hud");
        Enabled(true);
        Anchor(TOP_RIGHT);
        Offset(x: -24, y: 24);
        ReferenceCanvas(width: 1920, height: 1080);
        SafeMargin(12);
        TextScale(1.0);
        Format("HH:mm");
        ShowDay(false);
        Shadow(true);
        Background(false);
        Opacity(1.0);
        RespectHideHud(true);
        ClampToScreen(true);
        ScreenPolicy(SHOW_DURING_GAME_SCREENS);
    }
}
```

The resource must use namespace `JDH.Config.TimeHud`, extend `TimeHudConfigResource`, and contain only `private void Init()`. `Format("HH:mm")` is the only accepted format; any other value reports `HUD_CONFIG_FORMAT_INVALID` and retains LKG. `ShowDay(false)` renders `09:50`; `ShowDay(true)` renders `Day 2 · 09:50`. Opacity is `0.0..1.0`.

Client commands:

- `/echo_time_hud status` reports active config revision/path, snapshot revision/time, and diagnostics.
- `/echo_time_hud reload` forces a parse and applies it only on success.
- `/echo_time_hud debug bounds on|off` toggles physical reference/safe/text rectangles. Running `/echo_time_hud debug bounds` without `on|off` prints framebuffer, reference canvas, uniform scale, safe rect, anchor, measured text bounds, final physical rect, transformed GUI position, and GUI pixel ratios.

