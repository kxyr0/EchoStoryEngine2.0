package io.github.kxyr0.echoplayerhud.api.metric;

/**
 * Standard Echo Player HUD metric identifiers.
 */
public final class HudMetricIds {
    public static final HudMetricId HEALTH = new HudMetricId("echo_player_hud:health");
    public static final HudMetricId SPARK = new HudMetricId("echo_player_hud:spark");
    public static final HudMetricId FATIGUE = new HudMetricId("echo_player_hud:fatigue");
    public static final HudMetricId STAMINA = new HudMetricId("echo_player_hud:stamina");

    private HudMetricIds() {
    }
}
