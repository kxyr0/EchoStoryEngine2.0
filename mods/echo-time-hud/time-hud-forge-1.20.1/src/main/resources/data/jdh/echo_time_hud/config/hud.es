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

