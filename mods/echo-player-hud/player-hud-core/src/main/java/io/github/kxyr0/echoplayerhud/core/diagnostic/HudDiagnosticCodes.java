package io.github.kxyr0.echoplayerhud.core.diagnostic;

/**
 * Stable HUD diagnostic codes.
 */
public final class HudDiagnosticCodes {
    public static final String CONFIG_PARSE_ERROR = "PLAYER_HUD_CONFIG_PARSE_ERROR";
    public static final String CONFIG_BIND_ERROR = "PLAYER_HUD_CONFIG_BIND_ERROR";
    public static final String LAYOUT_INVALID = "PLAYER_HUD_LAYOUT_INVALID";
    public static final String ELEMENT_DUPLICATE = "PLAYER_HUD_ELEMENT_DUPLICATE";
    public static final String PROVIDER_NOT_FOUND = "PLAYER_HUD_METRIC_PROVIDER_NOT_FOUND";
    public static final String PROVIDER_CONFLICT = "PLAYER_HUD_METRIC_PROVIDER_CONFLICT";
    public static final String METRIC_INVALID = "PLAYER_HUD_METRIC_INVALID";
    public static final String GRADIENT_INVALID = "PLAYER_HUD_GRADIENT_INVALID";
    public static final String ANCHOR_INVALID = "PLAYER_HUD_ANCHOR_INVALID";
    public static final String FRAMEBUFFER_INVALID = "PLAYER_HUD_FRAMEBUFFER_INVALID";
    public static final String LAST_KNOWN_GOOD = "PLAYER_HUD_LAST_KNOWN_GOOD_RETAINED";
    public static final String RENDER_FAILED = "PLAYER_HUD_RENDER_FAILED";

    private HudDiagnosticCodes() {
    }
}
