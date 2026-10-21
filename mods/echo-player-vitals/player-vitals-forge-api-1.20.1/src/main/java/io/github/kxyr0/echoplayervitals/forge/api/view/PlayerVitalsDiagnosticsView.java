package io.github.kxyr0.echoplayervitals.forge.api.view;

import io.github.kxyr0.echoplayervitals.api.result.PlayerVitalsDiagnostic;
import java.util.List;

/**
 * Queryable diagnostics boundary for Vitals.
 */
public interface PlayerVitalsDiagnosticsView {
    List<PlayerVitalsDiagnostic> diagnostics();
}
