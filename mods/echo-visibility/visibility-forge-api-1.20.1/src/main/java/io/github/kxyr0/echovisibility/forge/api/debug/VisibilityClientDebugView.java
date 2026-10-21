package io.github.kxyr0.echovisibility.forge.api.debug;

import io.github.kxyr0.echovisibility.api.debug.VisibilityDebugSnapshot;
import io.github.kxyr0.echovisibility.api.debug.VisibilityPerformanceSnapshot;
import io.github.kxyr0.echovisibility.api.identity.VisibilityWatchId;
import io.github.kxyr0.echovisibility.api.report.VisibilityCameraSnapshot;
import java.util.List;
import java.util.Optional;

public interface VisibilityClientDebugView {
    Optional<VisibilityCameraSnapshot> camera();

    Optional<VisibilityDebugSnapshot> watch(VisibilityWatchId watchId);

    List<VisibilityDebugSnapshot> activeWatches();

    VisibilityPerformanceSnapshot performance();
}
