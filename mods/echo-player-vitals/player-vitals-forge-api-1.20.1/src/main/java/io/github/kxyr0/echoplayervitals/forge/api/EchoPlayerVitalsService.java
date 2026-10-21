package io.github.kxyr0.echoplayervitals.forge.api;

import io.github.kxyr0.echoplayervitals.forge.api.mutation.PlayerFatigueMutationService;
import io.github.kxyr0.echoplayervitals.forge.api.query.PlayerVitalsQueryService;
import io.github.kxyr0.echoplayervitals.forge.api.view.PlayerVitalsDiagnosticsView;

/**
 * Server-scoped public Echo Player Vitals service surface.
 */
public interface EchoPlayerVitalsService extends PlayerVitalsQueryService, PlayerFatigueMutationService,
        PlayerVitalsDiagnosticsView {
}
