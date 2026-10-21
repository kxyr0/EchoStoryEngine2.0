package io.github.kxyr0.echoplayerhud.forge.service;

import com.mojang.logging.LogUtils;
import io.github.kxyr0.echoplayerhud.api.metric.HudMetricId;
import io.github.kxyr0.echoplayerhud.api.metric.HudMetricProvider;
import io.github.kxyr0.echoplayerhud.api.metric.HudMetricSnapshot;
import io.github.kxyr0.echoplayerhud.api.result.HudDiagnostic;
import io.github.kxyr0.echoplayerhud.api.result.HudResult;
import io.github.kxyr0.echoplayerhud.core.diagnostic.HudDiagnosticCodes;
import io.github.kxyr0.echoplayerhud.core.layout.HudDefaultLayout;
import io.github.kxyr0.echoplayerhud.core.layout.HudLayoutPublisher;
import io.github.kxyr0.echoplayerhud.core.layout.HudLayoutSnapshot;
import io.github.kxyr0.echoplayerhud.core.metric.HudMetricProviderRegistry;
import io.github.kxyr0.echoplayerhud.forge.api.EchoPlayerHudService;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;

/**
 * Forge-facing HUD service backed by the core registry and layout publisher.
 */
public final class ForgePlayerHudServices implements EchoPlayerHudService {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final HudMetricProviderRegistry registry = new HudMetricProviderRegistry();
    private final HudLayoutPublisher layouts = new HudLayoutPublisher(new HudDefaultLayout().snapshot());
    private List<HudDiagnostic> layoutDiagnostics = List.of();
    private String layoutSource = "built-in default";

    @Override
    public boolean register(HudMetricProvider provider) {
        boolean registered = registry.register(provider);
        LOGGER.info("Echo Player HUD provider registration metric={} provider={} registered={}",
                provider.metricId(), provider.getClass().getName(), registered);
        return registered;
    }

    @Override
    public synchronized List<HudDiagnostic> diagnostics() {
        List<HudDiagnostic> diagnostics = new ArrayList<>(layoutDiagnostics);
        diagnostics.addAll(registry.diagnostics());
        return List.copyOf(diagnostics);
    }

    public Optional<HudMetricSnapshot> snapshot(HudMetricId metricId) {
        return registry.snapshot(metricId);
    }

    public List<HudMetricProvider> providers() {
        return registry.providers();
    }

    public HudLayoutSnapshot layout() {
        return layouts.current();
    }

    public synchronized boolean reloadLayout(HudResult<HudLayoutSnapshot> result, String source) {
        layoutSource = source;
        layoutDiagnostics = result.value().isPresent() ? result.diagnostics() : retainedDiagnostics(result);
        result.value().ifPresent(layouts::publish);
        return result.value().isPresent();
    }

    public synchronized String layoutSource() {
        return layoutSource;
    }

    public synchronized List<HudDiagnostic> layoutDiagnostics() {
        return layoutDiagnostics;
    }

    private List<HudDiagnostic> retainedDiagnostics(HudResult<HudLayoutSnapshot> result) {
        List<HudDiagnostic> diagnostics = new ArrayList<>(result.diagnostics());
        diagnostics.add(HudDiagnostic.warning(HudDiagnosticCodes.LAST_KNOWN_GOOD,
                "Broken HUD config was rejected; current last-known-good layout is still active.", layoutSource));
        return List.copyOf(diagnostics);
    }
}
