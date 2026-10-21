package io.github.kxyr0.echoplayerhud.core.config;

import io.github.kxyr0.echoplayerhud.api.layout.HudAnchor;
import io.github.kxyr0.echoplayerhud.api.layout.HudBarDefinition;
import io.github.kxyr0.echoplayerhud.api.layout.HudBarId;
import io.github.kxyr0.echoplayerhud.api.layout.HudFillDirection;
import io.github.kxyr0.echoplayerhud.api.layout.HudOrientation;
import io.github.kxyr0.echoplayerhud.api.metric.HudMetricId;
import io.github.kxyr0.echoplayerhud.api.metric.HudMetricIds;
import io.github.kxyr0.echoplayerhud.api.result.HudDiagnostic;
import io.github.kxyr0.echoplayerhud.api.result.HudResult;
import io.github.kxyr0.echoplayerhud.core.diagnostic.HudDiagnosticCodes;
import io.github.kxyr0.echoplayerhud.core.layout.HudLayoutSnapshot;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Binds the author-facing HUD .es resource into immutable render layout.
 */
public final class HudEsLayoutParser {
    private final HudEsColorParser colors = new HudEsColorParser();

    public HudResult<HudLayoutSnapshot> parse(String text, String source, long revision) {
        List<HudDiagnostic> diagnostics = new ArrayList<>();
        HudEsFields hud = HudEsCall.find(text, "PlayerHud").stream().findFirst()
                .map(call -> HudEsFields.parse(call.body()))
                .orElseGet(() -> new HudEsFields(java.util.Map.of()));
        int referenceWidth = integer(hud, "referenceWidth", "reference_width", 1920);
        int referenceHeight = integer(hud, "referenceHeight", "reference_height", 1080);
        boolean scaleWithResolution = bool(hud.valueOr("true", "scaleWithResolution",
                "scale_with_resolution", "referenceScale", "scale"));
        List<HudBarDefinition> bars = bars(text, source, diagnostics);
        if (bars.isEmpty()) {
            diagnostics.add(HudDiagnostic.error(HudDiagnosticCodes.LAYOUT_INVALID,
                    "HUD config must define at least one Bar(...).", source));
        }
        if (diagnostics.stream().anyMatch(HudDiagnostic::error)) {
            return HudResult.invalid(diagnostics);
        }
        return HudResult.valid(new HudLayoutSnapshot(bars, revision, referenceWidth, referenceHeight,
                scaleWithResolution), diagnostics);
    }

    private List<HudBarDefinition> bars(String text, String source, List<HudDiagnostic> diagnostics) {
        List<HudBarDefinition> bars = new ArrayList<>();
        Set<String> ids = new HashSet<>();
        for (HudEsCall call : HudEsCall.find(text, "Bar")) {
            HudEsFields fields = HudEsFields.parse(call.body());
            fields.value("id", "bar").ifPresentOrElse(id -> bindBar(fields, source, diagnostics, bars, ids, id),
                    () -> diagnostics.add(HudDiagnostic.error(HudDiagnosticCodes.CONFIG_BIND_ERROR,
                            "Bar is missing id.", source)));
        }
        return bars;
    }

    private void bindBar(HudEsFields fields, String source, List<HudDiagnostic> diagnostics,
                         List<HudBarDefinition> bars, Set<String> ids, String id) {
        if (!ids.add(id)) {
            diagnostics.add(HudDiagnostic.error(HudDiagnosticCodes.ELEMENT_DUPLICATE,
                    "Duplicate HUD bar id: " + id, source));
            return;
        }
        try {
            bars.add(new HudBarDefinition(new HudBarId(id), metric(fields), anchor(fields),
                    orientation(fields), fill(fields), integer(fields, "x", "offsetX", "offset_x", 0),
                    integer(fields, "y", "offsetY", "offset_y", 0), requiredInteger(fields, "width"),
                    requiredInteger(fields, "height"), colors.gradient(fields),
                    integer(fields, "gradientSteps", "gradient_steps", "gradientStepCount", 1),
                    colors.color(fields.valueOr("#99000000", "background", "backgroundColor")),
                    bool(fields.valueOr("true", "enabled", "visible")),
                    bool(fields.valueOr("true", "drawWhenUnavailable", "draw_when_unavailable")),
                    fields.valueOr("", "icon", "iconPng", "icon_png"),
                    integer(fields, "iconWidth", "icon_width", "iconSize", "icon_size",
                            requiredInteger(fields, "height")),
                    integer(fields, "iconHeight", "icon_height", "iconSize", "icon_size",
                            requiredInteger(fields, "height")),
                    integer(fields, "iconSpacing", "icon_spacing", "spacing", 4),
                    decimal(fields, "iconOffsetX", "icon_offset_x", "iconX", "icon_x", 0.0D),
                    decimal(fields, "iconOffsetY", "icon_offset_y", "iconY", "icon_y", 0.0D),
                    integer(fields, "borderWidth", "border_width", "border", 0),
                    colors.color(fields.valueOr("transparent", "borderColor", "border_color"))));
        } catch (IllegalArgumentException exception) {
            diagnostics.add(HudDiagnostic.error(HudDiagnosticCodes.CONFIG_BIND_ERROR,
                    "Invalid Bar " + id + ": " + exception.getMessage(), source));
        }
    }

    private HudMetricId metric(HudEsFields fields) {
        String value = fields.valueOr("", "metric", "source", "metricId", "metric_id");
        if (value.isBlank()) {
            throw new IllegalArgumentException("metric/source is required");
        }
        return switch (value.toLowerCase(Locale.ROOT)) {
            case "echo_player_vitals:health", "health" -> HudMetricIds.HEALTH;
            case "echo_content_player:spark", "spark" -> HudMetricIds.SPARK;
            case "echo_player_vitals:fatigue", "fatigue" -> HudMetricIds.FATIGUE;
            case "echo_player_vitals:fatigue_reserve", "echo_player_vitals:stamina",
                    "fatigue_reserve", "reserve", "stamina" -> HudMetricIds.STAMINA;
            default -> new HudMetricId(value);
        };
    }

    private HudAnchor anchor(HudEsFields fields) {
        return enumValue(HudAnchor.class, fields.valueOr("BOTTOM_LEFT", "anchor"));
    }

    private HudOrientation orientation(HudEsFields fields) {
        return enumValue(HudOrientation.class, fields.valueOr("HORIZONTAL", "orientation"));
    }

    private HudFillDirection fill(HudEsFields fields) {
        return enumValue(HudFillDirection.class, fields.valueOr("LEFT_TO_RIGHT", "fill", "fillDirection"));
    }

    private <T extends Enum<T>> T enumValue(Class<T> type, String value) {
        return Enum.valueOf(type, value.strip().toUpperCase(Locale.ROOT));
    }

    private boolean bool(String value) {
        return Boolean.parseBoolean(value.strip());
    }

    private int requiredInteger(HudEsFields fields, String key) {
        return Integer.parseInt(fields.value(key).orElseThrow(() ->
                new IllegalArgumentException(key + " is required")));
    }

    private int integer(HudEsFields fields, String firstKey, String secondKey, int fallback) {
        return Integer.parseInt(fields.value(firstKey, secondKey).orElse(Integer.toString(fallback)));
    }

    private int integer(HudEsFields fields, String firstKey, String secondKey, String thirdKey, int fallback) {
        return Integer.parseInt(fields.value(firstKey, secondKey, thirdKey).orElse(Integer.toString(fallback)));
    }

    private int integer(HudEsFields fields, String firstKey, String secondKey, String thirdKey,
                        String fourthKey, int fallback) {
        return Integer.parseInt(fields.value(firstKey, secondKey, thirdKey, fourthKey)
                .orElse(Integer.toString(fallback)));
    }

    private double decimal(HudEsFields fields, String firstKey, String secondKey, String thirdKey,
                           String fourthKey, double fallback) {
        return Double.parseDouble(fields.value(firstKey, secondKey, thirdKey, fourthKey)
                .orElse(Double.toString(fallback)));
    }
}
