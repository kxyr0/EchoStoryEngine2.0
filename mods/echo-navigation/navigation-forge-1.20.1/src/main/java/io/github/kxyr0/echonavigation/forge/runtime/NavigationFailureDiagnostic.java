package io.github.kxyr0.echonavigation.forge.runtime;

import io.github.kxyr0.echonavigation.api.diagnostic.NavigationDiagnostic;
import io.github.kxyr0.echonavigation.api.movement.NavigationFailure;
import io.github.kxyr0.echonavigation.api.movement.NavigationSnapshot;
import java.util.Map;

final class NavigationFailureDiagnostic {
    private NavigationFailureDiagnostic() {
    }

    static NavigationDiagnostic from(NavigationSnapshot snapshot, NavigationFailure failure) {
        return NavigationDiagnostic.error(failure.name(), "Navigation movement failed.",
                Map.of("moveId", snapshot.moveId().value(), "subjectId", snapshot.subjectId().value()));
    }
}
