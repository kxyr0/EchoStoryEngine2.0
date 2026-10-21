package io.github.kxyr0.echonavigation.core.settings;

import io.github.kxyr0.echonavigation.api.backend.NavigationSettingsSnapshot;
import io.github.kxyr0.echonavigation.api.diagnostic.NavigationDiagnostic;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class NavigationSettingsValidator {
    public List<NavigationDiagnostic> validate(NavigationSettingsSnapshot settings) {
        List<NavigationDiagnostic> diagnostics = new ArrayList<>();
        if (settings.arrivalTolerance() > 2.0D) {
            diagnostics.add(error("arrival_tolerance", "arrival_tolerance должен быть в диапазоне (0, 2]."));
        }
        if (settings.repathIntervalTicks() < 5) {
            diagnostics.add(error("repath_interval_ticks", "repath_interval_ticks должен быть не меньше 5."));
        }
        if (settings.stuckTimeoutTicks() < settings.progressSampleIntervalTicks()) {
            diagnostics.add(error("stuck_timeout_ticks", "stuck_timeout_ticks меньше progress_sample_interval_ticks."));
        }
        return List.copyOf(diagnostics);
    }

    private NavigationDiagnostic error(String field, String message) {
        return NavigationDiagnostic.error("NAVIGATION_BACKEND_SETTINGS_INVALID", message, Map.of("field", field));
    }
}
