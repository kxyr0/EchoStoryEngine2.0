package io.github.kxyr0.echoplayerhud.forge.api;

import io.github.kxyr0.echoplayerhud.api.metric.HudMetricProvider;

/**
 * Public registration boundary for HUD metric providers.
 */
public interface HudMetricProviderRegistrar {
    boolean register(HudMetricProvider provider);
}
