package io.github.kxyr0.echonavigation.forge.settings;

import io.github.kxyr0.echonavigation.api.backend.NavigationSettingsSnapshot;
import io.github.kxyr0.echonavigation.api.diagnostic.NavigationDiagnostic;
import io.github.kxyr0.echonavigation.api.identity.NavigationBackendId;
import io.github.kxyr0.echonavigation.core.settings.NavigationSettingsValidator;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public final class ForgeNavigationSettingsState {
    private final ForgeNavigationSettingsFile file;
    private final NavigationSettingsValidator validator = new NavigationSettingsValidator();
    private NavigationSettingsSnapshot current = NavigationSettingsSnapshot.defaults();

    public ForgeNavigationSettingsState(Path gameDir) {
        this.file = new ForgeNavigationSettingsFile(gameDir);
    }

    public NavigationSettingsSnapshot current() {
        return current;
    }

    public List<NavigationDiagnostic> reload(List<NavigationBackendId> available) {
        var loaded = file.load(current.revision() + 1L);
        if (loaded.settings().isEmpty()) {
            return loaded.diagnostics();
        }
        NavigationSettingsSnapshot candidate = loaded.settings().orElseThrow();
        List<NavigationDiagnostic> invalid = validate(candidate, available);
        if (invalid.isEmpty()) {
            current = candidate;
        }
        return invalid.isEmpty() ? loaded.diagnostics() : invalid;
    }

    public List<NavigationDiagnostic> setBackend(NavigationBackendId backendId, List<NavigationBackendId> available) {
        NavigationSettingsSnapshot candidate = new NavigationSettingsSnapshot(current.revision() + 1L, backendId,
                current.arrivalTolerance(), current.repathIntervalTicks(), current.maxReplans(),
                current.progressSampleIntervalTicks(), current.minimumProgressDistance(),
                current.stuckTimeoutTicks(), current.maxMoveDurationTicks());
        List<NavigationDiagnostic> invalid = validate(candidate, available);
        if (!invalid.isEmpty()) {
            return invalid;
        }
        try {
            current = candidate;
            file.write(current);
            return List.of();
        } catch (IOException exception) {
            return List.of(NavigationDiagnostic.error("NAVIGATION_PERSISTENCE_FAILED", exception.getMessage(),
                    Map.of("backend", backendId.value())));
        }
    }

    private List<NavigationDiagnostic> validate(
            NavigationSettingsSnapshot candidate,
            List<NavigationBackendId> available) {
        if (!available.contains(candidate.backendId())) {
            return List.of(NavigationDiagnostic.error("NAVIGATION_BACKEND_NOT_FOUND",
                    "Configured backend provider is not registered.",
                    Map.of("backend", candidate.backendId().value())));
        }
        return validator.validate(candidate);
    }
}
