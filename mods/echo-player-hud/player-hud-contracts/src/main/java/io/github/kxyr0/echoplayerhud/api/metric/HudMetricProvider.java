package io.github.kxyr0.echoplayerhud.api.metric;

/**
 * Snapshot provider for one HUD metric ID.
 */
public interface HudMetricProvider {
    HudMetricId metricId();

    HudMetricSnapshot snapshot();
}
