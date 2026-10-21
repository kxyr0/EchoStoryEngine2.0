package io.github.kxyr0.echonavigation.core.progress;

import io.github.kxyr0.echonavigation.api.backend.NavigationSettingsSnapshot;
import io.github.kxyr0.echonavigation.api.movement.NavigationProgressSnapshot;
import io.github.kxyr0.echonavigation.api.movement.NavigationStatus;

public final class NavigationStuckDetector {
    public boolean stuck(
            NavigationStatus status,
            NavigationProgressSnapshot progress,
            long tick,
            NavigationSettingsSnapshot settings) {
        if (status != NavigationStatus.MOVING && status != NavigationStatus.REPLANNING) {
            return false;
        }
        return tick - progress.lastProgressTick() >= settings.stuckTimeoutTicks();
    }
}
