package io.github.kxyr0.echoplayerhud.core.layout;

import io.github.kxyr0.echoplayerhud.api.gradient.HudColorStep;
import io.github.kxyr0.echoplayerhud.api.gradient.HudGradient;
import io.github.kxyr0.echoplayerhud.api.layout.HudAnchor;
import io.github.kxyr0.echoplayerhud.api.layout.HudBarDefinition;
import io.github.kxyr0.echoplayerhud.api.layout.HudBarId;
import io.github.kxyr0.echoplayerhud.api.layout.HudFillDirection;
import io.github.kxyr0.echoplayerhud.api.layout.HudOrientation;
import io.github.kxyr0.echoplayerhud.api.metric.HudMetricId;
import io.github.kxyr0.echoplayerhud.api.metric.HudMetricIds;
import java.util.List;

/**
 * Default authoring-free layout used before external .es config is loaded.
 */
public final class HudDefaultLayout {
    public HudLayoutSnapshot snapshot() {
        return new HudLayoutSnapshot(List.of(
                bar("echo_player_hud:health_bar", HudMetricIds.HEALTH, 36, 126, 260, 18,
                        gradient(0xFFB23A48, 0xFFE85D75)),
                bar("echo_player_hud:spark_bar", HudMetricIds.SPARK, 36, 104, 220, 14,
                        gradient(0xFF2E8CCF, 0xFF57D6E6))
        ), 1L);
    }

    private HudBarDefinition bar(String id, HudMetricId metricId, int x, int y, int width, int height,
                                 HudGradient gradient) {
        return new HudBarDefinition(new HudBarId(id), metricId, HudAnchor.BOTTOM_LEFT, HudOrientation.HORIZONTAL,
                HudFillDirection.LEFT_TO_RIGHT, x, y, width, height, gradient);
    }

    private HudGradient gradient(int low, int high) {
        return new HudGradient(List.of(new HudColorStep(0.0D, low), new HudColorStep(0.66D, high)));
    }
}
