namespace JDH.Configs.PlayerHud;

class PlayerVitalsHudConfig : PlayerHudConfigResource
{
    private void Build()
    {
        PlayerHud(
            id: "jdh:player_vitals_hud",
            referenceWidth: 1920,
            referenceHeight: 1080
        );

        Bar(
            id: "echo_player_hud:health_bar",
            source: "echo_player_vitals:health",
            enabled: true,
            drawWhenUnavailable: true,
            icon: "echo_player_hud:textures/gui/health.png",
            iconWidth: 16,
            iconHeight: 16,
            iconSpacing: 6,
            anchor: "BOTTOM_LEFT",
            x: 36,
            y: 126,
            width: 260,
            height: 18,
            orientation: "HORIZONTAL",
            fill: "LEFT_TO_RIGHT",
            background: "0x99000000",
            borderWidth: 1,
            borderColor: "#CC24151A",
            gradientStart: "#B23A48",
            gradientEnd: "#E85D75"
        );

        Bar(
            id: "echo_player_hud:spark_bar",
            source: "echo_content_player:spark",
            enabled: true,
            drawWhenUnavailable: true,
            icon: "echo_player_hud:textures/gui/spark.png",
            iconWidth: 14,
            iconHeight: 14,
            iconSpacing: 6,
            anchor: "BOTTOM_LEFT",
            x: 36,
            y: 104,
            width: 220,
            height: 14,
            orientation: "HORIZONTAL",
            fill: "LEFT_TO_RIGHT",
            background: "0x99000000",
            borderWidth: 1,
            borderColor: "#CC153143",
            gradientStart: "#2E8CCF",
            gradientEnd: "#57D6E6"
        );

    }
}
