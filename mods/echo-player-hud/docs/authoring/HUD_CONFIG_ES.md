# HUD Config `.es`

Runtime config lives in the Forge game config directory:

```text
<gameDir>/config/echo_player_hud/player_vitals_hud.es
```

For the Prism instance `Форма Мысли`:

```text
C:/Users/kxyr0/AppData/Roaming/PrismLauncher/instances/Форма Мысли/minecraft/config/echo_player_hud/player_vitals_hud.es
```

The client hot-reloads `.es` after the file timestamp changes. If parsing fails, the last known good layout remains active.

Example:

```csharp
Bar(
    id: "echo_player_hud:spark_bar",
    source: "echo_content_player:spark",
    enabled: true,
    drawWhenUnavailable: true,
    icon: "echo_player_hud:icons/spark.png",
    iconWidth: 12,
    iconHeight: 12,
    iconSpacing: -6,
    iconOffsetX: 0,
    iconOffsetY: -1,
    anchor: "BOTTOM_CENTER",
    x: 50,
    y: 28,
    width: 80,
    height: 8,
    orientation: "HORIZONTAL",
    fill: "LEFT_TO_RIGHT",
    background: "0x99000000",
    borderWidth: 1,
    borderColor: "#c5850dff",
    gradientSteps: 10,
    gradientStart: "#dbd144ff",
    gradientEnd: "#ca880eff"
);
```

`iconSpacing` can be negative. Positive values leave a gap between the icon and bar, `0` touches the bar, and negative values move the icon into the bar.

Colors support `#RRGGBB`, `#RRGGBBAA`, `0xRRGGBB`, and `0xAARRGGBB`.

`icon` supports external shorthand paths like `echo_player_hud:icons/name.png`, `icons/name.png`, normal resource locations like `echo_player_hud:textures/gui/name.png`, and absolute PNG paths.
