package io.github.kxyr0.echoplayerhud.forge.api;

import io.github.kxyr0.echoplayerhud.api.result.HudDiagnostic;
import java.util.List;

/**
 * Public Echo Player HUD service surface.
 */
public interface EchoPlayerHudService extends HudMetricProviderRegistrar {
    List<HudDiagnostic> diagnostics();
}
