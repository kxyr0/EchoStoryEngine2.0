package io.github.kxyr0.echovisibility.forge.api.diagnostic;

import io.github.kxyr0.echovisibility.api.diagnostic.VisibilityDiagnostic;
import io.github.kxyr0.echovisibility.api.identity.VisibilityWatchId;
import java.util.List;
import java.util.Optional;

public interface VisibilityDiagnosticsView {
    List<VisibilityDiagnostic> recent(Optional<VisibilityWatchId> watchId, int limit);
}
