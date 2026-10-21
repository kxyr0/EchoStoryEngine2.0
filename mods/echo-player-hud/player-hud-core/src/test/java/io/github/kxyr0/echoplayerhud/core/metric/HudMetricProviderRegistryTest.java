package io.github.kxyr0.echoplayerhud.core.metric;

import io.github.kxyr0.echoplayerhud.api.metric.HudMetricId;
import io.github.kxyr0.echoplayerhud.api.metric.HudMetricProvider;
import io.github.kxyr0.echoplayerhud.api.metric.HudMetricSnapshot;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HudMetricProviderRegistryTest {
    @Test
    void duplicateProviderIsRejectedAndDiagnosed() {
        HudMetricProviderRegistry registry = new HudMetricProviderRegistry();
        HudMetricProvider provider = provider("echo_player_hud:test");

        assertTrue(registry.register(provider));
        assertFalse(registry.register(provider));
        assertEquals(1, registry.diagnostics().size());
    }

    @Test
    void missingProviderDoesNotBreakOtherMetrics() {
        HudMetricProviderRegistry registry = new HudMetricProviderRegistry();
        registry.register(provider("echo_player_hud:health"));

        assertTrue(registry.snapshot(new HudMetricId("echo_player_hud:health")).isPresent());
        assertTrue(registry.snapshot(new HudMetricId("echo_player_hud:spark")).isEmpty());
    }

    private HudMetricProvider provider(String id) {
        HudMetricId metricId = new HudMetricId(id);
        return new HudMetricProvider() {
            @Override
            public HudMetricId metricId() {
                return metricId;
            }

            @Override
            public HudMetricSnapshot snapshot() {
                return HudMetricSnapshot.available(metricId, 1.0D, 1.0D, 1L);
            }
        };
    }
}
