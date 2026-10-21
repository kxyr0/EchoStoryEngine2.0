package io.github.kxyr0.echoplayerhud.api.metric;

import java.util.Objects;

/**
 * Immutable value returned by a HUD metric provider.
 */
public record HudMetricSnapshot(
        HudMetricId metricId,
        double current,
        double maximum,
        double normalized,
        HudMetricRevision revision,
        boolean available
) {
    public HudMetricSnapshot {
        metricId = Objects.requireNonNull(metricId, "metricId");
        revision = Objects.requireNonNull(revision, "revision");
        if (!Double.isFinite(current) || !Double.isFinite(maximum) || !Double.isFinite(normalized)) {
            throw new IllegalArgumentException("HUD metric values must be finite.");
        }
        if (available && (maximum <= 0.0D || current < 0.0D || current > maximum)) {
            throw new IllegalArgumentException("Available HUD metric current must be in 0..maximum.");
        }
        if (normalized < 0.0D || normalized > 1.0D) {
            throw new IllegalArgumentException("HUD metric normalized value must be in 0..1.");
        }
    }

    public static HudMetricSnapshot available(HudMetricId metricId, double current, double maximum, long revision) {
        double normalized = maximum <= 0.0D ? 0.0D : current / maximum;
        return new HudMetricSnapshot(metricId, current, maximum, clamp(normalized), new HudMetricRevision(revision),
                true);
    }

    public static HudMetricSnapshot unavailable(HudMetricId metricId) {
        return new HudMetricSnapshot(metricId, 0.0D, 1.0D, 0.0D, HudMetricRevision.unavailable(), false);
    }

    private static double clamp(double value) {
        return Math.max(0.0D, Math.min(1.0D, value));
    }
}
