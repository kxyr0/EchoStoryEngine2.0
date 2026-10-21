package io.github.kxyr0.echoplayerhud.core.metric;

import io.github.kxyr0.echoplayerhud.api.metric.HudMetricId;
import io.github.kxyr0.echoplayerhud.api.metric.HudMetricProvider;
import io.github.kxyr0.echoplayerhud.api.metric.HudMetricSnapshot;
import io.github.kxyr0.echoplayerhud.api.result.HudDiagnostic;
import io.github.kxyr0.echoplayerhud.core.diagnostic.HudDiagnosticCodes;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Deterministic registry for immutable HUD metric providers.
 */
public final class HudMetricProviderRegistry {
    private final Map<HudMetricId, HudMetricProvider> providers = new LinkedHashMap<>();
    private final Map<HudMetricId, HudDiagnostic> diagnostics = new LinkedHashMap<>();

    public synchronized boolean register(HudMetricProvider provider) {
        Objects.requireNonNull(provider, "provider");
        HudMetricId metricId = provider.metricId();
        if (providers.containsKey(metricId)) {
            diagnostics.put(metricId, HudDiagnostic.error(HudDiagnosticCodes.PROVIDER_CONFLICT,
                    "HUD metric provider is already registered: " + metricId, metricId.value()));
            return false;
        }
        providers.put(metricId, provider);
        return true;
    }

    public synchronized Optional<HudMetricSnapshot> snapshot(HudMetricId metricId) {
        HudMetricProvider provider = providers.get(metricId);
        if (provider == null) {
            diagnostics.put(metricId, HudDiagnostic.warning(HudDiagnosticCodes.PROVIDER_NOT_FOUND,
                    "HUD metric provider is not registered: " + metricId, metricId.value()));
            return Optional.empty();
        }
        return Optional.of(provider.snapshot());
    }

    public synchronized List<HudMetricProvider> providers() {
        return List.copyOf(providers.values());
    }

    public synchronized List<HudDiagnostic> diagnostics() {
        return List.copyOf(diagnostics.values());
    }
}
