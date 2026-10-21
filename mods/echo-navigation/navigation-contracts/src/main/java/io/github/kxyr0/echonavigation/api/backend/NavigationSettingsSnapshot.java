package io.github.kxyr0.echonavigation.api.backend;

import io.github.kxyr0.echonavigation.api.identity.NavigationBackendId;
import java.util.Objects;

public record NavigationSettingsSnapshot(
        long revision,
        NavigationBackendId backendId,
        double arrivalTolerance,
        int repathIntervalTicks,
        int maxReplans,
        int progressSampleIntervalTicks,
        double minimumProgressDistance,
        int stuckTimeoutTicks,
        int maxMoveDurationTicks) {
    public NavigationSettingsSnapshot {
        backendId = Objects.requireNonNull(backendId, "backendId");
        if (revision < 1L || arrivalTolerance <= 0.0D || repathIntervalTicks < 1 || maxReplans < 0
                || progressSampleIntervalTicks < 1 || minimumProgressDistance <= 0.0D
                || stuckTimeoutTicks < 1 || maxMoveDurationTicks < 0) {
            throw new IllegalArgumentException("Navigation settings are outside supported bounds.");
        }
    }

    public static NavigationSettingsSnapshot defaults() {
        return new NavigationSettingsSnapshot(1L, NavigationBackendId.VANILLA, 0.6D, 20, 3, 10, 0.1D, 100, 12000);
    }
}
