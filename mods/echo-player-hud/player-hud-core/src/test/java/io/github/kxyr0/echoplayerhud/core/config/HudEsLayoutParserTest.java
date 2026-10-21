package io.github.kxyr0.echoplayerhud.core.config;

import io.github.kxyr0.echoplayerhud.api.layout.HudAnchor;
import io.github.kxyr0.echoplayerhud.api.layout.HudFillDirection;
import io.github.kxyr0.echoplayerhud.api.metric.HudMetricIds;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HudEsLayoutParserTest {
    private final HudEsLayoutParser parser = new HudEsLayoutParser();

    @Test
    void bindsPositionColorsAndVisibility() {
        var result = parser.parse("""
                PlayerHud(id: "jdh:hud", referenceWidth: 1920, referenceHeight: 1080,
                    scaleWithResolution: false);
                Bar(
                    id: "echo_player_hud:custom_bar",
                    source: "echo_content_player:spark",
                    enabled: false,
                    drawWhenUnavailable: false,
                    icon: "icons/custom.png",
                    iconSize: 12,
                    iconSpacing: -2,
                    anchor: "BOTTOM_RIGHT",
                    x: 44,
                    y: 88,
                    width: 220,
                    height: 14,
                    fill: "RIGHT_TO_LEFT",
                    background: "0x66000000",
                    borderWidth: 2,
                    borderColor: "#112233cc",
                    gradientSteps: 8,
                    iconOffsetX: -3,
                    iconOffsetY: 0.2,
                    gradientStart: "#3D8F6Aff",
                    gradientEnd: "#E0B84Cff"
                );
                """, "test.es", 7L);

        assertTrue(result.value().isPresent());
        var layout = result.value().orElseThrow();
        var bar = layout.bars().get(0);
        assertEquals(1920, layout.referenceWidth());
        assertEquals(1080, layout.referenceHeight());
        assertFalse(layout.scaleWithResolution());
        assertEquals(HudMetricIds.SPARK, bar.metricId());
        assertEquals(HudAnchor.BOTTOM_RIGHT, bar.anchor());
        assertEquals(HudFillDirection.RIGHT_TO_LEFT, bar.fillDirection());
        assertEquals(0x66000000, bar.backgroundColor());
        assertFalse(bar.enabled());
        assertFalse(bar.drawWhenUnavailable());
        assertEquals("icons/custom.png", bar.icon());
        assertEquals(12, bar.iconWidth());
        assertEquals(12, bar.iconHeight());
        assertEquals(-2, bar.iconSpacing());
        assertEquals(8, bar.gradientSteps());
        assertEquals(-3.0D, bar.iconOffsetX());
        assertEquals(0.2D, bar.iconOffsetY());
        assertEquals(2, bar.borderWidth());
        assertEquals(0xCC112233, bar.borderColor());
        assertEquals(0xFF3D8F6A, bar.gradient().steps().get(0).argb());
        assertEquals(0xFFE0B84C, bar.gradient().steps().get(1).argb());
        assertEquals(2, bar.gradient().steps().size());
    }

    @Test
    void separatesRawFatigueFromReserve() {
        var fatigue = parser.parse("""
                Bar(id: "echo_player_hud:fatigue_bar", source: "echo_player_vitals:fatigue",
                    width: 1, height: 1);
                """, "test.es", 1L).value().orElseThrow().bars().get(0);
        var reserve = parser.parse("""
                Bar(id: "echo_player_hud:reserve_bar", source: "echo_player_vitals:fatigue_reserve",
                    width: 1, height: 1);
                """, "test.es", 1L).value().orElseThrow().bars().get(0);

        assertEquals(HudMetricIds.FATIGUE, fatigue.metricId());
        assertEquals(HudMetricIds.STAMINA, reserve.metricId());
    }

    @Test
    void rejectsDuplicateBarIds() {
        var result = parser.parse("""
                Bar(id: "echo_player_hud:health_bar", source: "health", width: 1, height: 1);
                Bar(id: "echo_player_hud:health_bar", source: "spark", width: 1, height: 1);
                """, "test.es", 1L);

        assertTrue(result.value().isEmpty());
        assertTrue(result.diagnostics().stream().anyMatch(diagnostic -> diagnostic.error()));
    }
}
